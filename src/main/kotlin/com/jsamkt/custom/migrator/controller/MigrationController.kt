package com.jsamkt.custom.migrator.controller

import com.jsamkt.custom.migrator.context.ConnectionElement
import com.jsamkt.custom.migrator.dto.ExecuteStageRequest
import com.jsamkt.custom.migrator.dto.FixActionInfo
import com.jsamkt.custom.migrator.dto.FixRequest
import com.jsamkt.custom.migrator.dto.MigrationCheckResult
import com.jsamkt.custom.migrator.dto.MigrationSettings
import com.jsamkt.custom.migrator.dto.MigrationStatusValue
import com.jsamkt.custom.migrator.dto.MonitoringData
import com.jsamkt.custom.migrator.dto.OperationResponse
import com.jsamkt.custom.migrator.dto.RollbackRequest
import com.jsamkt.custom.migrator.dto.SqlRequest
import com.jsamkt.custom.migrator.dto.SqlResponse
import com.jsamkt.custom.migrator.repository.MigrationLogRepository
import com.jsamkt.custom.migrator.service.FixService
import com.jsamkt.custom.migrator.service.MonitoringService
import com.jsamkt.custom.migrator.service.SqlExecutorService
import com.jsamkt.custom.migrator.service.stage.Stage
import javax.sql.DataSource
import kotlinx.coroutines.reactor.flux
import kotlinx.coroutines.withContext
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux

@RestController
@RequestMapping("/api/migration")
class MigrationController(
    stages: List<Stage>,
    private val migrationSettings: List<MigrationSettings>,
    private val migrationLogRepository: MigrationLogRepository,
    private val monitoringService: MonitoringService,
    private val sqlExecutorService: SqlExecutorService,
    private val fixService: FixService,
    private val dataSource: DataSource
) {
    private val stageMap = stages.associateBy { it.stage }

    @PostMapping("/rollback")
    suspend fun rollback(@RequestBody request: RollbackRequest): OperationResponse {
        return dataSource.connection.use { con ->
            withContext(ConnectionElement(con)) {
                val stage =
                    stageMap[request.stage]
                        ?: return@withContext OperationResponse(
                            success = false,
                            message = "Stage ${request.stage} not found",
                        )

                val settings =
                    migrationSettings.find { it.name == request.name }
                        ?: return@withContext OperationResponse(
                            success = false,
                            message = "Migration '${request.name}' not found",
                        )

                try {
                    stage.rollback(settings)
                    migrationLogRepository.save(
                        name = request.name,
                        stage = request.stage,
                        status = MigrationStatusValue.PENDING,
                        message = "Manual rollback executed",
                    )
                    OperationResponse(
                        success = true,
                        message =
                            "Rollback of stage ${request.stage} for '${request.name}' completed",
                    )
                } catch (e: Exception) {
                    migrationLogRepository.save(
                        name = request.name,
                        stage = request.stage,
                        status = MigrationStatusValue.FAILED,
                        message = "Manual rollback failed: ${e.message}",
                    )
                    OperationResponse(
                        success = false,
                        message = "Rollback failed: ${e.message}"
                    )
                }
            }
        }
    }

    @PostMapping("/execute-stage")
    suspend fun executeStage(@RequestBody request: ExecuteStageRequest): OperationResponse {
        return dataSource.connection.use { con ->
            withContext(ConnectionElement(con)) {
                val stage =
                    stageMap[request.stage]
                        ?: return@withContext OperationResponse(
                            success = false,
                            message = "Stage ${request.stage} not found",
                        )

                val settings =
                    migrationSettings.find { it.name == request.name }
                        ?: return@withContext OperationResponse(
                            success = false,
                            message = "Migration '${request.name}' not found",
                        )

                try {
                    stage.execute(settings)
                    migrationLogRepository.save(
                        name = request.name,
                        stage = request.stage,
                        status = MigrationStatusValue.SUCCESS,
                        message = "Manual stage execution completed",
                    )
                    OperationResponse(
                        success = true,
                        message =
                            "Stage ${request.stage} for '${request.name}' executed successfully",
                    )
                } catch (e: Exception) {
                    migrationLogRepository.save(
                        name = request.name,
                        stage = request.stage,
                        status = MigrationStatusValue.FAILED,
                        message = "Manual stage execution failed: ${e.message}",
                    )
                    OperationResponse(
                        success = false,
                        message = "Stage execution failed: ${e.message}",
                    )
                }
            }
        }
    }

    @PostMapping("/sql")
    suspend fun executeSql(@RequestBody request: SqlRequest): SqlResponse {
        return dataSource.connection.use { con ->
            withContext(ConnectionElement(con)) {
                sqlExecutorService.execute(
                    request.sql,
                    request.returnResult,
                )
            }
        }
    }

    @GetMapping("/monitoring", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun monitoring(): Flux<MonitoringData> {
        return dataSource.connection.use { con ->
            flux(ConnectionElement(con)) {
                monitoringService.subscribe().collect { data -> send(data) }
            }
        }
    }

    @PostMapping("/fix")
    suspend fun fix(@RequestBody request: FixRequest): SqlResponse {
        return dataSource.connection.use { con ->
            withContext(ConnectionElement(con)) { fixService.execute(request) }
        }
    }

    @GetMapping("/fix/actions")
    suspend fun getAvailableFixActions(): List<FixActionInfo> {
        return dataSource.connection.use { con ->
            withContext(ConnectionElement(con)) { fixService.listActions() }
        }
    }

    @GetMapping("/check-all")
    suspend fun checkAll(): List<MigrationCheckResult> {
        return dataSource.connection.use { con ->
            withContext(ConnectionElement(con)) { fixService.checkAll() }
        }
    }
}
