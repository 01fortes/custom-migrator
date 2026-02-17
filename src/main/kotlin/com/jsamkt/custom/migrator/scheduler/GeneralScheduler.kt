package com.jsamkt.custom.migrator.scheduler

import com.jsamkt.custom.migrator.config.ApplicationMigrationProperties
import com.jsamkt.custom.migrator.context.ConnectionElement
import com.jsamkt.custom.migrator.service.LockService
import com.jsamkt.custom.migrator.service.StateMachine
import java.time.Clock
import java.time.Duration
import java.time.LocalTime
import javax.sql.DataSource
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.slf4j.LoggerFactory
import org.springframework.context.SmartLifecycle
import org.springframework.stereotype.Service

@Service
class GeneralScheduler(
    private val lockService: LockService,
    private val dataSource: DataSource,
    private val stateMachine: StateMachine,
    private val migrationProperties: ApplicationMigrationProperties,
    private val clock: Clock
) : SmartLifecycle {
    val logger = LoggerFactory.getLogger(javaClass)

    private val supervisorJob = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + supervisorJob)

    @Volatile private var running = false

    private var migrationJob: Job? = null

    private companion object {
        val LOCK_RETRY_INTERVAL = 5.minutes
        val WINDOW_CHECK_INTERVAL = 1.minutes
    }

    override fun start() {
        if (running) return
        running = true
        migrationJob =
            scope.launch {
                try {
                    logger.info("Migration scheduler started")
                    runMigrationUntilComplete()
                } catch (e: CancellationException) {
                    logger.info("Migration scheduler cancelled (shutdown).")
                    throw e
                } catch (e: Exception) {
                    logger.error("Migration scheduler crashed", e)
                    throw e
                }
            }
    }

    override fun stop() {
        logger.info("ContextClosedEvent received. Cancelling migration scheduler early...")
        running = true
        migrationJob?.cancel()
        supervisorJob.cancel()
    }

    override fun isRunning(): Boolean {
        return running
    }

    private suspend fun runMigrationUntilComplete() {
        while (true) {
            waitForWorkWindow()
            val finished = executeMigrationWindow()
            if (finished) {
                logger.info("Migration completed")
                return
            }
            logger.info("Migration suspended")
        }
    }

    private suspend fun executeMigrationWindow(): Boolean {
        // Создаем НОВОЕ соединение для каждого окна миграции
        return dataSource.connection.use { connection ->
            withContext(ConnectionElement(connection)) {
                acquireLock()
                try {
                    runWithWindowTimeout { stateMachine.run() }
                } finally {
                    releaseLock()
                }
            }
        }
    }

    private suspend fun runWithWindowTimeout(block: suspend () -> Unit): Boolean {
        val timeout = remainingWindowTime()
        return try {
            yield()
            withTimeout(timeout) { retryOnError { block() } }
            true
        } catch (e: TimeoutCancellationException) {
            logger.info(
                "Work window closed at ${migrationProperties.suspendAt}. Suspending migration.", e)
            false
        }
    }

    private suspend fun retryOnError(block: suspend () -> Unit) {
        while (true) {
            try {
                block()
                return
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.error("Error during migration, retrying in $WINDOW_CHECK_INTERVAL", e)
                delay(WINDOW_CHECK_INTERVAL)
            }
        }
    }

    private suspend fun acquireLock() {
        while (true) {
            try {
                if (lockService.lock()) return
                logger.debug("Lock held by another instance, retrying in $LOCK_RETRY_INTERVAL")
            } catch (e: Exception) {
                logger.error("Lock acquisition failed", e)
            }
            delay(LOCK_RETRY_INTERVAL)
        }
    }

    private suspend fun releaseLock() {
        withContext(NonCancellable) {
            while (true) {
                try {
                    lockService.unlock()
                    return@withContext
                } catch (e: Exception) {
                    logger.error("Lock release failed, retrying", e)
                    delay(LOCK_RETRY_INTERVAL)
                }
            }
        }
    }

    private suspend fun waitForWorkWindow() {
        while (!isInWorkWindow()) {
            delay(WINDOW_CHECK_INTERVAL)
        }
    }

    private fun isInWorkWindow(): Boolean {
        val now = LocalTime.now(clock)
        val start = migrationProperties.startAt
        val end = migrationProperties.suspendAt

        return if (start < end) {
            now in start..end
        } else {
            now >= start || now <= end
        }
    }

    private fun remainingWindowTime(): Long {
        val now = LocalTime.now(clock)
        val end = migrationProperties.suspendAt
        val duration = Duration.between(now, end)
        return if (duration.isNegative) {
            duration.plusDays(1).toMillis()
        } else {
            duration.toMillis()
        }
    }
}
