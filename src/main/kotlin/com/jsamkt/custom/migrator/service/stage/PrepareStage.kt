package com.jsamkt.custom.migrator.service.stage

import com.jsamkt.custom.migrator.dto.MigrationSettings
import com.jsamkt.custom.migrator.dto.MigrationStageValue
import com.jsamkt.custom.migrator.service.MigrationStateService
import org.springframework.stereotype.Service

@Service
class PrepareStage(
    override val stage: MigrationStageValue = MigrationStageValue.PREPARE,
    private val migrationStateService: MigrationStateService
) : Stage {

    override suspend fun execute(settings: MigrationSettings) {
        migrationStateService.initState(name = settings.name, startCreatedAt = settings.createdAt)
    }

    override suspend fun rollback(settings: MigrationSettings) {
        migrationStateService.deleteState(settings.name)
    }
}
