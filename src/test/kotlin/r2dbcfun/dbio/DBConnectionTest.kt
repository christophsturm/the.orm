package r2dbcfun.dbio

import failfast.FailFast
import failfast.describe
import r2dbcfun.test.DBS
import r2dbcfun.test.forAllDatabases
import strikt.api.expectThat
import strikt.assertions.isEqualTo

fun main() {
    FailFast.runTest()
}

object DBConnectionTest {
    val context = describe(DBConnection::class) {
        forAllDatabases(DBS) { createConnectionProvider ->
            val connection = createConnectionProvider().dbConnection
            it("can insert with autoincrement") {
                val result =
                    connection.createStatement("insert into users(name) values ($1)").bind(0, "belle")
                        .executeInsert(listOf(String::class.java), sequenceOf("belle"))
                expectThat(result).isEqualTo(1)
            }

        }
    }
}
