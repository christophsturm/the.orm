package r2dbcfun.dbio

import failfast.FailFast
import r2dbcfun.test.DBS
import r2dbcfun.test.describeOnAllDbs
import strikt.api.expectThat
import strikt.assertions.isEqualTo

fun main() {
    FailFast.runTest()
}

object DBConnectionTest {
    val context = describeOnAllDbs("DBConnection::class", DBS.databases) { createConnectionProvider ->
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
        pending("can insert multiple rows with one command") {
            val result =
                createConnectionProvider().withConnection { connection ->
                    connection.createInsertStatement("insert into users(name, email) values ($1, $2)")
                        .executeBatch(
                            listOf(String::class.java, String::class.java),
                            sequenceOf(sequenceOf("belle", null), sequenceOf("sebastian", null))
                        )
                        .getId()
                }
            expectThat(result).isEqualTo(1)
        }

    }
}
