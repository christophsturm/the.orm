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
        forAllDatabases(DBS, DBS.unstableDatabases) { createConnectionProvider ->
            it("can insert with autoincrement") {
                val result =
                    createConnectionProvider().withConnection { connection ->
                        connection.createInsertStatement("insert into users(name) values ($1)")
                            .execute(listOf(String::class.java), sequenceOf("belle")).getId()
                    }
                expectThat(result).isEqualTo(1)
            }
            it("can insert null values with autoincrement") {
                val result =
                    createConnectionProvider().withConnection { connection ->
                        connection.createInsertStatement("insert into users(name, email) values ($1, $2)")
                            .execute(listOf(String::class.java, String::class.java), sequenceOf("belle", null))
                            .getId()
                    }
                expectThat(result).isEqualTo(1)
            }
        }
    }
}
