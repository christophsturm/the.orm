package r2dbcfun.test.functional

import io.kotest.core.spec.style.FunSpec
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.awaitSingle
import r2dbcfun.Repository
import r2dbcfun.test.forAllDatabases
import r2dbcfun.transaction.transaction

/**
 * place to add code examples for maybe interesting use-cases.
 * they run as part of the test suite to make sure they work.
 *
 */
class ExamplesTest : FunSpec({
    forAllDatabases(this, "examples") { connectionFactory ->
        val connection = connectionFactory.create().awaitSingle()!!
        val user = User(
            name = "a user",
            email = "with email"
        )
        val repo = Repository.create<User>()

        test("throttled bulk inserts") {
            val channel = Channel<Deferred<User>>(capacity = 40)
            val entries = 1000
            launch {
                connection.transaction {
                    repeat(entries) {
                        channel.send(async {
                            repo.create(connection, user.copy(email = java.util.UUID.randomUUID().toString()))
                        })
                    }
                }
            }
            repeat(entries) { channel.receive().await() }
        }
    }
})
