package r2dbcfun.transaction

import failfast.FailFast
import failfast.describe
import failfast.mock.mock
import failfast.mock.verify
import failfast.mock.whenever
import r2dbcfun.dbio.DBConnection
import r2dbcfun.dbio.DBConnectionFactory
import r2dbcfun.dbio.DBTransaction
import r2dbcfun.dbio.TransactionalConnectionProvider
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue

fun main() {
    FailFast.runTest()
}

object TransactionTest {
    val context = describe("transaction handling") {
        val connectionFactory = mock<DBConnectionFactory>()
        val r2dbcConnection = mock<DBConnection>()
        whenever(connectionFactory) { getConnection() }.thenReturn(r2dbcConnection)
        val transaction = mock<DBTransaction>()
        whenever(r2dbcConnection) { beginTransaction() }.thenReturn(transaction)
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
            expectThat(connectionProvider.transaction() { "RESULT" }).isEqualTo("RESULT")
        }
        test("rolls back transaction if exception occurs") {
            val runtimeException = RuntimeException("failed")
            expectThrows<RuntimeException> {
                connectionProvider.transaction() {
                    throw runtimeException
                }
            }.isEqualTo(runtimeException)
            verify(transaction) { rollbackTransaction() }

        }
    }
}
