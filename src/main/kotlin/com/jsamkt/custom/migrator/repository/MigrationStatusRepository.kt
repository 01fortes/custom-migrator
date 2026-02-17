package com.jsamkt.custom.migrator.repository

import com.jsamkt.custom.migrator.dto.MigrationStageValue
import com.jsamkt.custom.migrator.dto.MigrationStatusValue
import com.jsamkt.custom.migrator.entity.MigrationStatus
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Clock
import org.springframework.stereotype.Repository

@Repository
class MigrationStatusRepository(private val clock: Clock) : AbstractRepository() {

    private companion object {
        const val TABLE_NAME = "migration_status"
        const val COLUMN_NAME = "name"
        const val COLUMN_STAGE = "stage"
        const val COLUMN_STATUS = "status"
        const val COLUMN_MESSAGE = "message"
        const val COLUMN_CREATED_AT = "created_at"
        const val COLUMN_UPDATED_AT = "updated_at"
    }

    suspend fun getAll(): List<MigrationStatus> {
        val st =
            connection()
                .prepareStatement(
                    """
            SELECT 
                $COLUMN_NAME, 
                $COLUMN_STAGE, 
                $COLUMN_STATUS,
                $COLUMN_MESSAGE,
                $COLUMN_CREATED_AT, 
                $COLUMN_UPDATED_AT 
            FROM $TABLE_NAME
            ORDER BY $COLUMN_CREATED_AT
            """
                        .trimIndent())
        val rs = st.executeQuery()
        val result = mutableListOf<MigrationStatus>()
        while (rs.next()) {
            result.add(map(rs))
        }
        return result
    }

    suspend fun getByName(name: String): MigrationStatus? {
        val st =
            connection()
                .prepareStatement(
                    """
            SELECT 
                $COLUMN_NAME, 
                $COLUMN_STAGE, 
                $COLUMN_STATUS,
                $COLUMN_MESSAGE,
                $COLUMN_CREATED_AT, 
                $COLUMN_UPDATED_AT 
            FROM $TABLE_NAME
            WHERE $COLUMN_NAME = ?
            """
                        .trimIndent())
        st.setString(1, name)
        val rs = st.executeQuery()
        return if (rs.next()) map(rs) else null
    }

    suspend fun upsert(
        name: String,
        stage: MigrationStageValue,
        status: MigrationStatusValue,
        message: String? = null
    ) {
        val now = Timestamp.from(clock.instant())
        transaction { con ->
            val sql =
                """
                INSERT INTO $TABLE_NAME (
                    $COLUMN_NAME,
                    $COLUMN_STAGE,
                    $COLUMN_STATUS,
                    $COLUMN_MESSAGE,
                    $COLUMN_CREATED_AT,
                    $COLUMN_UPDATED_AT
                ) VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT ($COLUMN_NAME) DO UPDATE SET
                    $COLUMN_STAGE = EXCLUDED.$COLUMN_STAGE,
                    $COLUMN_STATUS = EXCLUDED.$COLUMN_STATUS,
                    $COLUMN_MESSAGE = EXCLUDED.$COLUMN_MESSAGE,
                    $COLUMN_UPDATED_AT = EXCLUDED.$COLUMN_UPDATED_AT
                """
                    .trimIndent()
            val st = con.prepareStatement(sql)
            st.setString(1, name)
            st.setString(2, stage.name)
            st.setString(3, status.name)
            st.setString(4, message)
            st.setTimestamp(5, now)
            st.setTimestamp(6, now)
            st.executeUpdate()
        }
    }

    suspend fun update(
        name: String,
        stage: MigrationStageValue,
        status: MigrationStatusValue,
        message: String? = null
    ) {
        val now = Timestamp.from(clock.instant())
        transaction { con ->
            val sql =
                """
                UPDATE $TABLE_NAME SET
                    $COLUMN_STAGE = ?,
                    $COLUMN_STATUS = ?,
                    $COLUMN_MESSAGE = ?,
                    $COLUMN_UPDATED_AT = ?
                WHERE $COLUMN_NAME = ?
                """
                    .trimIndent()
            val st = con.prepareStatement(sql)
            st.setString(1, stage.name)
            st.setString(2, status.name)
            st.setString(3, message)
            st.setTimestamp(4, now)
            st.setString(5, name)
            st.executeUpdate()
        }
    }

    private fun map(rs: ResultSet): MigrationStatus {
        return MigrationStatus(
            name = rs.getString(COLUMN_NAME),
            stage = MigrationStageValue.valueOf(rs.getString(COLUMN_STAGE)),
            status = MigrationStatusValue.valueOf(rs.getString(COLUMN_STATUS)),
            message = rs.getString(COLUMN_MESSAGE),
            createdAt = rs.getTimestamp(COLUMN_CREATED_AT).toLocalDateTime(),
            updatedAt = rs.getTimestamp(COLUMN_UPDATED_AT).toLocalDateTime())
    }
}
