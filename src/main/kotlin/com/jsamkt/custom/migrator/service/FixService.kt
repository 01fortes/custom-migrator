package com.jsamkt.custom.migrator.service


import com.jsamkt.custom.migrator.dto.FixAction
import com.jsamkt.custom.migrator.dto.FixActionInfo
import com.jsamkt.custom.migrator.dto.FixRequest
import com.jsamkt.custom.migrator.dto.MigrationCheckResult
import com.jsamkt.custom.migrator.dto.MigrationSettings
import com.jsamkt.custom.migrator.dto.ObjectCheck
import com.jsamkt.custom.migrator.dto.SqlResponse
import com.jsamkt.custom.migrator.dto.StateCheck
import com.jsamkt.custom.migrator.dto.StatusCheck
import com.jsamkt.custom.migrator.dto.TableResources
import com.jsamkt.custom.migrator.repository.AbstractRepository
import java.sql.Connection
import org.springframework.core.io.ResourceLoader
import org.springframework.stereotype.Service

@Service
class FixService(
    private val resourceLoader: ResourceLoader,
    private val migrationSettings: List<MigrationSettings>
) : AbstractRepository() {

    companion object {
        private const val FIX_PATH = "classpath:fix/"
        private val PARAM_REGEX = Regex("\\{(\\w+)}")
        private val COMMENT_REGEX = Regex("^--\\s*(.+)$", RegexOption.MULTILINE)
    }

    fun loadTemplate(action: FixAction): String {
        val resource = resourceLoader.getResource("$FIX_PATH${action.filename}.sql")
        if (!resource.exists()) {
            throw IllegalArgumentException("Fix action '${action.filename}' not found")
        }
        return String(resource.inputStream.readBytes())
    }

    fun extractParams(template: String): Set<String> =
        PARAM_REGEX.findAll(template).map { it.groupValues[1] }.toSet()

    fun extractDescription(template: String): String? =
        COMMENT_REGEX.find(template)?.groupValues?.get(1)

    fun buildQuery(template: String, params: Map<String, String>): String {
        var query = template
        params.forEach { (key, value) -> query = query.replace("{$key}", value) }
        return query
    }

    suspend fun execute(request: FixRequest): SqlResponse {
        val template =
            try {
                loadTemplate(request.action)
            } catch (e: Exception) {
                return SqlResponse(success = false, error = e.message)
            }

        val required = extractParams(template)
        val missing = required - request.params.keys
        if (missing.isNotEmpty()) {
            return SqlResponse(success = false, error = "Missing params: ${missing.joinToString()}")
        }

        val sql = buildQuery(template, request.params)

        return try {
            transaction { con ->
                val st = con.prepareStatement(sql)

                if (request.returnResult) {
                    val rs = st.executeQuery()
                    val metaData = rs.metaData
                    val columnCount = metaData.columnCount
                    val columnNames = (1..columnCount).map { metaData.getColumnName(it) }

                    val rows = mutableListOf<Map<String, Any?>>()
                    while (rs.next()) {
                        val row = columnNames.associateWith { colName -> rs.getObject(colName) }
                        rows.add(row)
                    }

                    SqlResponse(success = true, rows = rows)
                } else {
                    st.execute()
                    SqlResponse(success = true)
                }
            }
        } catch (e: Exception) {
            SqlResponse(success = false, error = e.message ?: "Unknown error")
        }
    }

    suspend fun checkAll(): List<MigrationCheckResult> {
        return transaction { con ->
            migrationSettings.map { settings ->
                MigrationCheckResult(
                    migration = settings.name,
                    status = checkMigrationStatus(con, settings.name),
                    state = checkMigrationState(con, settings.name),
                    original =
                        TableResources(
                            table = checkTableExists(con, settings.tableName),
                            primaryKey =
                                checkPrimaryKey(con, settings.tableName, settings.primaryKey.name),
                            indexes =
                                settings.indexes.map { index ->
                                    checkIndexExists(con, index.name, settings.tableName)
                                },
                            trigger =
                                checkTriggerExists(con, settings.trigger.name, settings.tableName)),
                    shadow =
                        TableResources(
                            table = checkTableExists(con, settings.shadowTableName),
                            primaryKey =
                                checkPrimaryKey(
                                    con, settings.shadowTableName, settings.primaryKey.shadowName),
                            indexes =
                                settings.indexes.map { index ->
                                    checkIndexExists(
                                        con, index.shadowName, settings.shadowTableName)
                                }),
                    function = checkFunctionExists(con, settings.procedure.name))
            }
        }
    }

    private fun checkMigrationStatus(con: Connection, name: String): StatusCheck? {
        val sql = "SELECT stage, status, message FROM migration_status WHERE name = ?"
        val st = con.prepareStatement(sql)
        st.setString(1, name)
        val rs = st.executeQuery()

        return if (rs.next()) {
            StatusCheck(
                stage = rs.getString("stage"),
                status = rs.getString("status"),
                message = rs.getString("message"))
        } else {
            null
        }
    }

    private fun checkMigrationState(con: Connection, name: String): StateCheck? {
        val sql =
            """
            SELECT 
                CASE WHEN window_start_at >= target_start_at THEN 'COMPLETE' ELSE 'IN_PROGRESS' END as status,
                COALESCE(
                    ROUND(
                        EXTRACT(EPOCH FROM (COALESCE(window_start_at, from_start_at) - from_start_at)) /
                        NULLIF(EXTRACT(EPOCH FROM (target_start_at - from_start_at)), 0) * 100
                    ),
                    0
                ) as progress
            FROM migration_state WHERE name = ?
        """
                .trimIndent()
        val st = con.prepareStatement(sql)
        st.setString(1, name)
        val rs = st.executeQuery()

        return if (rs.next()) {
            StateCheck(status = rs.getString("status"), progress = rs.getInt("progress"))
        } else {
            null
        }
    }

    private fun checkTableExists(con: Connection, tableName: String): ObjectCheck {
        val sql = "SELECT EXISTS (SELECT 1 FROM pg_tables WHERE tablename = ?) as exists"
        val st = con.prepareStatement(sql)
        st.setString(1, tableName)
        val rs = st.executeQuery()
        rs.next()

        return ObjectCheck(name = tableName, exists = rs.getBoolean("exists"))
    }

    private fun checkTriggerExists(
        con: Connection,
        triggerName: String,
        tableName: String
    ): ObjectCheck {
        val sql =
            """
            SELECT EXISTS (
                SELECT 1 FROM pg_trigger 
                WHERE tgname = ? AND tgrelid = to_regclass(?) AND NOT tgisinternal
            ) as exists
        """
                .trimIndent()

        return try {
            val st = con.prepareStatement(sql)
            st.setString(1, triggerName)
            st.setString(2, tableName)
            val rs = st.executeQuery()
            rs.next()
            ObjectCheck(name = triggerName, exists = rs.getBoolean("exists"))
        } catch (e: Exception) {
            ObjectCheck(name = triggerName, exists = false)
        }
    }

    private fun checkFunctionExists(con: Connection, functionName: String): ObjectCheck {
        val sql = "SELECT EXISTS (SELECT 1 FROM pg_proc WHERE proname = ?) as exists"
        val st = con.prepareStatement(sql)
        st.setString(1, functionName)
        val rs = st.executeQuery()
        rs.next()

        return ObjectCheck(name = functionName, exists = rs.getBoolean("exists"))
    }

    private fun checkPrimaryKey(con: Connection, tableName: String, pkName: String): ObjectCheck? {
        val sql =
            """
            SELECT EXISTS (
                SELECT 1 FROM pg_constraint 
                WHERE conrelid = to_regclass(?) AND contype = 'p' AND conname = ?
            ) as exists
        """
                .trimIndent()

        return try {
            val st = con.prepareStatement(sql)
            st.setString(1, tableName)
            st.setString(2, pkName)
            val rs = st.executeQuery()
            rs.next()
            ObjectCheck(name = pkName, exists = rs.getBoolean("exists"))
        } catch (e: Exception) {
            null
        }
    }

    private fun checkIndexExists(
        con: Connection,
        indexName: String,
        tableName: String
    ): ObjectCheck {
        val sql =
            "SELECT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = ? AND tablename = ?) as exists"
        val st = con.prepareStatement(sql)
        st.setString(1, indexName)
        st.setString(2, tableName)
        val rs = st.executeQuery()
        rs.next()

        return ObjectCheck(name = indexName, exists = rs.getBoolean("exists"))
    }

    fun listActions(): List<FixActionInfo> {
        return FixAction.entries.map { action ->
            try {
                val template = loadTemplate(action)
                FixActionInfo(
                    action = action,
                    description = extractDescription(template),
                    requiredParams = extractParams(template))
            } catch (e: Exception) {
                FixActionInfo(
                    action = action,
                    description = "Error: ${e.message}",
                    requiredParams = emptySet())
            }
        }
    }
}
