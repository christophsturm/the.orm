package r2dbcfun.test.functional

import io.kotest.core.spec.style.FunSpec
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.toCollection
import r2dbcfun.Repository
import r2dbcfun.forAllDatabases
import r2dbcfun.query.between
import r2dbcfun.query.isEqualTo
import r2dbcfun.query.isNull
import r2dbcfun.query.like
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo
import java.time.LocalDate

class QueryFactoryFunctionalTest : FunSpec({
    forAllDatabases(this) { connection ->
        val repo = Repository.create<User>()
        suspend fun create(instance: User) = repo.create(connection, instance)

        context("query language") {
            test("has a typesafe query api") {
                // create 3 users with different birthdays so that only the
                // middle date
                // fits the between condition
                create(
                    User(
                        name = "chris",
                        email = "my email",
                        birthday = LocalDate.parse("2020-06-18")
                    )
                )
                val userThatWillBeFound =
                    create(
                        User(
                            name = "jakob",
                            email = "different email",
                            birthday = LocalDate.parse("2020-06-20")
                        )
                    )
                create(
                    User(
                        name = "chris",
                        email = "different email",
                        birthday = LocalDate.parse("2020-06-22")
                    )
                )
                val date1 = LocalDate.parse("2020-06-19")
                val date2 = LocalDate.parse("2020-06-21")
                val findByUserNameLikeAndBirthdayBetween =
                    repo.queryFactory
                        .createQuery(User::name.like(), User::birthday.between())

                expectThat(
                    findByUserNameLikeAndBirthdayBetween(
                        connection,
                        "%",
                        Pair(date1, date2)
                    ).toCollection(mutableListOf())
                ).containsExactly(userThatWillBeFound)
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
                expectThat(findByNullCoolness(connection, Unit).single())
                    .isEqualTo(userOfUndefinedCoolness)
                // this does not compile because equaling by null makes no sense
                // anyway:
                // expectThat(findByCoolness(connection,
                // null).single()).isEqualTo(userOfUndefinedCoolness)
                expectThat(findByCoolness(connection, false).single())
                    .isEqualTo(uncoolUser)
                expectThat(findByCoolness(connection, true).single())
                    .isEqualTo(coolUser)
            }
        }

    }

})
