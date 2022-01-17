package io.the.orm.transaction

import failgood.Test
import failgood.describe
import failgood.mock.mock
import failgood.mock.verify
import failgood.mock.whenever
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
    val context = describe("transaction handling") {
        val connectionFactory = mock<DBConnectionFactory>()
        val r2dbcConnection = mock<DBConnection>()
        whenever(connectionFactory) { getConnection() }.then { r2dbcConnection }
        val transaction = mock<DBTransaction>()
        whenever(r2dbcConnection) { beginTransaction() }.then { transaction }
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
                connectionProvider.transaction {
                    throw runtimeException
                }
            }.isEqualTo(runtimeException)
            verify(transaction) { rollbackTransaction() }

        }
    }
}
