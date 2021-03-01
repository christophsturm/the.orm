package r2dbcfun.transaction

import failfast.FailFast
import failfast.describe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkStatic
import io.r2dbc.spi.Connection
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.reactivestreams.Publisher
import r2dbcfun.dbio.r2dbc.R2dbcConnection
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue

fun main() {
    FailFast.runTest()
}

object TransactionTest {
    init {
        // run this only once and not once per test
        mockkStatic(Publisher<Any>::awaitFirstOrNull)       // <*> is more correct but will throw an internal error
    }

    val context = describe("transaction handling") {
        val r2dbcConnection = mockk<Connection>("r2dbc connection")
        coEvery { r2dbcConnection.beginTransaction().awaitFirstOrNull() }.returns(null)
        coEvery { r2dbcConnection.commitTransaction().awaitFirstOrNull() }.returns(null)
        coEvery { r2dbcConnection.rollbackTransaction().awaitFirstOrNull() }.returns(null)

        val connection = R2dbcConnection(r2dbcConnection)
        it("calls block") {
            var called = false
            connection.transaction() {
                coVerify { r2dbcConnection.beginTransaction() }
                called = true
            }
            expectThat(called).isTrue()
            coVerify { r2dbcConnection.commitTransaction() }
        }
        it("returns the result of the block") {
            expectThat(connection.transaction() { "RESULT" }).isEqualTo("RESULT")
        }
        test("rolls back transaction if exception occurs") {
            val runtimeException = RuntimeException("failed")
            expectThrows<RuntimeException> {
                connection.transaction() {
                    throw runtimeException
                }
            }.isEqualTo(runtimeException)
            coVerify { r2dbcConnection.rollbackTransaction() }

        }
    }
}
