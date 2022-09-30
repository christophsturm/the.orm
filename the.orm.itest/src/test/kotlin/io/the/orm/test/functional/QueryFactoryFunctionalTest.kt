@file:Suppress("NAME_SHADOWING")

package io.the.orm.test.functional

import failgood.Test
import io.the.orm.ConnectedRepository
import io.the.orm.NotFoundException
import io.the.orm.PK
import io.the.orm.Repository
import io.the.orm.query.QueryFactory
import io.the.orm.query.between
import io.the.orm.query.isEqualTo
import io.the.orm.query.isNull
import io.the.orm.query.like
import io.the.orm.test.DBS
import io.the.orm.test.describeOnAllDbs
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import java.time.LocalDate
import kotlin.reflect.KProperty1

data class Vegetable(val id: Long? = null, val name: String, val weight: Double? = null)

private const val SCHEMA = """
$USERS_SCHEMA
create sequence vegetables_id_seq no maxvalue;
create table vegetables
(
    id     bigint      not null default nextval('vegetables_id_seq') primary key,
    name   varchar(20) not null unique,
    weight decimal(5, 2)
);

"""

@Test
class QueryFactoryFunctionalTest {
    val context = describeOnAllDbs("support for querying data", DBS.databases, SCHEMA) { createConnectionProvider ->
        val connectionProvider by dependency({ createConnectionProvider() })

        val repo = Repository.create<User>()
        suspend fun create(instance: User) = repo.create(connectionProvider, instance)

        val user = User(
            name = "a user",
            email = "with email"
        )
        describe("query language") {
            describe("has a typesafe query api") {
                val usersPerMonth = (1 until 12).map {
                    create(
                        user.copy(
                            name = "user with birthday in month $it",
                            birthday = LocalDate.of(2000, it, 1),
                            email = java.util.UUID.randomUUID().toString()
                        )
                    )
                }
                it("can query by condition with one and two parameters") {
                    val findByUserNameLikeAndBirthdayBetween =
                        repo.queryFactory.createQuery(User::name.like(), User::birthday.between())

                    expectThat(
                        findByUserNameLikeAndBirthdayBetween.with(
                            connectionProvider, "%",
                            Pair(LocalDate.of(2000, 4, 2), LocalDate.of(2000, 6, 2))
                        ).find().toCollection(mutableListOf())
                    ).containsExactlyInAnyOrder(usersPerMonth[4], usersPerMonth[5])
                }
                // works on R2DBC psql and vertx psql but not on H2
                ignore("can query by list parameters with unnest") {
                    fun <T> KProperty1<T, PK?>.`in`(): QueryFactory.Condition<Array<Long>> =
                        QueryFactory.Condition("in (select unnest((?)::bigint[]))", this)

                    val findIdIn = repo.queryFactory.createQuery(User::id.`in`())

                    expectThat(
                        findIdIn.with(
                            connectionProvider,
                            arrayOf(usersPerMonth[4].id!!.id, usersPerMonth[5].id!!.id)
                        ).find().toCollection(mutableListOf())
                    ).containsExactlyInAnyOrder(usersPerMonth[4], usersPerMonth[5])
                }
                it("can query by list parameters with ANY") {
                    fun <T> KProperty1<T, PK?>.`in`(): QueryFactory.Condition<Array<Long>> =
                        QueryFactory.Condition("= ANY(?)", this)

                    val findIdIn = repo.queryFactory.createQuery(User::id.`in`())

                    expectThat(
                        findIdIn.with(
                            connectionProvider,
                            arrayOf(usersPerMonth[4].id!!.id, usersPerMonth[5].id!!.id)
                        ).find().toCollection(mutableListOf())
                    ).containsExactlyInAnyOrder(usersPerMonth[4], usersPerMonth[5])
                }
            }
            test("can query null values") {
                val coolUser =
                    create(User(name = "coolUser", email = "email1", isCool = true))
                val uncoolUser =
                    create(
                        User(name = "uncoolUser", email = "email2", isCool = false)
                    )
                val userOfUndefinedCoolness =
                    create(
                        User(
                            name = "userOfUndefinedCoolness",
                            email = "email3",
                            isCool = null
                        )
                    )
                val findByCoolness =
                    repo.queryFactory.createQuery(User::isCool.isEqualTo())
                val findByNullCoolness =
                    repo.queryFactory.createQuery(User::isCool.isNull())
                expectThat(findByNullCoolness.with(connectionProvider, Unit).findSingle())
                    .isEqualTo(userOfUndefinedCoolness)
                // this does not compile because equaling by null makes no sense
                // anyway:
                // expectThat(findByCoolness(connection,
                // null).single()).isEqualTo(userOfUndefinedCoolness)
                expectThat(findByCoolness.with(connectionProvider, false).findSingle())
                    .isEqualTo(uncoolUser)
                expectThat(findByCoolness.with(connectionProvider, true).findSingle())
                    .isEqualTo(coolUser)
            }
            test("can delete by query") {
                val kurt = create(user.copy(name = "kurt", email = "kurti@email.com"))
                val freddi = create(user.copy(name = "freddi", email = "freddi@email.com"))
                val queryByName = repo.queryFactory.createQuery(User::name.isEqualTo())

                expectThat(queryByName.with(connectionProvider, "kurt").delete()).isEqualTo(1)
                expectThrows<NotFoundException> { repo.findById(connectionProvider, kurt.id!!) }
                repo.findById(connectionProvider, freddi.id!!)
            }
            describe("findOrCreate") {
                val repo = ConnectedRepository.create<Vegetable>(connectionProvider)
                val carrot = repo.create(Vegetable(name = "carrot"))
                val queryByName = repo.repository.queryFactory.createQuery(Vegetable::name.isEqualTo())
                it("finds an existing entity") {
                    expectThat(
                        queryByName.with(connectionProvider, "carrot")
                            .findOrCreate { throw RuntimeException() }).isEqualTo(
                        carrot
                    )
                }
                it("creates a new entity if it does not yet exist") {
                    expectThat(
                        queryByName.with(connectionProvider, "tomato")
                            .findOrCreate { Vegetable(name = "tomato") }) {
                        get { id }.isNotNull()
                        get { name }.isEqualTo("tomato")
                    }
                }
            }
            describe("createOrUpdate") {
                val repo = ConnectedRepository.create<Vegetable>(connectionProvider)
                val queryByName = repo.repository.queryFactory.createQuery(Vegetable::name.isEqualTo())
                it("creates a new entity if it does not yet exist") {
                    expectThat(
                        queryByName.with(connectionProvider, "tomato").createOrUpdate(Vegetable(name = "tomato"))
                    ) {
                        get { id }.isNotNull()
                        get { name }.isEqualTo("tomato")
                    }
                }
                it("updates the existing entity if it already exists") {
                    val carrot = repo.create(Vegetable(name = "carrot", weight = 10.0))
                    expectThat(
                        queryByName.with(connectionProvider, "carrot")
                            .createOrUpdate(Vegetable(null, "carrot", weight = 20.0))
                    ) {
                        get { id }.isEqualTo(carrot.id)
                        get { name }.isEqualTo("carrot")
                        get { weight }.isEqualTo(20.0)
                    }
                }
            }
        }
    }
}
