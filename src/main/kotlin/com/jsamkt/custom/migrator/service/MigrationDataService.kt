package com.jsamkt.custom.migrator.service

import com.jsamkt.custom.migrator.dto.ForeignKey
import com.jsamkt.custom.migrator.dto.Index
import com.jsamkt.custom.migrator.dto.PrimaryKey
import com.jsamkt.custom.migrator.entity.MigrationData
import com.jsamkt.custom.migrator.repository.MigrationDataRepository
import org.springframework.stereotype.Service

@Service
class MigrationDataService(private val repository: MigrationDataRepository) {

    suspend fun createShadowTable(
        tableName: String,
        shadowTableName: String,
        foreignKeys: List<ForeignKey>
    ) {
        repository.createShadowTable(tableName, shadowTableName, foreignKeys)
    }

    suspend fun dropShadowTable(shadowTableName: String) {
        repository.dropShadowTable(shadowTableName)
    }

    suspend fun createPrimaryIndex(
        shadowTableName: String,
        columns: List<String>,
        pkIndexName: String
    ) {
        repository.createPrimaryIndex(shadowTableName, columns, pkIndexName)
    }

    suspend fun deletePrimaryIndex(shadowTableName: String, pkIndexName: String) {
        repository.deletePrimaryIndex(shadowTableName, pkIndexName)
    }

    suspend fun createMirrorFunction(
        tableName: String,
        shadowTableName: String,
        functionName: String
    ) {
        repository.createMirrorFunction(tableName, shadowTableName, functionName)
    }

    suspend fun deleteMirrorFunction(functionName: String) {
        repository.deleteMirrorFunction(functionName)
    }

    suspend fun createMirrorTrigger(tableName: String, triggerName: String, functionName: String) {
        repository.createMirrorTrigger(tableName, triggerName, functionName)
    }

    suspend fun deleteMirrorTrigger(triggerName: String, tableName: String) {
        repository.deleteMirrorTrigger(triggerName, tableName)
    }

    suspend fun createShadowIndexes(shadowTableName: String, indexes: List<Index>) {
        repository.createShadowIndexes(shadowTableName, indexes)
    }

    suspend fun deleteShadowIndexes(indexes: List<Index>) {
        repository.deleteShadowIndexes(indexes)
    }

    suspend fun swapTables(
        oldTableName: String,
        newTableName: String,
        indexes: List<Pair<String, String>>
    ) {
        repository.swapTables(oldTableName, newTableName, indexes)
    }

    suspend fun migrateData(
        migrationName: String,
        tableName: String,
        shadowTableName: String,
        windowStep: String,
        pk: PrimaryKey
    ): MigrationData {
        return repository.migrateBatchOfData(
            migrationName = migrationName,
            tableName = tableName,
            shadowTableName = shadowTableName,
            windowStep = windowStep,
            pk = pk)
    }
}
