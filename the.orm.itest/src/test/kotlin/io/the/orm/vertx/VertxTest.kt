package io.the.orm.vertx

import failgood.Ignored
import failgood.Test
import failgood.describe
import io.the.orm.test.DBS
import io.the.orm.test.PostgresDb
import io.the.orm.test.TestUtilConfig
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.pgclient.PgBuilder
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.PoolOptions
import io.vertx.sqlclient.PreparedQuery
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.SqlConnectOptions
import io.vertx.sqlclient.Tuple
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo

/*
this test is for experimenting with the vertx psql client. it uses the vertx api directly to try out and show
how things work.
 */
suspend fun VertxClientFixture(): VertxClientFixture {
    val db: PostgresDb = DBS.psql16.preparePostgresDB()
    val client: Pool = VertxClientFixture.createClient(db)
    return VertxClientFixture(db, client)
}

@Suppress("SqlNoDataSourceInspection")
class VertxClientFixture(val db: PostgresDb, val client: Pool) : AutoCloseable {

    companion object {

        suspend fun createClient(db: PostgresDb): Pool {
            val connectOptions = SqlConnectOptions()
                .setPort(db.port)
                .setHost(db.host)
                .setDatabase(db.databaseName)
                .setUser("test")
                .setPassword("test")
            return PgBuilder.pool().with(PoolOptions().setMaxSize(5)).connectingTo(connectOptions).build()!!.also {
                it.query(
                    """create sequence users_id_seq no maxvalue;
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
                ).execute().coAwait()
            }
        }
    }

    override fun close() {
        db.close()
        client.close()
    }

    suspend fun query(sql: String) = client.query(sql).execute().coAwait()
    suspend fun preparedQuery(query: String, tuple: Tuple) = preparedQuery(query).execute(tuple).coAwait()

    fun preparedQuery(query: String): PreparedQuery<RowSet<Row>> = client.preparedQuery(query)
}

@Test
class VertxTest {
    val context = describe(
        "vertx sql client api",
        ignored = if (TestUtilConfig.H2_ONLY) Ignored.Because("Running in h2 only mode") else null,
        given = { VertxClientFixture() }
    ) {
        it("can run sql queries") {
            val result: RowSet<Row> = given.query("SELECT * FROM users WHERE id=1")
            expectThat(result.size()).isEqualTo(0)
        }
        it("can run prepared queries") {
            val result: RowSet<Row> = given.preparedQuery("SELECT * FROM users WHERE id=$1", Tuple.of(1))
            expectThat(result.size()).isEqualTo(0)
        }
        it("can insert with autoincrement") {
            val result: RowSet<Row> =
                given.preparedQuery("insert into users(name) values ($1) returning id", Tuple.of("belle"))
            expectThat(result.size()).isEqualTo(1)
            expectThat(result.columnsNames()).containsExactly("id")
            expectThat(result.single().get(Integer::class.java, "id").toInt()).isEqualTo(1)
        }
        describe("querying by lists", given = {
            val fixture = given()
            // insert 3 rows
            val query = fixture.preparedQuery("insert into users(name) values ($1) returning id")
            val ids = listOf("ton", "steine", "scherben").map {
                query.execute(Tuple.of(it)).coAwait().single().get(Integer::class.java, "id").toInt()
            }
            assert(ids == listOf(1, 2, 3))
            fixture
        }) {
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
