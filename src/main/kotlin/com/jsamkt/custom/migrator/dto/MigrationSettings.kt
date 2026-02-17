package com.jsamkt.custom.migrator.dto

import java.time.LocalDateTime

data class MigrationSettings(
    val name: String,
    val tableName: String,
    val shadowTableName: String,
    val windowStep: String,
    val createdAt: LocalDateTime,
    val primaryKey: PrimaryKey,
    val indexes: List<Index> = emptyList(),
    val procedure: Procedure,
    val trigger: Trigger,
    val foreignKeys: List<ForeignKey>
)
