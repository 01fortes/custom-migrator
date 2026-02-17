package com.jsamkt.custom.migrator.dto

data class MigrationCheckResult(
    val migration: String,
    val status: StatusCheck?,
    val state: StateCheck?,
    val original: TableResources,
    val shadow: TableResources,
    val function: ObjectCheck
)

data class TableResources(
    val table: ObjectCheck,
    val primaryKey: ObjectCheck?,
    val indexes: List<ObjectCheck>,
    val trigger: ObjectCheck? = null
)

data class StatusCheck(val stage: String, val status: String, val message: String?)

data class StateCheck(val status: String, val progress: Int)

data class ObjectCheck(val name: String, val exists: Boolean)
