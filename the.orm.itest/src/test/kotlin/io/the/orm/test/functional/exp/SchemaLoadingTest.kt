package io.the.orm.test.functional.exp

import failgood.Test
import io.the.orm.dbio.DBConnection
import io.the.orm.dbio.DBResult
import io.the.orm.mapper.SimpleResultMapper
import io.the.orm.test.DBS
import io.the.orm.test.Dialect
import io.the.orm.test.describeAll
import io.the.orm.test.fixture
import io.the.orm.test.functional.USERS_SCHEMA
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList

@Test
object SchemaLoadingTest {
    val context =
        DBS.databases
            .filter { it.dialect == Dialect.POSTGRESQL_VERTX }
            .describeAll("detecting the table structure", given = { it.fixture(USERS_SCHEMA) }) {
                it("prints result") {
                    given.transactionProvider.withConnection { conn ->
                        /*
                        println(
                            conn.executeToList(
                                "select column_name, data_type, table_catalog, character_maximum_length, column_default, is_nullable" +
                                    " from INFORMATION_SCHEMA.COLUMNS where lower(table_name) = 'users'"
                            )
                        )
                        println(conn.executeToList("select database()"))*/
                        println(
                            conn.executeToList(
                                // query straight from <strike>hell</strike>chat-gpt
                                """SELECT
    cols.table_name,
    cols.column_name,
    cols.data_type,
    cols.character_maximum_length,
    cols.is_nullable,
    cols.column_default,
    cols.ordinal_position,
    indexes.index_name,
    indexes.index_type,
    indexes.index_columns
FROM
    information_schema.columns AS cols
LEFT JOIN (
    SELECT
        idx.indrelid::regclass AS table_name,
        idx.indexrelid::regclass AS index_name,
        idx.indisunique,
        idx.indisprimary,
        idx.indisclustered,
        idx.indisvalid,
        idx.indnkeyatts,
        idx.indkey,
        idx.indexrelid,
        idx.indrelid,
        string_agg(attname, ', ') AS index_columns,
        CASE
            WHEN idx.indisunique THEN 'UNIQUE'
            WHEN idx.indisprimary THEN 'PRIMARY KEY'
            WHEN idx.indisclustered THEN 'CLUSTERED'
            ELSE 'INDEX'
        END AS index_type
    FROM
        pg_index AS idx
    JOIN
        pg_attribute AS att ON att.attnum = ANY(idx.indkey) AND att.attrelid = idx.indrelid
    GROUP BY
        1, 2, 3, 4, 5, 6, 7, 8, 9
) AS indexes ON cols.table_name::text = indexes.table_name::text AND cols.column_name = indexes.index_columns
ORDER BY
    cols.table_name, cols.ordinal_position;
"""
                            )
                        )
                    }
                }
                it("can get the table structure") {
                    given.transactionProvider.withConnection { conn ->
                        val columns =
                            conn
                                .createStatement(
                                    "select column_name, data_type, character_maximum_length, column_default, is_nullable\n" +
                                        "from INFORMATION_SCHEMA.COLUMNS where lower(table_name) = 'users'"
                                )
                                .execute()
                                .mapToList<Columns>()
                                .toList()
                        /*
                                            id             bigint       not null default nextval('users_id_seq') primary key,
                        name           varchar(100) not null,
                        email          varchar(100) unique,
                        is_cool        boolean,
                        bio            text,
                        favorite_color varchar(10),
                        birthday       date,
                        weight         decimal(5, 2),
                        balance        decimal(5, 2)

                                     */
                        assertEquals(
                            listOf(
                                    "id",
                                    "name",
                                    "email",
                                    "is_cool",
                                    "bio",
                                    "favorite_color",
                                    "birthday",
                                    "weight",
                                    "balance"
                                )
                                .sorted(),
                            columns.map { it.columnName.lowercase() }.sorted()
                        )
                    }
                }
            }
}

private suspend fun DBConnection.executeToList(sql: String) =
    createStatement(sql).execute().asMapFlow().toList()

private suspend inline fun <reified T : Any> DBResult.mapToList(): Flow<T> {
    val mapper = SimpleResultMapper.forClass(T::class)
    return mapper.mapQueryResult(this)
}

data class Columns(
    val columnName: String,
    val dataType: String,
    val characterMaximumLength: Long?,
    val isNullable: String
)
