package com.jsamkt.custom.migrator.service

import com.jsamkt.custom.migrator.dto.MigrationStageValue
import com.jsamkt.custom.migrator.dto.MigrationStatusValue
import com.jsamkt.custom.migrator.repository.MigrationStatusRepository
import org.springframework.stereotype.Service

@Service
class MigrationStatusService(private val repository: MigrationStatusRepository) {

    suspend fun getAll() = repository.getAll()

    suspend fun getByName(name: String) = repository.getByName(name)

    suspend fun upsert(
        name: String,
        stage: MigrationStageValue,
        status: MigrationStatusValue,
        message: String? = null
    ) = repository.upsert(name, stage, status, message)

    suspend fun update(
        name: String,
        stage: MigrationStageValue,
        status: MigrationStatusValue,
        message: String? = null
    ) = repository.update(name, stage, status, message)
}
