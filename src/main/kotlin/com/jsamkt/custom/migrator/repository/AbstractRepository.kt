package com.jsamkt.custom.migrator.repository

import com.jsamkt.custom.migrator.context.ConnectionElement
import java.sql.Connection
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.yield

abstract class AbstractRepository() {

    suspend fun connection(): Connection {
        yield()
        return currentCoroutineContext()[ConnectionElement]?.connection
            ?: throw IllegalStateException("Connection is not found in coroutine context")
    }

    suspend fun <T> transaction(block: suspend (Connection) -> T): T {
        val con = connection()
        val prevAutoCommit = con.autoCommit
        try {
            con.autoCommit = false
            val result = block(con)
            con.commit()
            return result
        } catch (e: Exception) {
            try {
                con.rollback()
            } catch (_: Exception) {
                // ignore
            }
            throw e
        } finally {
            try {
                con.autoCommit = prevAutoCommit
            } catch (_: Exception) {
                // ignore
            }
        }
    }
}
