package com.jsamkt.custom.migrator.entity

import java.time.LocalDateTime

data class MigrationState(
    val name: String,
    val fromStartAt: LocalDateTime,
    val targetStartAt: LocalDateTime,
    val windowStartAt: LocalDateTime,
    val windowEndAt: LocalDateTime,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
