@file:Suppress("SqlResolve")

package r2dbcfun

import dev.minutest.junit.JUnit5Minutests
import dev.minutest.rootContext
import io.r2dbc.spi.Connection
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.Result
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toCollection
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.test.runBlockingTest
import org.flywaydb.core.Flyway
import org.h2.jdbcx.JdbcDataSource
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo


data class User(val id: Int? = null, val name: String, val email: String?)

@ExperimentalCoroutinesApi
class R2dbcTest : JUnit5Minutests {

    fun tests() = rootContext<ConnectionFactory> {
        fixture {
            val dataSource = JdbcDataSource()
            dataSource.setURL("jdbc:h2:mem:r2dbc-test;DB_CLOSE_DELAY=-1")
            val flyway = Flyway.configure().dataSource(dataSource).load()
            flyway.migrate()
            ConnectionFactories.get("r2dbc:h2:mem:///r2dbc-test;DB_CLOSE_DELAY=-1")
        }

        test("can insert values and select result") {
            runBlockingTest {
                val connection: Connection = fixture.create().awaitSingle()
                val firstId =
                    connection.createStatement("insert into USERS(name) values($1)").bind("$1", "belle")
                        .executeInsert()
                val secondId =
                    connection.createStatement("insert into USERS(name) values($1)").bind("$1", "sebastian")
                        .executeInsert()

                val selectResult: Result = connection.createStatement("select * from USERS").execute().awaitSingle()
                val namesFlow = selectResult.map { row, _ -> row.get("NAME", String::class.java) }.asFlow()
                val names = namesFlow.toCollection(mutableListOf())
                expectThat(firstId).isEqualTo(1)
                expectThat(secondId).isEqualTo(2)
                expectThat(names).containsExactly("belle", "sebastian")
            }
        }
        test("can insert data class") {
            runBlockingTest {
                val connection: Connection = fixture.create().awaitSingle()
                val instance = User(name = "chris", email = "my email")
                val user = connection.create(User(name = "chris", email = "my email"), instance::class)
                expectThat(user).isEqualTo(User(3, "chris", "my email"))
            }
        }
    }

}

