package com.jsamkt.custom.migrator.service.stage

import com.jsamkt.custom.migrator.dto.MigrationSettings
import com.jsamkt.custom.migrator.dto.MigrationStageValue
import com.jsamkt.custom.migrator.service.MigrationDataService
import org.springframework.stereotype.Service

@Service
class DropShadowTableStage(
    override val stage: MigrationStageValue = MigrationStageValue.DROP_SHADOW_TABLE,
    private val migrationDataService: MigrationDataService
) : Stage {
    override suspend fun execute(settings: MigrationSettings) {
        migrationDataService.dropShadowTable(settings.shadowTableName)
    }

    override suspend fun rollback(settings: MigrationSettings) {
        TODO("Not yet implemented")
    }
}
