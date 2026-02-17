package com.jsamkt.custom.migrator.service

import com.jsamkt.custom.migrator.dto.MigrationSettings
import com.jsamkt.custom.migrator.dto.MigrationStageValue
import com.jsamkt.custom.migrator.dto.MigrationStatusValue
import com.jsamkt.custom.migrator.repository.MigrationLogRepository
import com.jsamkt.custom.migrator.service.stage.Stage
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class StateMachine(
    private val migrationSettings: List<MigrationSettings>,
    private val migrationStatusService: MigrationStatusService,
    private val migrationLogRepository: MigrationLogRepository,
    stages: List<Stage>
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val stageMap: Map<MigrationStageValue, Stage> = stages.associateBy { it.stage }

    suspend fun run() {
        logger.info("StateMachine started, processing {} migrations", migrationSettings.size)

        for (settings in migrationSettings) {
            val result = processMigration(settings)
            if (result == ProcessResult.Failure) {
                logger.info("Migration failed for settings: {}", settings.name)
                return
            }
        }

        logger.info("StateMachine completed: all migrations finished successfully")
    }

    private suspend fun processMigration(settings: MigrationSettings): ProcessResult {
        val migrationStatus = migrationStatusService.getByName(settings.name)
        var stage: MigrationStageValue? = migrationStatus?.stage ?: MigrationStageValue.PREPARE
        var status: MigrationStatusValue? = migrationStatus?.status ?: MigrationStatusValue.PENDING

        while (true) {
            if (stage == null || status == null) {
                return ProcessResult.Success
            }
            if (status == MigrationStatusValue.FAILED) {
                return ProcessResult.Failure
            }
            val res = processStage(settings, stage, status)
            stage = res.first
            status = res.second
        }
    }

    private suspend fun processStage(
        settings: MigrationSettings,
        stage: MigrationStageValue,
        status: MigrationStatusValue,
    ): Pair<MigrationStageValue?, MigrationStatusValue?> {
        if (status == MigrationStatusValue.SUCCESS) {
            val nextStage = findNextAutoStage(stage)
            return nextStage to nextStage?.let { MigrationStatusValue.PENDING }
        }

        setStatus(settings.name, stage, MigrationStatusValue.PENDING, null)

        val stageExecutor =
            stageMap[stage] ?: throw IllegalStateException("No executor for stage $stage")
        val executeResult: ExecutionResult =
            when (status) {
                MigrationStatusValue.PENDING,
                MigrationStatusValue.SUSPENDED -> stageExecutor.safeExecute(settings)
                MigrationStatusValue.SUCCESS -> ExecutionResult.Success
                MigrationStatusValue.FAILED -> ExecutionResult.Failure("Migration failed")
            }

        setStatus(settings.name, stage, executeResult)

        return when (executeResult) {
            is ExecutionResult.Success -> {
                val nextStage = findNextAutoStage(stage)
                val nextStatus = if (nextStage != null) MigrationStatusValue.PENDING else null
                nextStage to nextStatus
            }
            is ExecutionResult.Suspended -> stage to MigrationStatusValue.SUSPENDED
            is ExecutionResult.Failure -> stage to MigrationStatusValue.FAILED
        }
    }

    private suspend fun Stage.safeExecute(settings: MigrationSettings): ExecutionResult {
        try {
            execute(settings)
            return ExecutionResult.Success
        } catch (e: TimeoutCancellationException) {
            return ExecutionResult.Suspended
        } catch (e: Exception) {
            return ExecutionResult.Failure(e.message ?: "Unknown error")
        }
    }

    private fun findNextAutoStage(current: MigrationStageValue): MigrationStageValue? {
        val allStages = MigrationStageValue.entries
        val currentIndex = allStages.indexOf(current)

        for (i in (currentIndex + 1) until allStages.size) {
            val stage = allStages[i]
            if (stage.auto) {
                return stage
            }
        }
        return null
    }

    private suspend fun setStatus(
        name: String,
        stage: MigrationStageValue,
        executeResult: ExecutionResult
    ) {
        when (executeResult) {
            is ExecutionResult.Failure ->
                setStatus(name, stage, MigrationStatusValue.FAILED, executeResult.message)
            ExecutionResult.Success -> setStatus(name, stage, MigrationStatusValue.SUCCESS, null)
            ExecutionResult.Suspended ->
                setStatus(name, stage, MigrationStatusValue.SUSPENDED, null)
        }
    }

    private suspend fun setStatus(
        name: String,
        stage: MigrationStageValue,
        status: MigrationStatusValue,
        message: String?
    ) {
        withContext(NonCancellable) {
            migrationStatusService.upsert(
                name,
                stage,
                status,
            )
            migrationLogRepository.save(
                name,
                stage,
                status,
                message,
            )
        }
    }
}

sealed class ExecutionResult {
    object Success : ExecutionResult()

    data class Failure(val message: String) : ExecutionResult()

    object Suspended : ExecutionResult()
}

sealed class ProcessResult {
    object Success : ProcessResult()

    object Failure : ProcessResult()
}
