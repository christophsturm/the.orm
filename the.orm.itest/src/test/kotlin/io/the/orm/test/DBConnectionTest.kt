package io.the.orm.test

import failgood.Test
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import strikt.api.expectThat
import strikt.assertions.isEqualTo


@Test
class DBConnectionTest {
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
        it("can insert multiple rows with one command") {
            val result =
                createConnectionProvider().withConnection { connection ->
                    connection.createInsertStatement("insert into users(name, email) values ($1, $2)")
                        .executeBatch(
                            listOf(String::class.java, String::class.java),
                            sequenceOf(sequenceOf("belle", "belle@bs.com"), sequenceOf("sebastian", "seb@bs.com"))
                        )
                        .map { it.getId() }.toList()
                }
            expectThat(result).isEqualTo(listOf(1, 2))
        }
        it("can insert multiple rows with one command even null") {
            val result =
                createConnectionProvider().withConnection { connection ->
                    connection.createInsertStatement("insert into users(name, email) values ($1, $2)")
                        .executeBatch(
                            listOf(String::class.java, String::class.java),
                            sequenceOf(sequenceOf("belle", null), sequenceOf("sebastian", null))
                        )
                        .map { it.getId() }.toList()
                }
            expectThat(result).isEqualTo(listOf(1, 2))
        }

    }
}
