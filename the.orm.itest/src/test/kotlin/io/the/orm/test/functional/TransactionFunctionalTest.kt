package io.the.orm.test.functional

import failgood.Test
import io.the.orm.Repo
import io.the.orm.query.like
import io.the.orm.test.DBS
import io.the.orm.test.describeOnAllDbs
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Test
class TransactionFunctionalTest {
    val context = describeOnAllDbs("Transaction handling", DBS.databases, USERS_SCHEMA) { createConnectionProvider ->
        val repo = Repo.create<User>()
        val userNameLike = repo.queryFactory.createQuery(User::name.like())

        val connectionProvider by dependency({ createConnectionProvider() })
        describe("a transaction started with the repository class") {
            val outerRepo = io.the.orm.TransactionalRepo(repo, connectionProvider)
            it("has transaction isolation") {
                val user = outerRepo.transaction { transactionRepo ->
                    val user =
                        transactionRepo.create(User(name = "a user", email = "with email"))
                    // the created user is visible in the same connection
                    expectThat(
                        userNameLike.with("%").findSingle(transactionRepo.connectionProvider)
                    ).isEqualTo(user)
                    // but the outer connection does not see it
                    expectThat(
                        userNameLike.with("%").find(connectionProvider).count()
                    ).isEqualTo(0)
                    user
                }
                // now the outer connection sees them too
                expectThat(userNameLike.with("%").findSingle(connectionProvider)).isEqualTo(
                    user
                )
            }
            it("rolls back the transaction if the block fails") {
                try {
                    @Suppress("IMPLICIT_NOTHING_TYPE_ARGUMENT_IN_RETURN_POSITION")
                    outerRepo.transaction { transactionRepo ->
                        transactionRepo.create(User(name = "a user", email = "with email"))
                        throw RuntimeException("failed (oops)")
                    }
                } catch (_: Exception) {
                }
                expectThat(userNameLike.with("%").find(connectionProvider).count()).isEqualTo(0)
            }
        }

        describe("a transaction started with the connectionProvider") {
            it("has transaction isolation") {
                val user = connectionProvider.transaction { transactionConnectionProvider ->
                    val user =
                        repo.create(transactionConnectionProvider, User(name = "a user", email = "with email"))
                    // the created user is visible in the same connection
                    expectThat(
                        userNameLike.with("%").findSingle(transactionConnectionProvider)
                    ).isEqualTo(user)
                    // but the outer connection does not see it
                    expectThat(
                        userNameLike.with("%").find(connectionProvider).count()
                    ).isEqualTo(0)
                    user
                }
                // now the outer connection sees them too
                expectThat(userNameLike.with("%").findSingle(connectionProvider)).isEqualTo(
                    user
                )
            }
            it("rolls back the transaction if the block fails") {
                try {
                    connectionProvider.transaction { connectionProvider ->
                        repo.create(connectionProvider, User(name = "a user", email = "with email"))
                        throw RuntimeException("failed (oops)")
                    }
                } catch (_: Exception) {
                }
                expectThat(userNameLike.with("%").find(connectionProvider).count()).isEqualTo(0)
            }
        }
    }
}
