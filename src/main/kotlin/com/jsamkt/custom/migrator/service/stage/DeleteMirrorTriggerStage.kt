package com.jsamkt.custom.migrator.service.stage

import com.jsamkt.custom.migrator.dto.MigrationSettings
import com.jsamkt.custom.migrator.dto.MigrationStageValue
import com.jsamkt.custom.migrator.service.MigrationDataService
import org.springframework.stereotype.Service

@Service
class DeleteMirrorTriggerStage(
    override val stage: MigrationStageValue = MigrationStageValue.DELETE_MIRROR_TRIGGER,
    private val migrationDataService: MigrationDataService
) : Stage {
    override suspend fun execute(settings: MigrationSettings) {
        migrationDataService.deleteMirrorTrigger(
            triggerName = settings.trigger.name, tableName = settings.tableName)
    }

    override suspend fun rollback(settings: MigrationSettings) {
        migrationDataService.createMirrorTrigger(
            triggerName = settings.trigger.name,
            tableName = settings.tableName,
            functionName = settings.procedure.name,
        )
    }
}
