package com.jsamkt.custom.migrator.service.stage

import com.jsamkt.custom.migrator.dto.MigrationSettings
import com.jsamkt.custom.migrator.dto.MigrationStageValue
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class CompleteStage(override val stage: MigrationStageValue = MigrationStageValue.COMPLETE) :
    Stage {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun execute(settings: MigrationSettings) {
        logger.info("Migration '{}' completed all stages", settings.name)
    }

    override suspend fun rollback(settings: MigrationSettings) {
        // Nothing to rollback for COMPLETE stage
    }
}
