package r2dbcfun

import dev.minutest.ContextBuilder
import dev.minutest.junit.JUnit5Minutests
import dev.minutest.rootContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNull


@ExperimentalCoroutinesApi
class R2dbcRepoTest : JUnit5Minutests {
    data class User(val id: Long? = null, val name: String, val email: String?)

    private fun ContextBuilder<R2dbcRepo<User>>.repoTests() {
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
        test("loading data objects") {
            runBlocking {
                fixture.create(User(name = "anotherUser", email = "my email"))
                val id = fixture.create(User(name = "chris", email = "my email")).id!!
                val user = fixture.findById(id)
                expectThat(user).and {
                    get { id }.isEqualTo(id)
                    get { name }.isEqualTo("chris")
                    get { email }.isEqualTo("my email")
                }
            }

        }
    }

    @Suppress("unused")
    fun tests() = rootContext<Unit> {
        derivedContext<R2dbcRepo<User>>("run on postgresql") {
            fixture {
                runBlocking {
                    R2dbcRepo.create<User>(preparePostgreSQL().create().awaitSingle())
                }
            }
            repoTests()
        }
        derivedContext<R2dbcRepo<User>>("run on H2") {
            fixture {
                runBlocking {
                    R2dbcRepo.create<User>(prepareH2().create().awaitSingle())
                }
            }
            repoTests()
        }

    }
}
