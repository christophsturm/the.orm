package r2dbcfun

import dev.minutest.junit.JUnit5Minutests
import dev.minutest.rootContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNull


@ExperimentalCoroutinesApi
class R2dbcRepoTest : JUnit5Minutests {
    data class User(val id: Int? = null, val name: String, val email: String?)

    @Suppress("unused")
    fun tests() = rootContext<R2dbcRepo<User>> {
        fixture {
            runBlocking {
                R2dbcRepo.create<User>(prepareDB().create().awaitSingle())
            }
        }
        context("Creating Rows") {
            test("can insert data class and return primary key") {
                runBlockingTest {
                    val user = fixture.create(User(name = "chris", email = "my email"))
                    expectThat(user).and {
                        get { id }.isEqualTo(1)
                        get { name }.isEqualTo("chris")
                        get { email }.isEqualTo("my email")
                    }
                }
            }

            test("supports nullable values") {
                runBlockingTest {
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
            runBlockingTest {
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
}
