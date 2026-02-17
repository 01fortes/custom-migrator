package com.jsamkt.custom.migrator.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.jsamkt.custom.migrator.dto.MigrationItemYamlFileContent
import com.jsamkt.custom.migrator.dto.MigrationSettings
import com.jsamkt.custom.migrator.dto.MigrationYamlFileContent
import java.time.Clock
import java.time.LocalDateTime
import java.time.LocalTime
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ResourceLoader

@Configuration
class ApplicationConfig {

    private companion object {
        const val MIGRATION_BASE_PATH = "migration"
        const val CONFIG_FILE_NAME = "migration.yaml"
    }

    private val objectMapper =
        ObjectMapper(YAMLFactory())
            .registerModule(kotlinModule { enable(KotlinFeature.NullIsSameAsDefault) })
            .setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    @Bean
    fun parseConfig(
        resourceLoader: ResourceLoader,
        applicationMigrationProperties: ApplicationMigrationProperties
    ): List<MigrationSettings> {
        val stream =
            resourceLoader
                .getResource("classpath:$MIGRATION_BASE_PATH/$CONFIG_FILE_NAME")
                .inputStream
                ?: throw IllegalArgumentException("Migration configuration file not found")

        val migration = objectMapper.readValue(stream, MigrationYamlFileContent::class.java)
        return migration.files.map { fileName ->
            val st =
                resourceLoader.getResource("classpath:$MIGRATION_BASE_PATH/$fileName").inputStream
                    ?: throw IllegalArgumentException("Migration file '$fileName' not found")
            val item = objectMapper.readValue(st, MigrationItemYamlFileContent::class.java)

            map(fileName, item, applicationMigrationProperties)
        }
    }

    @Bean fun clock(): Clock = Clock.systemUTC()

    private fun map(
        fileName: String,
        item: MigrationItemYamlFileContent,
        applicationMigrationProperties: ApplicationMigrationProperties
    ): MigrationSettings =
        MigrationSettings(
            name = fileName,
            tableName = item.table.name,
            shadowTableName = item.table.shadowName,
            createdAt = applicationMigrationProperties.sinceDate,
            primaryKey = item.table.pk,
            indexes = item.table.indexes,
            procedure = item.procedure,
            trigger = item.trigger,
            windowStep = applicationMigrationProperties.windowStep,
            foreignKeys = item.table.foreignKeys
        )
}

@ConfigurationProperties(prefix = "application.migration")
data class ApplicationMigrationProperties(
    val startAt: LocalTime,
    val suspendAt: LocalTime,
    val windowStep: String,
    val sinceDate: LocalDateTime
)
