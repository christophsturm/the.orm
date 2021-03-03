package r2dbcfun.dbio

interface DBConnectionFactory {
    suspend fun getConnection(): DBConnection
}
