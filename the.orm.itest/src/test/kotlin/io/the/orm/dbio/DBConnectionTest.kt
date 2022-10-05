package io.the.orm.dbio

import failgood.Test
import io.the.orm.test.DBS
import io.the.orm.test.describeOnAllDbs
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import strikt.api.expectThat
import strikt.assertions.isEqualTo

private const val SCHEMA = """
    create sequence users_id_seq no maxvalue;
create table users
(
    id             bigint       not null default nextval('users_id_seq') primary key,
    name           varchar(100) not null,
    email          varchar(100) unique
);

"""

@Test
class DBConnectionTest {
    val context = describeOnAllDbs<DBConnection>(DBS.databases, SCHEMA) { createConnectionProvider ->
        describe("inserting with autoincrement") {
            it("works when all fields are non-null") {
                val result =
                    createConnectionProvider().withConnection { connection ->
                        connection.createInsertStatement("insert into users(name) values ($1)")
                            .execute(listOf(String::class.java), sequenceOf("belle")).getId()
                    }
                expectThat(result).isEqualTo(1)
            }
            it("even works when some fields are null") {
                val result =
                    createConnectionProvider().withConnection { connection ->
                        connection.createInsertStatement("insert into users(name, email) values ($1, $2)")
                            .execute(listOf(String::class.java, String::class.java), sequenceOf("belle", null))
                            .getId()
                    }
                expectThat(result).isEqualTo(1)
            }
        }
        describe("inserting multiple rows in a batch") {
            it("works when all types are not null") {
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
            it("even works for null values") {
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
}
