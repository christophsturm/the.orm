package io.the.orm.test.functional.exp

import failgood.Ignored
import failgood.Test
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
            .filter { it.dialect != Dialect.H2_R2DBC }
            .describeAll("detecting the table structure", given = { it.fixture(USERS_SCHEMA) }) {
                it("prints result", ignored = Ignored.Because("only for debugging")) {
                    given.transactionProvider.withConnection { conn ->
                        println(
                            conn
                                .createStatement(
                                    "select column_name, data_type, table_catalog, character_maximum_length," +
                                        " column_default, is_nullable " +
                                        "from INFORMATION_SCHEMA.COLUMNS where lower(table_name) = 'users'"
                                )
                                .execute()
                                .asMapFlow()
                                .toList()
                        )
                        println(
                            conn.createStatement("select database()").execute().asMapFlow().toList()
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
