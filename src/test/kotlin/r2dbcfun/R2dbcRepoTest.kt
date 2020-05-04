package r2dbcfun

import dev.minutest.ContextBuilder
import dev.minutest.junit.JUnit5Minutests
import dev.minutest.rootContext
import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.failed
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import strikt.assertions.message


@ExperimentalCoroutinesApi
class R2dbcRepoTest : JUnit5Minutests {
    data class User(val id: Long? = null, val name: String, val email: String?, val isCool: Boolean = false)

    private fun ContextBuilder<ConnectionFactory>.repoTests() {
        derivedContext<R2dbcRepo<User>>("a repo with a data class") {
            deriveFixture {
                val db = this
                runBlocking {
                    R2dbcRepo.create<User>(db.create().awaitSingle())
                }
            }
            context("Creating Rows") {
                test("can insert data class and return primary key") {
                    runBlocking {
                        val user = fixture.create(User(name = "chris", email = "my email"))
                        expectThat(user).and {
                            get { id }.isEqualTo(1)
                            get { name }.isEqualTo("chris")
                            get { email }.isEqualTo("my email")
                        }
                    }
                }

                test("supports nullable values") {
                    runBlocking {
                        val user = fixture.create(User(name = "chris", email = null))
                        expectThat(user).and {
                            get { id }.isEqualTo(1)
                            get { name }.isEqualTo("chris")
                            get { email }.isNull()
                        }
                    }
                }
            }
            context("loading data objects") {
                test("can load data object by id") {
                    runBlocking {
                        fixture.create(User(name = "anotherUser", email = "my email"))
                        val id = fixture.create(User(name = "chris", email = "my email")).id!!
                        val user = fixture.findById(id)
                        expectThat(user).and {
                            get { id }.isEqualTo(id)
                            get { name }.isEqualTo("chris")
                            get { email }.isEqualTo("my email")
                            get { isCool }.isFalse()
                        }
                    }
                }
                test("throws NotFoundException when id does not exist") {
                    runBlocking {
                        expectCatching {
                            fixture.findById(1)
                        }.failed().isA<NotFoundException>().message.isNotNull().isEqualTo("No users found for id 1")

                    }

                }

            }
        }
        context("fail fast error handling") {
            test("fails fast if id type is not Long") {
                data class Mismatch(val id: Int?)
                runBlocking {
                    expectCatching {
                        R2dbcRepo.create<Mismatch>(fixture.create().awaitSingle())
                    }.failed().isA<R2dbcRepoException>().message.isNotNull()
                        .contains("Id Column type was class kotlin.Int, but must be class kotlin.Long")
                }
            }
        }

    }

    @Suppress("unused")
    fun tests() = rootContext<Unit> {
        derivedContext<ConnectionFactory>("run on H2") {
            fixture {
                prepareH2()
            }
            repoTests()
        }
        derivedContext<ConnectionFactory>("run on postgresql") {
            fixture {
                preparePostgreSQL()
            }
            repoTests()
        }

    }
}
