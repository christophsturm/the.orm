@file:Suppress("SqlResolve")

package r2dbcfun.exp

import failfast.describe
import io.r2dbc.spi.ConnectionFactories
import kotlinx.coroutines.flow.toCollection
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
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
        test("can open and close pool") {
            val (databaseName, host, port) = DBS.psql13.preparePostgresDB()
            val factory =
                ConnectionFactories.get("r2dbc:pool:postgresql://test:test@$host:$port/$databaseName?initialSize=1")
            val connection1 = factory.create().awaitSingle()
            val connection2 = factory.create().awaitSingle()
            connection1.createStatement("select * from users").execute().awaitSingle()
            connection2.createStatement("select * from users").execute().awaitSingle()
            connection1.close().awaitFirstOrNull()
            connection2.close().awaitFirstOrNull()

        }

    }
}
