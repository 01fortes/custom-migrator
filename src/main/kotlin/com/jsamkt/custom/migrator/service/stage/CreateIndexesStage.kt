package com.jsamkt.custom.migrator.service.stage

import com.jsamkt.custom.migrator.dto.MigrationSettings
import com.jsamkt.custom.migrator.dto.MigrationStageValue
import com.jsamkt.custom.migrator.service.MigrationDataService
import org.springframework.stereotype.Service

@Service
class CreateIndexesStage(
    override val stage: MigrationStageValue = MigrationStageValue.CREATE_INDEXES,
    private val migrationDataService: MigrationDataService
) : Stage {

    override suspend fun execute(settings: MigrationSettings) {
        migrationDataService.createShadowIndexes(settings.shadowTableName, settings.indexes)
    }

    override suspend fun rollback(settings: MigrationSettings) {
        migrationDataService.deleteShadowIndexes(settings.indexes)
    }
}
