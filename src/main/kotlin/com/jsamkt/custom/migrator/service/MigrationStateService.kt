package com.jsamkt.custom.migrator.service

import com.jsamkt.custom.migrator.repository.MigrationStateRepository
import java.time.LocalDateTime
import org.springframework.stereotype.Service

@Service
class MigrationStateService(private val migrationRepository: MigrationStateRepository) {

    suspend fun initState(name: String, startCreatedAt: LocalDateTime) {
        migrationRepository.createInitState(name = name, startCreatedAt = startCreatedAt)
    }

    suspend fun deleteState(name: String) {
        migrationRepository.deleteByName(name)
    }
}
