package com.jsamkt.custom.migrator.service.stage

import com.jsamkt.custom.migrator.dto.MigrationSettings
import com.jsamkt.custom.migrator.dto.MigrationStageValue


interface Stage {

    val stage: MigrationStageValue

    suspend fun execute(settings: MigrationSettings)

    suspend fun rollback(settings: MigrationSettings)
}
