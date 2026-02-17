package com.jsamkt.custom.migrator.dto

enum class MigrationStatusValue {
    PENDING,
    SUSPENDED,
    SUCCESS,
    FAILED
}

enum class MigrationStageValue(val auto: Boolean = true) {
    PREPARE,
    CREATE_SHADOW_TABLE,
    CREATE_PK,
    CREATE_MIRROR_FUNCTION,
    CREATE_MIRROR_TRIGGER,
    DATA_MIGRATION,
    CREATE_INDEXES,
    DELETE_MIRROR_TRIGGER,
    DELETE_MIRROR_FUNCTION,
    SWAP,
    COMPLETE,
    DROP_SHADOW_TABLE(false)
}

data class MigrationYamlFileContent(val files: List<String>)

data class MigrationItemYamlFileContent(
    val table: Table,
    val procedure: Procedure,
    val trigger: Trigger,
)
