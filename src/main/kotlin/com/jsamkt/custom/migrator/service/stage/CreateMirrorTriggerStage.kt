package com.jsamkt.custom.migrator.service.stage

import com.jsamkt.custom.migrator.dto.MigrationSettings
import com.jsamkt.custom.migrator.dto.MigrationStageValue
import com.jsamkt.custom.migrator.service.MigrationDataService
import org.springframework.stereotype.Service

@Service
class CreateMirrorTriggerStage(
    override val stage: MigrationStageValue = MigrationStageValue.CREATE_MIRROR_TRIGGER,
    private val migrationDataService: MigrationDataService
) : Stage {

    override suspend fun execute(settings: MigrationSettings) {
        migrationDataService.createMirrorTrigger(
            tableName = settings.tableName,
            triggerName = settings.trigger.name,
            functionName = settings.trigger.procedure)
    }

    override suspend fun rollback(settings: MigrationSettings) {
        migrationDataService.deleteMirrorTrigger(
            triggerName = settings.trigger.name, tableName = settings.tableName)
    }
}
