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
import kotlinx.coroutines.runBlocking
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo

/**
 * this is just the r2dbc playground that started this project.
 * @see r2dbcfun.R2dbcRepoTest for api usage.
 **/

@ExperimentalCoroutinesApi
class R2dbcTest : JUnit5Minutests {

    @Suppress("unused")
    fun tests() = rootContext<ConnectionFactory> {
        fixture {
            preparePostgreSQL()
        }

        test("can insert values and select result") {
            runBlocking {
                val connection: Connection = fixture.create().awaitSingle()
                val firstId =
                    connection.createStatement("insert into users(name) values($1)").bind("$1", "belle")
                        .executeInsert()
                val secondId =
                    connection.createStatement("insert into users(name) values($1)").bind("$1", "sebastian")
                        .executeInsert()

                val selectResult: Result = connection.createStatement("select * from users").execute().awaitSingle()
                val namesFlow = selectResult.map { row, _ -> row.get("NAME", String::class.java) }.asFlow()
                val names = namesFlow.toCollection(mutableListOf())
                expectThat(firstId).isEqualTo(1)
                expectThat(secondId).isEqualTo(2)
                expectThat(names).containsExactly("belle", "sebastian")
            }
        }
    }


}

