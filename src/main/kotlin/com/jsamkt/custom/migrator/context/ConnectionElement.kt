package com.jsamkt.custom.migrator.context

import java.sql.Connection
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

class ConnectionElement(val connection: Connection) :
    AbstractCoroutineContextElement(ConnectionElement) {
    companion object Key : CoroutineContext.Key<ConnectionElement>
}
