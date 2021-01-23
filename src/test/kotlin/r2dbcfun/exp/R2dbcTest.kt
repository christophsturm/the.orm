@file:Suppress("SqlResolve")

package r2dbcfun.exp

import failfast.describe
import failfast.r2dbc.forAllDatabases
import io.r2dbc.spi.Result
import kotlinx.coroutines.flow.toCollection
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitSingle
import r2dbcfun.executeInsert
import r2dbcfun.test.DBS
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo

/**
 * this is just the r2dbc playground that started this project.
 *
 * @see r2dbcfun.test.functional.RepositoryFunctionalTest for api usage.
 */
object R2dbcTest {
    val context = describe("the r2dbc api") {
        forAllDatabases(DBS)
        { connectionFactory ->
            val connection = autoClose(connectionFactory.create().awaitSingle()) { it.close() }

            test("can insert values and select result") {
                val firstId =
                    connection.createStatement("insert into users(name) values($1)")
                        .bind("$1", "belle")
                        .executeInsert()
                val secondId =
                    connection.createStatement("insert into users(name) values($1)")
                        .bind("$1", "sebastian")
                        .executeInsert()

                val selectResult: Result =
                    connection.createStatement("select * from users").execute().awaitSingle()
                val namesFlow =
                    selectResult.map { row, _ -> row.get("NAME", String::class.java) as String }.asFlow()
                val names = namesFlow.toCollection(mutableListOf())
                expectThat(firstId).isEqualTo(1)
                expectThat(secondId).isEqualTo(2)
                expectThat(names).containsExactly("belle", "sebastian")
            }

        }

    }
}
