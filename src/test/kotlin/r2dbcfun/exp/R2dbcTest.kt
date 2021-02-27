@file:Suppress("SqlResolve")

package r2dbcfun.exp

import failfast.describe
import kotlinx.coroutines.flow.toCollection
import r2dbcfun.test.DBS
import r2dbcfun.test.forAllDatabases
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
        { createConnectionProvider ->
            val connection = createConnectionProvider()
            test("can insert values and select result") {
                val firstId =
                    connection.dbConnection.createStatement("insert into users(name) values($1)")
                        .bind("$1", "belle")
                        .executeInsert()
                val secondId =
                    connection.dbConnection.createStatement("insert into users(name) values($1)")
                        .bind("$1", "sebastian")
                        .executeInsert()

                val selectResult =
                    connection.dbConnection.createStatement("select * from users").execute()
                val namesFlow =
                    selectResult.map { row -> row.get("NAME", String::class.java) as String }
                val names = namesFlow.toCollection(mutableListOf())
                expectThat(firstId).isEqualTo(1)
                expectThat(secondId).isEqualTo(2)
                expectThat(names).containsExactly("belle", "sebastian")
            }

        }

    }
}
