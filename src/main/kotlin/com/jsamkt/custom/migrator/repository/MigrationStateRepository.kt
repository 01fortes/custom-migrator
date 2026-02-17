package com.jsamkt.custom.migrator.repository

import java.sql.Timestamp
import java.time.Clock
import java.time.LocalDateTime
import org.springframework.stereotype.Repository

@Repository
class MigrationStateRepository(private val clock: Clock) : AbstractRepository() {

    private companion object {
        val TABLE_NAME = "migration_state"
        val COLUMN_NAME = "name"
        val COLUMN_FROM_START_AT = "from_start_at"
        val COLUMN_TARGET_START_AT = "target_start_at"
        val COLUMN_WINDOW_START_AT = "window_start_at"
        val COLUMN_WINDOW_END_AT = "window_end_at"
        val COLUMN_CREATED_AT = "created_at"
        val COLUMN_UPDATED_AT = "updated_at"
    }

    suspend fun createInitState(name: String, startCreatedAt: LocalDateTime) {
        val sql =
            """
            INSERT INTO $TABLE_NAME (
                $COLUMN_NAME, 
                $COLUMN_FROM_START_AT, 
                $COLUMN_TARGET_START_AT, 
                $COLUMN_WINDOW_START_AT, 
                $COLUMN_WINDOW_END_AT,
                $COLUMN_CREATED_AT,
                $COLUMN_UPDATED_AT)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT ($COLUMN_NAME) DO NOTHING
        """
                .trimIndent()

        transaction { con ->
            val st = con.prepareStatement(sql)
            st.setString(1, name)
            st.setTimestamp(2, Timestamp.valueOf(startCreatedAt))
            st.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now(clock)))
            st.setTimestamp(4, null)
            st.setTimestamp(5, null)
            st.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now(clock)))
            st.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now(clock)))
            st.executeUpdate()
        }
    }

    suspend fun deleteByName(name: String) {
        transaction { con ->
            val sql =
                """
                DELETE FROM $TABLE_NAME
                WHERE $COLUMN_NAME = ?
            """
                    .trimIndent()
            val st = con.prepareStatement(sql)
            st.setString(1, name)
            st.executeUpdate()
        }
    }
}
