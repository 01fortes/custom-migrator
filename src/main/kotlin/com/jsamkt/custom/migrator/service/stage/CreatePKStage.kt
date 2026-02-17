package com.jsamkt.custom.migrator.service.stage

import com.jsamkt.custom.migrator.dto.MigrationSettings
import com.jsamkt.custom.migrator.dto.MigrationStageValue
import com.jsamkt.custom.migrator.service.MigrationDataService
import org.springframework.stereotype.Service

@Service
class CreatePKStage(
    override val stage: MigrationStageValue = MigrationStageValue.CREATE_PK,
    private val migrationDataService: MigrationDataService
) : Stage {
    override suspend fun execute(settings: MigrationSettings) {
        migrationDataService.createPrimaryIndex(
            shadowTableName = settings.shadowTableName,
            columns = settings.primaryKey.columns,
            pkIndexName = settings.primaryKey.shadowName)
    }

    override suspend fun rollback(settings: MigrationSettings) {
        migrationDataService.deletePrimaryIndex(
            settings.shadowTableName, settings.primaryKey.shadowName)
    }
}
