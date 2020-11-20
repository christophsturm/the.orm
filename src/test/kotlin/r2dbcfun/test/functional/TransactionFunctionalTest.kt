package r2dbcfun.test.functional

import io.kotest.core.spec.style.FunSpec
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

class TransactionFunctionalTest : FunSpec({
    forAllDatabases(this, "transactions") { connectionFactory ->
        val connection = connectionFactory.create().awaitSingle()!!
        val user = User(
            name = "a user",
            email = "with email"
        )
        val repo = Repository.create<User>()

        test("transaction isolation") {
            val newConnection = connectionFactory.create().awaitSingle()!!
            val isolationLevel = IsolationLevel.READ_COMMITTED
            newConnection.setTransactionIsolationLevel(isolationLevel).awaitFirstOrNull()
            val userNameLike = repo.queryFactory.createQuery(User::name.like())
            val entries = 10
            connection.transaction {
                repeat(entries) {
                    repo.create(connection, user)
                }
                expectThat(userNameLike.with(connection, "%").find().toCollection(mutableListOf()).size).isEqualTo(
                    entries
                )
                expectThat(userNameLike.with(newConnection, "%").find().toCollection(mutableListOf()).size).isEqualTo(0)
            }
            expectThat(userNameLike.with(newConnection, "%").find().toCollection(mutableListOf()).size).isEqualTo(
                entries
            )
        }

    }
})

