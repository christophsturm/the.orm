package io.the.orm.dbio

interface DBConnection {
    fun createStatement(sql: String): Statement
    fun createInsertStatement(sql: String): Statement
    suspend fun beginTransaction(): DBTransaction
    suspend fun close()
    suspend fun execute(sql: String)
}
