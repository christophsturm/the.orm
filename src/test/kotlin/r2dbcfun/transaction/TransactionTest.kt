package r2dbcfun.transaction

import failfast.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkStatic
import io.r2dbc.spi.Connection
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.reactivestreams.Publisher
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue

fun main() {
    Suite(TransactionTest.context).run()
}

object TransactionTest {
    init {
        // run this only once and not once per test
        mockkStatic(Publisher<Any>::awaitFirstOrNull)       // <*> is more correct but will throw an internal error
    }

    val context = describe("transaction handling") {
        val connection = mockk<Connection>("r2dbc connection")
        coEvery { connection.beginTransaction().awaitFirstOrNull() }.returns(null)
        coEvery { connection.commitTransaction().awaitFirstOrNull() }.returns(null)
        coEvery { connection.rollbackTransaction().awaitFirstOrNull() }.returns(null)
        it("calls block") {
            var called = false
            transaction(connection) {
                coVerify { connection.beginTransaction().awaitFirstOrNull() }
                called = true
            }
            expectThat(called).isTrue()
            coVerify { connection.commitTransaction().awaitFirstOrNull() }
        }
        it("returns the result of the block") {
            expectThat(transaction(connection) { "RESULT" }).isEqualTo("RESULT")
        }
        test("rolls back transaction if exception occurs") {
            val runtimeException = RuntimeException("failed")
            expectThrows<RuntimeException> {
                transaction(connection) {
                    throw runtimeException
                }
            }.isEqualTo(runtimeException)
            coVerify { connection.rollbackTransaction().awaitFirstOrNull() }

        }
    }
}
