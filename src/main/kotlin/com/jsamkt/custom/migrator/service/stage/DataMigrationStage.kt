package com.jsamkt.custom.migrator.service.stage

import com.jsamkt.custom.migrator.dto.MigrationSettings
import com.jsamkt.custom.migrator.dto.MigrationStageValue
import com.jsamkt.custom.migrator.service.MigrationDataService
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.ensureActive
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class DataMigrationStage(
    override val stage: MigrationStageValue = MigrationStageValue.DATA_MIGRATION,
    private val service: MigrationDataService
) : Stage {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun execute(settings: MigrationSettings) {
        var batchCount = 0L
        var totalRows = 0L

        while (true) {
            coroutineContext.ensureActive()

            val result =
                service.migrateData(
                    migrationName = settings.name,
                    tableName = settings.tableName,
                    shadowTableName = settings.shadowTableName,
                    windowStep = settings.windowStep,
                    pk = settings.primaryKey)

            batchCount++
            totalRows += result.insertedRows

            logger.info(
                "Migration '{}' batch #{}: selected={}, inserted={}, complete={}, hasMore={}",
                settings.name,
                batchCount,
                result.selectedRows,
                result.insertedRows,
                result.migrationComplete,
                result.hasMore)

            if (result.migrationComplete) {
                logger.info(
                    "Migration '{}' data migration completed: {} batches, {} total rows",
                    settings.name,
                    batchCount,
                    totalRows)
                break
            }
        }
    }

    override suspend fun rollback(settings: MigrationSettings) {
        // Data migration rollback is not supported - data remains in shadow table
    }
}
