package r2dbcfun.transaction

import io.kotest.core.spec.style.FunSpec
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkStatic
import io.r2dbc.spi.Connection
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.reactivestreams.Publisher
import strikt.api.expectThat
import strikt.assertions.isTrue

class TransactionTest : FunSpec({
    context("transaction support") {
        val publisher: Publisher<Void> = mockk()
        mockkStatic("kotlinx.coroutines.reactive.AwaitKt")
        coEvery {
            publisher.awaitFirstOrNull()
        }.returns(null)
        val connection = mockk<Connection>(relaxed = true)
        coEvery { connection.beginTransaction() }.returns(publisher)
        coEvery { connection.commitTransaction() }.returns(publisher)
        test("calls block") {
            var called = false
            connection.transaction {
                called = true
            }
            expectThat(called).isTrue()
        }
    }
})
