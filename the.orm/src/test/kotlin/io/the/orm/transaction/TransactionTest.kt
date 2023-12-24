package io.the.orm.transaction

import failgood.Test
import failgood.mock.mock
import failgood.mock.verify
import failgood.tests
import io.the.orm.dbio.DBConnection
import io.the.orm.dbio.DBConnectionFactory
import io.the.orm.dbio.DBTransaction
import io.the.orm.dbio.TransactionalConnectionProvider
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue

@Test
class TransactionTest {
    val context = tests {
        val transaction = mock<DBTransaction>()
        val r2dbcConnection =
            mock<DBConnection> { method { beginTransaction() }.returns(transaction) }
        val connectionFactory =
            mock<DBConnectionFactory> { method { getConnection() }.returns(r2dbcConnection) }
        val connectionProvider = TransactionalConnectionProvider(connectionFactory)
        it("calls block") {
            var called = false
            connectionProvider.transaction {
                verify(r2dbcConnection) { r2dbcConnection.beginTransaction() }
                called = true
            }
            expectThat(called).isTrue()
            verify(transaction) { commitTransaction() }
        }
        it("returns the result of the block") {
            expectThat(connectionProvider.transaction { "RESULT" }).isEqualTo("RESULT")
        }
        test("rolls back transaction if exception occurs") {
            val runtimeException = RuntimeException("failed")
            expectThrows<RuntimeException> {
                    connectionProvider.transaction { throw runtimeException }
                }
                .isEqualTo(runtimeException)
            verify(transaction) { rollbackTransaction() }
        }
    }
}
