package r2dbcfun.transaction

import failfast.Suite
import failfast.describe
import io.mockk.coVerify
import io.mockk.mockk
import r2dbcfun.r2dbc.DBConnection
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue

fun main() {
    Suite(TransactionTest.context).run()
}

object TransactionTest {

    val context = describe("transaction handling") {
        val connection = mockk<DBConnection>("database connection", relaxed = true)
        it("calls block") {
            var called = false
            transaction(connection) {
                coVerify { connection.beginTransaction() }
                called = true
            }
            expectThat(called).isTrue()
            coVerify { connection.commitTransaction() }
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
            coVerify { connection.rollbackTransaction() }

        }
    }
}
