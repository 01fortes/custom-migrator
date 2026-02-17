package com.jsamkt.custom.migrator.dto

data class RollbackRequest(val name: String, val stage: MigrationStageValue)

data class ExecuteStageRequest(val name: String, val stage: MigrationStageValue)

data class SqlRequest(val sql: String, val returnResult: Boolean = false)

data class SqlResponse(
    val success: Boolean,
    val rows: List<Map<String, Any?>>? = null,
    val error: String? = null
)

data class OperationResponse(val success: Boolean, val message: String)

enum class FixAction(val filename: String) {
    // Диагностика
    GET_STATUS("get_status"),
    GET_STATE("get_state"),
    GET_LOGS("get_logs"),
    COMPARE_ROW_COUNT("compare_row_count"),
    CHECK_SHADOW_TABLE_EXISTS("check_shadow_table_exists"),
    CHECK_PK_EXISTS("check_pk_exists"),
    CHECK_FK_DEPENDENCIES("check_fk_dependencies"),
    CHECK_TABLE_NAMES("check_table_names"),
    LIST_TRIGGERS("list_triggers"),
    LIST_INDEXES("list_indexes"),

    // Reset
    RESET_STATUS("reset_status"),
    RESET_DATA_CURSOR("reset_data_cursor"),
    DELETE_STATUS("delete_status"),
    DELETE_STATE("delete_state"),

    // Cleanup
    DROP_SHADOW_TABLE("drop_shadow_table"),
    DROP_SHADOW_TABLE_CASCADE("drop_shadow_table_cascade"),
    TRUNCATE_SHADOW("truncate_shadow"),
    DROP_TRIGGER("drop_trigger"),
    DROP_FUNCTION("drop_function"),
    DROP_PK("drop_pk"),
    DROP_INDEX("drop_index"),

    // Full
    FULL_CLEANUP("full_cleanup"),

    // Manual
    MANUAL_DATA_COPY("manual_data_copy")
}

data class FixRequest(
    val action: FixAction,
    val params: Map<String, String> = emptyMap(),
    val returnResult: Boolean = false
)

data class FixActionInfo(
    val action: FixAction,
    val description: String?,
    val requiredParams: Set<String>
)
