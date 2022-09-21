package io.the.orm.test.vertx

import failgood.Test
import failgood.describe
import io.the.orm.test.DBS
import io.the.orm.test.TestUtilConfig
import io.vertx.kotlin.coroutines.await
import io.vertx.pgclient.PgConnectOptions
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.PoolOptions
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.SqlClient
import io.vertx.sqlclient.Tuple
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo

@Suppress("SqlNoDataSourceInspection", "SqlResolve")
@Test
class VertxTest {
    val context = describe("vertx sql client api", disabled = TestUtilConfig.H2_ONLY) {
        val db by dependency({ DBS.psql14.preparePostgresDB() }) { it.close() }

        val client: SqlClient by dependency({
            PgPool.pool(
                PgConnectOptions()
                    .setPort(db.port)
                    .setHost(db.host)
                    .setDatabase(db.databaseName)
                    .setUser("test")
                    .setPassword("test"), PoolOptions().setMaxSize(5)
            ).also { it.query("""create sequence users_id_seq no maxvalue;
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
""").execute().await() }
        }) { it.close() }
        it("can run sql queries") {
            val result: RowSet<Row> = client.query("SELECT * FROM users WHERE id=1").execute().await()
            expectThat(result.size()).isEqualTo(0)
        }
        it("can run prepared queries") {
            val result: RowSet<Row> =
                client.preparedQuery("SELECT * FROM users WHERE id=$1").execute(Tuple.of(1)).await()
            expectThat(result.size()).isEqualTo(0)
        }
        it("can insert with autoincrement") {
            val result: RowSet<Row> =
                client.preparedQuery("insert into users(name) values ($1) returning id").execute(Tuple.of("belle"))
                    .await()
            expectThat(result.size()).isEqualTo(1)
            expectThat(result.columnsNames()).containsExactly("id")
            expectThat(result.single().get(Integer::class.java, "id").toInt()).isEqualTo(1)
        }
    }
}
