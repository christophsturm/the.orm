package io.the.orm.test.functional

import failgood.Test
import io.the.orm.PK
import io.the.orm.Repo
import io.the.orm.dbio.TransactionProvider
import io.the.orm.test.DBS
import io.the.orm.test.describeOnAllDbs
import kotlinx.coroutines.flow.single
import kotlinx.serialization.Serializable
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isFalse
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import strikt.assertions.message
import java.math.BigDecimal
import java.time.LocalDate

@Serializable
data class SerializableUser(
    val id: PK? = null,
    val name: String,
    val email: String?
)
private const val SCHEMA = """
    $USERS_SCHEMA
create sequence serializable_users_id_seq no maxvalue;

create table serializable_users
(
    id    bigint       not null default nextval('serializable_users_id_seq') primary key,
    name  varchar(100) not null,
    email varchar(100)
);


"""

@Test
class RepositoryFunctionalTest {
    private val characters = ('A'..'Z').toList() + (('a'..'z').toList()).plus(' ')
    private val reallyLongString = (1..20000).map { characters.random() }.joinToString("")
    val context = describeOnAllDbs("the repository class", DBS.databases,
        SCHEMA
    ) { createConnectionProvider ->
        val connection: TransactionProvider by dependency({ createConnectionProvider() })
        context("with a user class") {
            val repo = Repo.create<User>()
            context("Creating Rows") {
                test("can insert data class and return primary key") {
                    val user =
                        repo.create(
                            connection,
                            User(
                                name = "chris",
                                email = "my email",
                                bio = "bio",
                                birthday = LocalDate.parse("2020-06-20"),
                                weight = 3.14,
                                balance = BigDecimal("3.14")

                            )
                        )
                    expectThat(user) {
                        get { id }.isEqualTo(1)
                        get { name }.isEqualTo("chris")
                        get { email }.isEqualTo("my email")
                        get { birthday }.isEqualTo(LocalDate.parse("2020-06-20"))
                        get { balance }.isEqualTo(BigDecimal("3.14"))
                        get { weight }.isEqualTo(3.14)
                    }
                }
                test("supports nullable values") {
                    val user =
                        repo.create(
                            connection,
                            User(
                                name = "chris",
                                email = null,
                                birthday = LocalDate.parse("2020-06-20")
                            )
                        )
                    expectThat(user) {
                        get { id }.isEqualTo(1)
                        get { name }.isEqualTo("chris")
                        get { email }.isNull()
                    }
                }
            }
            context("loading data objects") {
                test("can load data object by id") {
                    repo.create(
                        connection,
                        User(
                            name = "anotherUser",
                            email = "anoher email",
                            birthday = LocalDate.parse("2020-06-20")
                        )
                    )
                    val id =
                        repo
                            .create(
                                connection,
                                User(
                                    name = "chris",
                                    email = "chris' email",
                                    isCool = false,
                                    bio = reallyLongString,
                                    favoriteColor = Color.RED,
                                    birthday = LocalDate.parse("2020-06-20"),
                                    weight = 3.14,
                                    balance = BigDecimal("3.14")
                                )
                            )
                            .id!!
                    val user = repo.findById(connection, id)
                    expectThat(user) {
                        get { id }.isEqualTo(id)
                        get { name }.isEqualTo("chris")
                        get { email }.isEqualTo("chris' email")
                        get { isCool }.isFalse()
                        get { bio }.isEqualTo(reallyLongString)
                        get { favoriteColor }.isEqualTo(Color.RED)
                        get { birthday }.isEqualTo(LocalDate.parse("2020-06-20"))
                        get { balance }.isEqualTo(BigDecimal("3.14"))
                        get { weight }.isEqualTo(3.14)
                    }
                }

                test("throws NotFoundException when id does not exist") {
                    expectCatching { repo.findById(connection, 1) }.isFailure()
                        .isA<io.the.orm.NotFoundException>()
                        .message
                        .isNotNull()
                        .isEqualTo("No users found for id 1")
                }
            }
            context("updating objects") {
                test("can update objects") {
                    val originalUser =
                        User(
                            name = "chris",
                            email = "my email",
                            bio = "bio",
                            birthday = LocalDate.parse("2020-06-20")
                        )
                    val id = repo.create(connection, originalUser).id!!
                    val readBackUser = repo.findById(connection, id)
                    repo.update(
                        connection,
                        readBackUser.copy(name = "updated name", email = null)
                    )
                    val readBackUpdatedUser = repo.findById(connection, id)
                    expectThat(readBackUpdatedUser)
                        .isEqualTo(
                            originalUser.copy(id = id, name = "updated name", email = null)
                        )
                }
            }
            context("enum fields") {
                test("enum fields are serialized as upper case strings") {
                    val id =
                        repo
                            .create(
                                connection,
                                User(
                                    name = "chris",
                                    email = "my email",
                                    isCool = false,
                                    bio = "bio",
                                    favoriteColor = Color.RED,
                                    birthday = LocalDate.parse("2020-06-20")
                                )
                            )
                            .id!!

                    @Suppress("SqlResolve")
                    val color =
                        connection.withConnection { connection ->
                            connection.createStatement("select * from Users where id = $1")
                                .execute(listOf(Long::class.java), listOf(id))
                                .map { row ->
                                    row.get(
                                        "favorite_color",
                                        String::class.java
                                    )!!
                                }.single()
                        }
                    expectThat(color).isEqualTo("RED")
                }
            }
        }
        context("interop with kotlinx.serializable") {
            val repo = Repo.create<SerializableUser>()

            test("can insert data class and return primary key") {
                val user =
                    repo.create(
                        connection,
                        SerializableUser(name = "chris", email = "my email")
                    )
                expectThat(user) {
                    get { id }.isEqualTo(1)
                    get { name }.isEqualTo("chris")
                    get { email }.isEqualTo("my email")
                }
            }
        }
    }
}
