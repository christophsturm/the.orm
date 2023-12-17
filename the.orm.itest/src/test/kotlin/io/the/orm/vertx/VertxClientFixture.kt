package io.the.orm.vertx

import io.the.orm.test.DBS
import io.the.orm.test.PostgresDb
import io.the.orm.test.vertxPool
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.PreparedQuery
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.Tuple

/**
 * a fixture for testing with vertx.
 * it loads a schema, and it can run queries and prepared queries
 */
suspend fun VertxClientFixture(schema: String): VertxClientFixture {
    val db: PostgresDb = DBS.psql16.preparePostgresDB()
    val client = db.vertxPool()
    client.query(schema).execute().coAwait()
    return VertxClientFixture(db, client)
}

class VertxClientFixture(private val db: PostgresDb, private val client: Pool) : AutoCloseable {
    override fun close() {
        db.close()
        client.close()
    }

    suspend fun query(sql: String) = client.query(sql).execute().coAwait()
    suspend fun preparedQuery(query: String, tuple: Tuple) = preparedQuery(query).execute(tuple).coAwait()

    fun preparedQuery(query: String): PreparedQuery<RowSet<Row>> = client.preparedQuery(query)
}
