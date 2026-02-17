package com.jsamkt.custom.migrator.entity

data class MigrationData(
    val selectedRows: Long,
    val insertedRows: Long,
    val migrationComplete: Boolean,
    val hasMore: Boolean
)
