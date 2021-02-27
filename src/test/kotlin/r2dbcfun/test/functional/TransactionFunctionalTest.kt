package r2dbcfun.test.functional

import failfast.Suite
import failfast.describe
import failfast.r2dbc.forAllDatabases
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.reactive.awaitSingle
import r2dbcfun.ConnectionProvider
import r2dbcfun.Repository
import r2dbcfun.query.like
import r2dbcfun.test.DBS
import strikt.api.expectThat
import strikt.assertions.isEqualTo

fun main() {
    Suite(TransactionFunctionalTest.context).run().check()
}

object TransactionFunctionalTest {
    val context = describe("FT: The transaction handling") {
        val repo = Repository.create<User>()
        val userNameLike = repo.queryFactory.createQuery(User::name.like())

        forAllDatabases(DBS) { connectionFactory ->
            val connection = ConnectionProvider(autoClose(connectionFactory.create().awaitSingle()) { it.close() })
            it("has transaction isolation") {
                val differentConnection = autoClose(connectionFactory.create().awaitSingle()) { it.close() }
                val user = connection.transaction {
                    val user = repo.create(connection, User(name = "a user", email = "with email"))
                    // the created user is visible in the same connection
                    expectThat(userNameLike.with(connection, "%").find().single()).isEqualTo(user)
                    // but a different connection does not see it
                    expectThat(
                        userNameLike.with(ConnectionProvider(differentConnection), "%").find().count()
                    ).isEqualTo(0)
                    user
                }
                // now the other connection sees them too
                expectThat(userNameLike.with(ConnectionProvider(differentConnection), "%").find().single()).isEqualTo(
                    user
                )
            }
            it("rolls back the transaction if the block fails") {
                try {
                    connection.transaction {
                        repo.create(connection, User(name = "a user", email = "with email"))
                        throw RuntimeException("failed (oops)")
                    }
                } catch (e: Exception) {
                }
                expectThat(userNameLike.with(connection, "%").find().count()).isEqualTo(0)
            }
        }
    }
}

