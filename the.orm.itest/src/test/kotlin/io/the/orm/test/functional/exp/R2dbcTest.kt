@file:Suppress("SqlResolve", "SqlNoDataSourceInspection", "PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package io.the.orm.test.functional.exp

import failgood.Test
import failgood.describe
import io.r2dbc.spi.ConnectionFactories
import io.the.orm.dbio.TransactionalConnectionProvider
import io.the.orm.dbio.r2dbc.R2dbcConnection
import io.the.orm.test.DBS
import io.the.orm.test.DBTestUtil
import io.the.orm.test.TestUtilConfig
import io.the.orm.test.describeOnAllDbs
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toCollection
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo
import java.lang.Long

/**
 * this is just the R2DBC playground that started this project.
 *
 * @see io.the.orm.test.functional.RepoFunctionalTest for api usage.
 */

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
class R2dbcTest {
    val context = describeOnAllDbs(
        "the r2dbc api",
        DBS.databases.filterNot { it is DBTestUtil.VertxPSQLTestDatabase }, SCHEMA) { connection ->
        it("can batch insert values and select result") {
            val dbConnectionFactory = (connection as TransactionalConnectionProvider).dbConnectionFactory
            val conn = (dbConnectionFactory.getConnection() as R2dbcConnection).connection
            autoClose(conn) { it.close().awaitFirstOrNull() }
            val (firstId, secondId) = conn.createStatement("insert into users(name) values($1)")
                .bind("$1", "belle")
                .add()
                .bind("$1", "sebastian")
                .returnGeneratedValues().execute().asFlow().map {
                    it.map { row, _ -> row.get(0, Long::class.java)!!.toLong() }.awaitSingle()
                }.toList()
            val selectResult =
                conn.createStatement("select * from users").execute().awaitSingle()
            val namesFlow =
                selectResult.map { row, _ -> row.get("NAME", String::class.java) as String }.asFlow()
            val names = namesFlow.toCollection(mutableListOf())
            expectThat(firstId).isEqualTo(1)
            expectThat(secondId).isEqualTo(2)
            expectThat(names).containsExactly("belle", "sebastian")
        }
    } + if (!TestUtilConfig.H2_ONLY)
        listOf(describe("r2dbc pool") {
            test("can open and close pool") {
                val (databaseName, host, port) = DBS.psql14.preparePostgresDB()
                val factory =
                    ConnectionFactories.get("r2dbc:pool:postgresql://test:test@$host:$port/$databaseName?initialSize=1")
                val connection1 = factory.create().awaitSingle()
                val connection2 = factory.create().awaitSingle()
                connection1.createStatement("select * from users").execute().awaitSingle()
                connection2.createStatement("select * from users").execute().awaitSingle()
                connection1.close().awaitFirstOrNull()
                connection2.close().awaitFirstOrNull()
            }
        })
    else listOf()
}
