package r2dbcfun.test.functional

import failfast.describe
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import r2dbcfun.ConnectedRepository
import r2dbcfun.test.DBS
import r2dbcfun.test.forAllDatabases


/**
 * place to add code examples for maybe interesting use-cases.
 * they run as part of the test suite to make sure they work.
 *
 */

object ExamplesTest {
    val context = describe("examples") {
        forAllDatabases(DBS, DBS.unstableDatabases) { createConnectionProvider ->
            val connectionProvider = createConnectionProvider()
            val user = User(
                name = "a user",
                email = "with email"
            )
            val repo = ConnectedRepository.create<User>(connectionProvider)

            test("throttled bulk inserts") {
                val channel = Channel<Deferred<User>>(capacity = 40)
                val entries = 1000
                coroutineScope {
                    launch {
                        connectionProvider.transaction {
                            repeat(entries) {
                                channel.send(async {
                                    repo.create(user.copy(email = java.util.UUID.randomUUID().toString()))
                                })
                            }
                        }
                    }
                    repeat(entries) {
                        channel.receive().await()
                    }
                }
            }
        }
    }
}
