package com.jsamkt.custom.migrator.service

import com.jsamkt.custom.migrator.dto.SqlResponse
import com.jsamkt.custom.migrator.repository.AbstractRepository
import org.springframework.stereotype.Service

@Service
class SqlExecutorService : AbstractRepository() {

    suspend fun execute(sql: String, returnResult: Boolean): SqlResponse {
        return try {
            transaction { con ->
                val st = con.prepareStatement(sql)

                if (returnResult) {
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
}
