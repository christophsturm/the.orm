package r2dbcfun.test.functional

import failfast.FailFast
import failfast.describe
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.single
import r2dbcfun.ConnectedRepository
import r2dbcfun.Repository
import r2dbcfun.query.like
import r2dbcfun.test.DBS
import r2dbcfun.test.forAllDatabases
import strikt.api.expectThat
import strikt.assertions.isEqualTo

fun main() {
    FailFast.runTest()
}

object TransactionFunctionalTest {
    val context = describe("FT: The transaction handling") {
        val repo = Repository.create<User>()
        val userNameLike = repo.queryFactory.createQuery(User::name.like())

        forAllDatabases(DBS.databases) { createConnectionProvider ->
            val connectionProvider = createConnectionProvider()
            describe("a transaction started with the repository class") {
                val outerRepo = ConnectedRepository(repo, connectionProvider)
                it("has transaction isolation") {
                    val user = outerRepo.transaction { transactionRepo ->
                        val user =
                            transactionRepo.create(User(name = "a user", email = "with email"))
                        // the created user is visible in the same connection
                        expectThat(
                            userNameLike.with(transactionRepo.connectionProvider, "%").find().single()
                        ).isEqualTo(user)
                        // but the outer connection does not see it
                        expectThat(
                            userNameLike.with(connectionProvider, "%").find().count()
                        ).isEqualTo(0)
                        user
                    }
                    // now the outer connection sees them too
                    expectThat(userNameLike.with(connectionProvider, "%").find().single()).isEqualTo(
                        user
                    )
                }
                it("rolls back the transaction if the block fails") {
                    try {
                        outerRepo.transaction<Nothing> { transactionRepo ->
                            transactionRepo.create(User(name = "a user", email = "with email"))
                            throw RuntimeException("failed (oops)")
                        }
                    } catch (e: Exception) {
                    }
                    expectThat(userNameLike.with(connectionProvider, "%").find().count()).isEqualTo(0)
                }

            }

            describe("a transaction started with the connectionProvider") {
                it("has transaction isolation") {
                    val user = connectionProvider.transaction { transactionConnectionProvider ->
                        val user =
                            repo.create(transactionConnectionProvider, User(name = "a user", email = "with email"))
                        // the created user is visible in the same connection
                        expectThat(
                            userNameLike.with(transactionConnectionProvider, "%").find().single()
                        ).isEqualTo(user)
                        // but the outer connection does not see it
                        expectThat(
                            userNameLike.with(connectionProvider, "%").find().count()
                        ).isEqualTo(0)
                        user
                    }
                    // now the outer connection sees them too
                    expectThat(userNameLike.with(connectionProvider, "%").find().single()).isEqualTo(
                        user
                    )
                }
                it("rolls back the transaction if the block fails") {
                    try {
                        connectionProvider.transaction { connectionProvider ->
                            repo.create(connectionProvider, User(name = "a user", email = "with email"))
                            throw RuntimeException("failed (oops)")
                        }
                    } catch (e: Exception) {
                    }
                    expectThat(userNameLike.with(connectionProvider, "%").find().count()).isEqualTo(0)
                }

            }
        }
    }
}

