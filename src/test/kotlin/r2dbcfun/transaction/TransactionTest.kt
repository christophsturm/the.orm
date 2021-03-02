package r2dbcfun.transaction

import failfast.FailFast
import failfast.describe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import r2dbcfun.dbio.ConnectionFactory
import r2dbcfun.dbio.ConnectionProvider
import r2dbcfun.dbio.DBConnection
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue

fun main() {
    FailFast.runTest()
}

object ConnectionProviderTest {
    val context = describe("transaction handling") {
        val connectionFactory = mockk<ConnectionFactory>()
        val r2dbcConnection = mockk<DBConnection>(relaxed = true)
        coEvery { connectionFactory.getConnection() }.returns(r2dbcConnection)
        val connectionProvider = ConnectionProvider(connectionFactory)
        it("calls block") {
            var called = false
            connectionProvider.transaction() {
                coVerify { r2dbcConnection.beginTransaction() }
                called = true
            }
            expectThat(called).isTrue()
            coVerify { r2dbcConnection.commitTransaction() }
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
            coVerify { r2dbcConnection.rollbackTransaction() }

        }
    }
}
