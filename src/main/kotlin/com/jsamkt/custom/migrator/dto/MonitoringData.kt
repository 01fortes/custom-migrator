package com.jsamkt.custom.migrator.dto

import java.time.LocalDateTime

data class MonitoringData(
    val name: String,
    val stage: MigrationStageValue,
    val status: MigrationStatusValue,
    val message: String?,
    val createdAt: LocalDateTime
)
