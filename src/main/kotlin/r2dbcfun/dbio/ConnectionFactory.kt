package r2dbcfun.dbio

interface ConnectionFactory {
    suspend fun getConnection(): DBConnection
}
