package com.jsamkt.custom.migrator.entity

import com.jsamkt.custom.migrator.dto.MigrationStageValue
import com.jsamkt.custom.migrator.dto.MigrationStatusValue
import java.time.LocalDateTime

data class MigrationStatus(
    val name: String,
    val stage: MigrationStageValue,
    val status: MigrationStatusValue,
    val message: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
