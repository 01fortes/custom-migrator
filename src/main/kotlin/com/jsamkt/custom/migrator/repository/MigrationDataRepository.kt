package com.jsamkt.custom.migrator.repository

import com.jsamkt.custom.migrator.dto.ForeignKey
import com.jsamkt.custom.migrator.dto.Index
import com.jsamkt.custom.migrator.dto.PrimaryKey
import com.jsamkt.custom.migrator.entity.MigrationData
import java.lang.StringBuilder
import org.springframework.core.io.ResourceLoader
import org.springframework.stereotype.Repository

@Repository
class MigrationDataRepository(
    resourceLoader: ResourceLoader,
) : AbstractRepository() {

    private val migrationSQL =
        String(
            resourceLoader.getResource("classpath:data_migration-template.sql").contentAsByteArray,
        )

    suspend fun createShadowTable(
        tableName: String,
        shadowTableName: String,
        foreignKeys: List<ForeignKey>
    ) {
        transaction { con ->
            val sql =
                """
                DO $$
                BEGIN
                    IF NOT EXISTS (SELECT 1 FROM pg_tables WHERE tablename = '$shadowTableName') THEN
                        CREATE TABLE $shadowTableName
                        (LIKE $tableName
                            INCLUDING DEFAULTS
                            INCLUDING IDENTITY
                            INCLUDING CONSTRAINTS
                            INCLUDING STORAGE);
                    END IF;
                END $$;
            """
                    .trimIndent()
            con.prepareStatement(sql).execute()

            foreignKeys.forEach { fk ->
                val sql =
                    """
                    ALTER TABLE $shadowTableName
                        ADD FOREIGN KEY (${fk.column}) REFERENCES ${fk.foreignTable} (${fk.foreignColumn})
                        NOT VALID;
                """
                        .trimIndent()
                con.prepareStatement(sql).execute()
            }
        }
    }

    suspend fun dropShadowTable(shadowTableName: String) {
        transaction { con ->
            val sql =
                """
                DROP TABLE IF EXISTS $shadowTableName;
            """
                    .trimIndent()
            con.prepareStatement(sql).execute()
        }
    }

    suspend fun createPrimaryIndex(
        shadowTableName: String,
        columns: List<String>,
        pkIndexName: String
    ) {
        transaction { con ->
            val sql =
                """
                DO $$
                BEGIN
                    ALTER TABLE $shadowTableName ADD CONSTRAINT "$pkIndexName" PRIMARY KEY (${
                    columns.joinToString(
                        ", ",
                    )
                });
                EXCEPTION WHEN duplicate_object THEN
                    NULL;
                END $$;
            """
                    .trimIndent()
            con.prepareStatement(sql).execute()
        }
    }

    suspend fun deletePrimaryIndex(shadowTableName: String, pkIndexName: String) {
        transaction { con ->
            val sql =
                """
                ALTER TABLE $shadowTableName DROP CONSTRAINT IF EXISTS $pkIndexName;
            """
                    .trimIndent()
            con.prepareStatement(sql).execute()
        }
    }

    suspend fun createMirrorFunction(
        tableName: String,
        shadowTableName: String,
        functionName: String
    ) {
        transaction { con ->
            val sql =
                """
                CREATE OR REPLACE FUNCTION $functionName()
                RETURNS TRIGGER AS $$
                BEGIN
                    INSERT INTO $shadowTableName SELECT * FROM $tableName WHERE id = NEW.id ON CONFLICT (id) DO NOTHING;
                    RETURN NEW;
                END;
                $$ LANGUAGE plpgsql;
            """
                    .trimIndent()
            con.prepareStatement(sql).execute()
        }
    }

    suspend fun deleteMirrorFunction(functionName: String) {
        transaction { con ->
            val sql =
                """
                DROP FUNCTION IF EXISTS $functionName();
            """
                    .trimIndent()
            con.prepareStatement(sql).execute()
        }
    }

    suspend fun createMirrorTrigger(tableName: String, triggerName: String, functionName: String) {
        transaction { con ->
            val sql =
                """
                DROP TRIGGER IF EXISTS $triggerName ON $tableName;
                CREATE TRIGGER $triggerName
                AFTER INSERT ON $tableName
                FOR EACH ROW
                EXECUTE FUNCTION $functionName();
            """
                    .trimIndent()
            con.prepareStatement(sql).execute()
        }
    }

    suspend fun deleteMirrorTrigger(triggerName: String, tableName: String) {
        transaction { con ->
            val sql =
                """
                DROP TRIGGER IF EXISTS $triggerName ON $tableName;
            """
                    .trimIndent()
            con.prepareStatement(sql).execute()
        }
    }

    suspend fun createShadowIndexes(shadowTableName: String, indexes: List<Index>) {
        val con = connection()
        val prevAutoCommit = con.autoCommit
        connection().autoCommit = true
        indexes.forEach { index ->
            val columns = index.columns.joinToString(",")
            val isUnique = index.unique
            val sql =
                """
                        CREATE ${if (isUnique) "UNIQUE" else ""} INDEX CONCURRENTLY IF NOT EXISTS "${index.shadowName}" 
                        ON $shadowTableName ($columns);
                        """
                    .trimIndent()
            con.prepareStatement(sql).execute()
        }
        try {
            con.autoCommit = prevAutoCommit
        } catch (_: Exception) {}
    }

    suspend fun deleteShadowIndexes(indexes: List<Index>) {
        val con = connection()
        val prevAutoCommit = con.autoCommit
        con.autoCommit = true
        indexes.forEach { index ->
            val sql =
                """
                    DROP INDEX CONCURRENTLY IF EXISTS "${index.shadowName}";
                """
                    .trimIndent()
            con.prepareStatement(sql).execute()
        }
        try {
            con.autoCommit = prevAutoCommit
        } catch (_: Exception) {}
    }

    suspend fun swapTables(
        oldTableName: String,
        newTableName: String,
        indexes: List<Pair<String, String>>
    ) {
        transaction { con ->
            con.prepareStatement("ALTER TABLE $oldTableName RENAME TO ${newTableName}_temp")
                .execute()
            con.prepareStatement("ALTER TABLE $newTableName RENAME TO $oldTableName").execute()
            con.prepareStatement("ALTER TABLE ${newTableName}_temp RENAME TO $newTableName")
                .execute()

            indexes.forEach { (old, new) ->
                con.prepareStatement("ALTER INDEX \"$old\" RENAME TO \"${new}_temp\"").execute()
                con.prepareStatement("ALTER INDEX \"$new\" RENAME TO \"$old\"").execute()
                con.prepareStatement("ALTER INDEX \"${new}_temp\" RENAME TO \"$new\"").execute()
            }
        }
    }

    suspend fun migrateBatchOfData(
        migrationName: String,
        tableName: String,
        shadowTableName: String,
        windowStep: String,
        pk: PrimaryKey
    ): MigrationData {
        val sql =
            StringBuilder(migrationSQL)
                .replace("{migration_name}", migrationName)
                .replace("{table_name}", tableName)
                .replace("{shadow_table_name}", shadowTableName)
                .replace("{window_step}", windowStep)
                .replace("{primary_key}", pk.columns.joinToString(", "))
                .toString()

        return transaction { con ->
            val rs = con.prepareStatement(sql).executeQuery()

            if (!rs.next()) {
                return@transaction MigrationData(0, 0, false, false)
            }

            val selectedRows = rs.getLong("selected_rows")
            val insertedRows = rs.getLong("inserted_rows")
            val migrationComplete = rs.getBoolean("migration_complete")
            val hasMore = rs.getBoolean("has_more")

            MigrationData(selectedRows, insertedRows, migrationComplete, hasMore)
        }
    }
}

fun StringBuilder.replace(old: String, new: String): StringBuilder {
    var index = 0
    while (indexOf(old, index).also { index = it } >= 0) {
        replace(index, index + old.length, new)
        index += new.length
    }
    return this
}
