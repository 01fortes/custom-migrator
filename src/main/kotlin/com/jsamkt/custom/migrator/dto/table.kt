package com.jsamkt.custom.migrator.dto

data class Table(
    val name: String,
    val shadowName: String,
    val pk: PrimaryKey,
    val indexes: List<Index> = emptyList(),
    val foreignKeys: List<ForeignKey> = emptyList()
)

data class PrimaryKey(val columns: List<String>, val name: String, val shadowName: String)

data class Index(
    val name: String,
    val shadowName: String,
    val columns: List<String>,
    val unique: Boolean = false
)

data class ForeignKey(val column: String, val foreignTable: String, val foreignColumn: String)
