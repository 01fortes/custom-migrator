package com.jsamkt.custom.migrator.service.stage

import com.jsamkt.custom.migrator.dto.MigrationSettings
import com.jsamkt.custom.migrator.dto.MigrationStageValue
import com.jsamkt.custom.migrator.service.MigrationDataService
import org.springframework.stereotype.Service

@Service
class DeleteMirrorFunctionStage(
    override val stage: MigrationStageValue = MigrationStageValue.DELETE_MIRROR_FUNCTION,
    private val migrationDataService: MigrationDataService
) : Stage {
    override suspend fun execute(settings: MigrationSettings) {
        migrationDataService.deleteMirrorFunction(settings.procedure.name)
    }

    override suspend fun rollback(settings: MigrationSettings) {
        migrationDataService.createMirrorFunction(
            tableName = settings.tableName,
            shadowTableName = settings.shadowTableName,
            functionName = settings.procedure.name)
    }
}
