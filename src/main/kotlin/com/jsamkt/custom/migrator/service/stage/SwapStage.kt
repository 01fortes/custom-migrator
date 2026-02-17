package com.jsamkt.custom.migrator.service.stage

import com.jsamkt.custom.migrator.dto.MigrationSettings
import com.jsamkt.custom.migrator.dto.MigrationStageValue
import com.jsamkt.custom.migrator.service.MigrationDataService
import org.springframework.stereotype.Service

@Service
class SwapStage(
    override val stage: MigrationStageValue = MigrationStageValue.SWAP,
    private val migrationDataService: MigrationDataService
) : Stage {

    override suspend fun execute(settings: MigrationSettings) {
        val indexes = buildList {
            add(Pair(settings.primaryKey.shadowName, settings.primaryKey.name))
            addAll(
                settings.indexes.map { Pair(it.shadowName, it.name) },
            )
        }
        migrationDataService.swapTables(
            oldTableName = settings.shadowTableName, newTableName = settings.tableName, indexes)
    }

    override suspend fun rollback(settings: MigrationSettings) {
        val indexes = buildList {
            add(settings.primaryKey.name to settings.primaryKey.shadowName)
            addAll(
                settings.indexes.map { it.name to it.shadowName },
            )
        }
        migrationDataService.swapTables(
            oldTableName = settings.tableName, newTableName = settings.shadowTableName, indexes)
    }
}
