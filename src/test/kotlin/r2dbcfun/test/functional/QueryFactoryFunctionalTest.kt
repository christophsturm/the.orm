package r2dbcfun.test.functional

import io.kotest.core.spec.style.FunSpec
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.toCollection
import kotlinx.coroutines.reactive.awaitSingle
import r2dbcfun.NotFoundException
import r2dbcfun.Repository
import r2dbcfun.query.between
import r2dbcfun.query.isEqualTo
import r2dbcfun.query.isNull
import r2dbcfun.query.like
import r2dbcfun.test.autoClose
import r2dbcfun.test.forAllDatabases
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.isEqualTo
import java.time.LocalDate

class QueryFactoryFunctionalTest : FunSpec({
    forAllDatabases(this, "QueryFactoryFT") { connectionFactory ->
        val connection = autoClose(connectionFactory.create().awaitSingle()) { it.close() }

        val repo = Repository.create<User>()
        suspend fun create(instance: User) = repo.create(connection, instance)

        val user = User(
            name = "a user",
            email = "with email"
        )
        context("query language") {
            test("has a typesafe query api") {
                val usersPerMonth = (1 until 12).map {
                    create(
                        user.copy(name = "user with birthday in month $it", birthday = LocalDate.of(2000, it, 1))
                    )
                }
                val findByUserNameLikeAndBirthdayBetween =
                    repo.queryFactory
                        .createQuery(User::name.like(), User::birthday.between())

                expectThat(
                    findByUserNameLikeAndBirthdayBetween.with(
                        connection,
                        "%",
                        Pair(LocalDate.of(2000, 4, 2), LocalDate.of(2000, 6, 2))
                    ).find().toCollection(mutableListOf())
                ).containsExactlyInAnyOrder(usersPerMonth[4], usersPerMonth[5])
            }
            test("can query null values") {
                val coolUser =
                    create(User(name = "coolUser", email = "email", isCool = true))
                val uncoolUser =
                    create(
                        User(name = "uncoolUser", email = "email", isCool = false)
                    )
                val userOfUndefinedCoolness =
                    create(
                        User(
                            name = "userOfUndefinedCoolness",
                            email = "email",
                            isCool = null
                        )
                    )
                val findByCoolness =
                    repo.queryFactory.createQuery(User::isCool.isEqualTo())
                val findByNullCoolness =
                    repo.queryFactory.createQuery(User::isCool.isNull())
                expectThat(findByNullCoolness.with(connection, Unit).find().single())
                    .isEqualTo(userOfUndefinedCoolness)
                // this does not compile because equaling by null makes no sense
                // anyway:
                // expectThat(findByCoolness(connection,
                // null).single()).isEqualTo(userOfUndefinedCoolness)
                expectThat(findByCoolness.with(connection, false).find().single())
                    .isEqualTo(uncoolUser)
                expectThat(findByCoolness.with(connection, true).find().single())
                    .isEqualTo(coolUser)
            }
            test("can delete by query") {
                val kurt = create(user.copy(name = "kurt"))
                val freddi = create(user.copy(name = "freddi"))
                val queryByName = repo.queryFactory.createQuery(User::name.isEqualTo())

                expectThat(queryByName.with(connection, "kurt").delete()).isEqualTo(1)
                expectThrows<NotFoundException> { repo.findById(connection, kurt.id!!) }
                repo.findById(connection, freddi.id!!)

            }
        }

    }

})
