package com.jsamkt.custom.migrator.service

import com.jsamkt.custom.migrator.dto.MonitoringData
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.springframework.stereotype.Service

@Service
class MonitoringService {

    private val state: MutableMap<String, CopyOnWriteArrayList<MonitoringData>> =
        ConcurrentHashMap()

    private val _events = MutableSharedFlow<MonitoringData>(extraBufferCapacity = 100)

    fun append(data: MonitoringData) {
        state.computeIfAbsent(data.name) { CopyOnWriteArrayList() }.add(data)
        _events.tryEmit(data)
    }

    fun subscribe(): Flow<MonitoringData> = _events.asSharedFlow()

    fun getLast(name: String, n: Int): List<MonitoringData> {
        val list = state[name] ?: return emptyList()
        val size = list.size
        return if (size <= n) {
            list.toList()
        } else {
            list.subList(size - n, size).toList()
        }
    }

    fun getAll(): List<MonitoringData> {
        return state.values.flatten().sortedBy { it.createdAt }
    }
}
