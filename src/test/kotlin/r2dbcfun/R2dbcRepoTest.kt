package r2dbcfun

import dev.minutest.ContextBuilder
import dev.minutest.junit.JUnit5Minutests
import dev.minutest.junit.experimental.applyRule
import dev.minutest.rootContext
import io.r2dbc.spi.Connection
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.debug.junit4.CoroutinesTimeout
import kotlinx.coroutines.flow.toCollection
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import reactor.blockhound.BlockHound
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isFalse
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import strikt.assertions.message


@ExperimentalCoroutinesApi
class R2dbcRepoTest : JUnit5Minutests {
    init {
        BlockHound.install()
    }

    private val characters = ('A'..'Z').toList() + (('a'..'z').toList()).plus(' ')
    private val reallyLongString = (1..20000).map { characters.random() }.joinToString("")

    data class UserPK(override val id: Long) : PK

    // HasPK interface it optional
    interface HasPK {
        val id: PK?
    }

    data class User(
        override val id: UserPK? = null,
        val name: String,
        val email: String?,
        val isCool: Boolean = false,
        val bio: String? = null
    ) : HasPK

    private fun ContextBuilder<Connection>.repoTests() {
        class Fixture(connection: Connection) {
            val repo = R2dbcRepo.create<User, UserPK>(connection)
            val timeout = CoroutinesTimeout(if (System.getenv("CI") != null) 5000 else 500)
        }
        derivedContext<Fixture>("a repo with a data class") {
            applyRule { timeout }
            deriveFixture {
                val connection = this
                runBlocking {
                    Fixture(connection)
                }
            }
            context("Creating Rows") {
                test("can insert data class and return primary key") {
                    runBlocking {
                        val user = repo.create(User(name = "chris", email = "my email", bio = reallyLongString))
                        expectThat(user).and {
                            get { id }.isEqualTo(UserPK(1))
                            get { name }.isEqualTo("chris")
                            get { email }.isEqualTo("my email")
                        }
                    }
                }

                test("supports nullable values") {
                    runBlocking {
                        val user = repo.create(User(name = "chris", email = null))
                        expectThat(user).and {
                            get { id }.isEqualTo(UserPK(1))
                            get { name }.isEqualTo("chris")
                            get { email }.isNull()
                        }
                    }
                }
            }
            context("loading data objects") {
                test("can load data object by id") {
                    runBlocking {
                        repo.create(User(name = "anotherUser", email = "my email"))
                        val id = repo.create(User(name = "chris", email = "my email", bio = reallyLongString)).id!!
                        val user = repo.findById(id)
                        expectThat(user).and {
                            get { id }.isEqualTo(id)
                            get { name }.isEqualTo("chris")
                            get { email }.isEqualTo("my email")
                            get { isCool }.isFalse()
                            get { bio }.isEqualTo(reallyLongString)
                        }
                    }
                }
                test("can load data object by field value") {
                    runBlocking {
                        val firstUser = repo.create(User(name = "chris", email = "my email"))
                        val secondUser = repo.create(User(name = "chris", email = "different email"))
                        val users = repo.findBy(User::name, "chris").toCollection(mutableListOf())
                        expectThat(users).containsExactlyInAnyOrder(firstUser, secondUser)
                    }
                }
                test("throws NotFoundException when id does not exist") {
                    runBlocking {
                        expectCatching {
                            repo.findById(UserPK(1))
                        }.isFailure().isA<NotFoundException>().message.isNotNull().isEqualTo("No users found for id 1")

                    }
                }
            }
            context("updating objects") {
                test("can update objects") {
                    val originalUser = User(name = "chris", email = "my email", bio = reallyLongString)
                    runBlocking {
                        val id = repo.create(originalUser).id!!
                        val readBackUser = repo.findById(id)
                        repo.update(readBackUser.copy(name = "updated name", email = null))
                        val readBackUpdatedUser = repo.findById(id)
                        expectThat(readBackUpdatedUser).isEqualTo(
                            originalUser.copy(
                                id = id,
                                name = "updated name",
                                email = null
                            )
                        )
                    }

                }
            }
        }
        context("fail fast error handling") {
            test("fails fast if PK has more than one constructor") {
                data class MismatchPK(override val id: Long, val blah: String) : PK
                data class Mismatch(val id: MismatchPK)
                runBlocking {
                    expectCatching {
                        R2dbcRepo.create<Mismatch, MismatchPK>(fixture)
                    }.isFailure().isA<R2dbcRepoException>().message.isNotNull()
                        .contains("PK classes must have a single field of type long")
                }
            }
        }

    }

    @Suppress("unused")
    fun tests() = rootContext<Unit> {
        derivedContext<Connection>("run on H2") {
            fixture {
                runBlocking {
                    prepareH2().create().awaitSingle()
                }
            }
            repoTests()
            after {
                runBlocking {
                    fixture.close().awaitFirstOrNull()
                }
            }
        }
        derivedContext<Connection>("run on postgresql") {
            fixture {
                runBlocking {
                    preparePostgreSQL().create().awaitSingle()
                }
            }
            repoTests()
            after {
                runBlocking {
                    fixture.close().awaitFirstOrNull()
                }
            }
        }

    }
}

