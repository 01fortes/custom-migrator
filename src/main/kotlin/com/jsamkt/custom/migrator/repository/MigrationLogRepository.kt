package com.jsamkt.custom.migrator.repository

import com.jsamkt.custom.migrator.dto.MigrationStageValue
import com.jsamkt.custom.migrator.dto.MigrationStatusValue
import com.jsamkt.custom.migrator.dto.MonitoringData
import com.jsamkt.custom.migrator.entity.MigrationLog
import com.jsamkt.custom.migrator.service.MonitoringService
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Clock
import org.springframework.stereotype.Repository

@Repository
class MigrationLogRepository(
    private val monitoringService: MonitoringService,
    private val clock: Clock
) : AbstractRepository() {

    private companion object {
        val TABLE_NAME = "migration_log"
        val COLUMN_NAME = "name"
        val COLUMN_STAGE = "stage"
        val COLUMN_STATUS = "status"
        val COLUMN_MESSAGE = "message"
        val COLUMN_CREATED_AT = "created_at"
    }

    suspend fun save(
        name: String,
        stage: MigrationStageValue,
        status: MigrationStatusValue,
        message: String?
    ) {
        transaction { con ->
            val now = clock.instant()
            val st =
                con.prepareStatement(
                    """
                INSERT INTO $TABLE_NAME (
                    $COLUMN_NAME,
                    $COLUMN_STAGE,
                    $COLUMN_STATUS,
                    $COLUMN_MESSAGE,
                    $COLUMN_CREATED_AT
                ) VALUES (?, ?, ?, ?, ?)
            """
                        .trimIndent(),
                )
            st.setString(1, name)
            st.setString(2, stage.name)
            st.setString(3, status.name)
            st.setString(4, message)
            st.setTimestamp(5, Timestamp.from(now))

            st.execute()

            monitoringService.append(
                MonitoringData(
                    name = name,
                    stage = stage,
                    status = status,
                    message = message,
                    createdAt = now.atZone(java.time.ZoneOffset.UTC).toLocalDateTime()))
        }
    }

    suspend fun getAll(): List<MigrationLog> {
        val st = connection().prepareStatement("SELECT * FROM migration_log ORDER BY CREATED_AT")
        val rs = st.executeQuery()

        val result = mutableListOf<MigrationLog>()
        while (rs.next()) {
            result.add(map(rs))
        }
        return result
    }

    private fun map(rs: ResultSet): MigrationLog {
        return MigrationLog(
            name = rs.getString(COLUMN_NAME),
            stage = MigrationStageValue.valueOf(rs.getString(COLUMN_STAGE)),
            status = MigrationStatusValue.valueOf(rs.getString(COLUMN_STATUS)),
            message = rs.getString(COLUMN_MESSAGE),
            createdAt = rs.getTimestamp(COLUMN_CREATED_AT).toLocalDateTime())
    }
}
