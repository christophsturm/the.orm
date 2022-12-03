package io.the.orm.exp.testing

import io.the.orm.dbio.ConnectionProvider
import io.the.orm.dbio.DBConnection
import io.the.orm.dbio.TransactionProvider

class MockTransactionProvider(val connectionProvider: ConnectionProvider = MockConnectionProvider()) :
    TransactionProvider {
    override suspend fun <T> transaction(function: suspend (ConnectionProvider) -> T): T = function(connectionProvider)

    override suspend fun <T> withConnection(function: suspend (DBConnection) -> T): T {
        TODO("Not yet implemented")
    }
}
