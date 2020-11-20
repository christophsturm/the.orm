@file:Suppress("SqlResolve")

package r2dbcfun

import io.kotest.core.spec.style.FunSpec
import io.r2dbc.spi.Result
import kotlinx.coroutines.flow.toCollection
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import r2dbcfun.test.autoClose
import r2dbcfun.test.forAllDatabases
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo

/**
 * this is just the r2dbc playground that started this project.
 *
 * @see r2dbcfun.test.functional.RepositoryFunctionalTest for api usage.
 */
class R2dbcTest : FunSpec({
    forAllDatabases(this, "r2dbctest") { connectionFactory ->
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
            connection.close().awaitFirstOrNull()
        }

    }
})
