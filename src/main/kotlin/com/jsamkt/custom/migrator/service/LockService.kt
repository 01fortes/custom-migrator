package com.jsamkt.custom.migrator.service

import com.jsamkt.custom.migrator.repository.LockRepository
import org.springframework.stereotype.Service

@Service
class LockService(private val lockRepository: LockRepository) {

    suspend fun lock(): Boolean = lockRepository.lock()

    suspend fun unlock(): Boolean = lockRepository.unlock()
}
