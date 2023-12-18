package io.the.orm.vertx

import failgood.Ignored
import failgood.Test
import failgood.describe
import io.the.orm.test.TestUtilConfig
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.Tuple
import kotlinx.coroutines.runBlocking
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo

const val SCHEMA = """create sequence users_id_seq no maxvalue;
                                create table users
                (
                    id             bigint       not null default nextval('users_id_seq') primary key,
                    name           varchar(100) not null,
                    email          varchar(100) unique,
                    is_cool        boolean,
                    bio            text,
                    favorite_color varchar(10),
                    birthday       date,
                    weight         decimal(5, 2),
                    balance        decimal(5, 2)
                );
                """

/*
this test is for experimenting with the vertx psql client. it uses the vertx api directly to try out and show
how things work.
 */
@Test
class VertxTest {
    // this test tries to share givens by setting isolation to false and creating the shared givens outside the context
    // this is just an experiment and will be replaced by something better
    val context = describe(
        "vertx sql client api",
        ignored = if (TestUtilConfig.H2_ONLY) Ignored.Because("Running in h2 only mode") else null, isolation = false,
        given = { VertxClientFixture(SCHEMA) }
    ) {
        val empty = runBlocking { VertxClientFixture(SCHEMA) }
        describe("read only tests", given = { empty }) {
            it("can run sql queries") {
                val result: RowSet<Row> = given.query("SELECT * FROM users WHERE id=1")
                expectThat(result.size()).isEqualTo(0)
            }
            it("can run prepared queries") {
                val result: RowSet<Row> = given.preparedQuery("SELECT * FROM users WHERE id=$1", Tuple.of(1))
                expectThat(result.size()).isEqualTo(0)
            }
        }
        it("can insert with autoincrement") {
            val result: RowSet<Row> =
                given.preparedQuery("insert into users(name) values ($1) returning id", Tuple.of("belle"))
            expectThat(result.size()).isEqualTo(1)
            expectThat(result.columnsNames()).containsExactly("id")
            expectThat(result.single().get(Integer::class.java, "id").toInt()).isEqualTo(1)
        }
        val with3Users =
            VertxClientFixture(SCHEMA).also {
                // insert 3 rows
                val query = it.preparedQuery("insert into users(name) values ($1) returning id")
                val ids = listOf("ton", "steine", "scherben").map {
                    query.execute(Tuple.of(it)).coAwait().single().get(Integer::class.java, "id").toInt()
                }
                assert(ids == listOf(1, 2, 3))
            }
        describe("querying by lists", given = { with3Users }) {
            it("works with one parameter per item") {
                assert(
                    given.preparedQuery("SELECT * FROM users WHERE id in ($1, $2)", Tuple.from(listOf(1, 2)))
                        .size() == 2
                )
            }
            it("works with ANY") {
                assert(
                    given.preparedQuery("SELECT * FROM users WHERE id = ANY($1)", Tuple.of(arrayOf(1, 2))).size() == 2
                )
            }
            it("works with unnest") {
                assert(
                    given.preparedQuery(
                        "SELECT * FROM users WHERE id in (select unnest(($1)::bigint[]))",
                        Tuple.of(arrayOf(1, 2))
                    ).size() == 2
                )
            }
        }
    }
}
