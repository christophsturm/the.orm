package r2dbcfun

import dev.minutest.junit.JUnit5Minutests
import dev.minutest.rootContext
import io.r2dbc.spi.Connection
import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.test.runBlockingTest
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isGreaterThan
import strikt.assertions.isNotNull

data class User(val id: Int? = null, val name: String, val email: String?)

@ExperimentalCoroutinesApi
class R2dbcRepoTest : JUnit5Minutests {

    fun tests() = rootContext<ConnectionFactory> {
        fixture {
            prepareDB()
        }
        test("can insert data class") {
            runBlockingTest {
                val connection: Connection = fixture.create().awaitSingle()
                val instance = User(name = "chris", email = "my email")
                val user = connection.create(User(name = "chris", email = "my email"), instance::class)
                expectThat(user).and {
                    get { id }.isNotNull().isGreaterThan(0)
                    get { name }.isEqualTo("chris")
                    get { email }.isEqualTo("my email")
                }
            }
        }

    }
}
