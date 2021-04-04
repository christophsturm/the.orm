package io.the.orm.dbio

interface DBConnectionFactory {
    suspend fun getConnection(): DBConnection
}
