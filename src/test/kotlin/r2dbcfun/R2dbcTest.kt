@file:Suppress("SqlResolve")

package r2dbcfun

import dev.minutest.junit.JUnit5Minutests
import dev.minutest.rootContext
import io.r2dbc.spi.Connection
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.Result
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toCollection
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.test.runBlockingTest
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo


@ExperimentalCoroutinesApi
class R2dbcTest : JUnit5Minutests {

    fun tests() = rootContext<ConnectionFactory> {
        fixture {
            prepareDB()
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
    }


}

