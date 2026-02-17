package com.jsamkt.custom.migrator.repository

import org.springframework.stereotype.Repository

@Repository
class LockRepository : AbstractRepository() {

    companion object {
        private val SERVICE_NAME = "custom-migrator"
        private val LOCK_ID = 9999
    }

    suspend fun lock(): Boolean {
        val st = connection().prepareStatement("SELECT pg_try_advisory_lock(hashtext(?))")

        st.setString(1, "$SERVICE_NAME - $LOCK_ID")
        val rs = st.executeQuery()
        rs.next()

        return rs.getBoolean(1)
    }

    suspend fun unlock(): Boolean {
        val st = connection().prepareStatement("SELECT pg_advisory_unlock(hashtext(?))")

        st.setString(1, "$SERVICE_NAME - $LOCK_ID")
        val rs = st.executeQuery()
        rs.next()

        return rs.getBoolean(1)
    }
}
