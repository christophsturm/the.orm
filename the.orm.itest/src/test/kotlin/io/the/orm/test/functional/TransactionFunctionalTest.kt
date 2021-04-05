package io.the.orm.test.functional

import failfast.FailFast
import io.the.orm.query.like
import io.the.orm.test.DBS
import io.the.orm.test.describeOnAllDbs
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.single
import strikt.api.expectThat
import strikt.assertions.isEqualTo

fun main() {
    FailFast.runTest()
}

object TransactionFunctionalTest {
    val context = describeOnAllDbs("Transaction handling", DBS.databases) { createConnectionProvider ->
        val repo = io.the.orm.Repository.create<User>()
        val userNameLike = repo.queryFactory.createQuery(User::name.like())

        val connectionProvider by dependency({ createConnectionProvider() })
        describe("a transaction started with the repository class") {
            val outerRepo = io.the.orm.TransactionalRepository(repo, connectionProvider)
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

