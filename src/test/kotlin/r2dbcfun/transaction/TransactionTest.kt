package r2dbcfun.transaction

import failfast.FailFast
import failfast.describe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
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

object ConnectionProviderTest {
    val context = describe("transaction handling") {
        val connectionFactory = mockk<DBConnectionFactory>()
        val r2dbcConnection = mockk<DBConnection>(relaxed = true)
        coEvery { connectionFactory.getConnection() }.returns(r2dbcConnection)
        val transaction = mockk<DBTransaction>(relaxed = true)
        coEvery { r2dbcConnection.beginTransaction() }.returns(transaction)
        val connectionProvider = TransactionalConnectionProvider(connectionFactory)
        it("calls block") {
            var called = false
            connectionProvider.transaction {
                coVerify { r2dbcConnection.beginTransaction() }
                called = true
            }
            expectThat(called).isTrue()
            coVerify { transaction.commitTransaction() }
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
            coVerify { transaction.rollbackTransaction() }

        }
    }
}
