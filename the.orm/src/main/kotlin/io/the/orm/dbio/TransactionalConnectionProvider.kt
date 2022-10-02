package io.the.orm.dbio

class TransactionalConnectionProvider(val dbConnectionFactory: DBConnectionFactory) : TransactionProvider {
    override suspend fun <T> transaction(function: suspend (ConnectionProvider) -> T): T {
        val connection = dbConnectionFactory.getConnection()
        val transaction = connection.beginTransaction()
        val result = try {
            function(FixedConnectionProvider(connection))
        } catch (e: Exception) {
            transaction.rollbackTransaction()
            throw e
        }
        transaction.commitTransaction()
        connection.close()
        return result
    }

    override suspend fun <T> withConnection(function: suspend (DBConnection) -> T): T {
        val connection = dbConnectionFactory.getConnection()
        return try {
            function(connection)
        } catch (e: Exception) {
            throw e
        } finally {
            connection.close()
        }
    }
}
