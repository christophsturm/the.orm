package io.the.orm.test.functional

import failgood.Test
import io.the.orm.ConnectedRepo
import io.the.orm.test.DBS
import io.the.orm.test.describeOnAllDbs
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * examples for standard use-cases.
 * they run as part of the test suite to make sure they work.
 *
 */
@Test
object ExamplesTest {
    val context = describeOnAllDbs("examples", DBS.databases, USERS_SCHEMA) { connectionProvider ->

        test("throttled bulk inserts") {
            val connectionProvider = connectionProvider
            val user = User(
                name = "a user",
                email = "with email"
            )
            val repo = ConnectedRepo.create<User>(connectionProvider)

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
