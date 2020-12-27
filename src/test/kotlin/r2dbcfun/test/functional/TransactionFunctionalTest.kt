package r2dbcfun.test.functional

import failfast.Suite
import failfast.context
import io.r2dbc.spi.IsolationLevel
import kotlinx.coroutines.flow.toCollection
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import r2dbcfun.Repository
import r2dbcfun.query.like
import r2dbcfun.test.forAllDatabases
import r2dbcfun.transaction.transaction
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.util.*

fun main() {
    Suite(TransactionFunctionalTest.context).run().check()
}

object TransactionFunctionalTest {
    val context = context {

        forAllDatabases() { connectionFactory ->
            val connection = autoClose(connectionFactory.create().awaitSingle()) { it.close() }
            val user = User(
                name = "a user",
                email = "with email"
            )
            val repo = Repository.create<User>()

            test("transaction isolation") {
                val newConnection = autoClose(connectionFactory.create().awaitSingle()) { it.close() }
                val isolationLevel = IsolationLevel.READ_COMMITTED
                newConnection.setTransactionIsolationLevel(isolationLevel).awaitFirstOrNull()
                val userNameLike = repo.queryFactory.createQuery(User::name.like())
                val entries = 10
                connection.transaction {
                    repeat(entries) {
                        repo.create(connection, user.copy(email = UUID.randomUUID().toString()))
                    }
                    expectThat(userNameLike.with(connection, "%").find().toCollection(mutableListOf()).size).isEqualTo(
                        entries
                    )
                    expectThat(
                        userNameLike.with(newConnection, "%").find().toCollection(mutableListOf()).size
                    ).isEqualTo(0)
                }
                expectThat(userNameLike.with(newConnection, "%").find().toCollection(mutableListOf()).size).isEqualTo(
                    entries
                )
            }

        }
    }
}

