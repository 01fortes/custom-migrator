package com.jsamkt.custom.migrator

import com.jsamkt.custom.migrator.config.ApplicationMigrationProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(ApplicationMigrationProperties::class)
class CustomMigratorApplication

fun main() {
    runApplication<CustomMigratorApplication>()
}
