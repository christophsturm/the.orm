package io.the.orm.test.functional.exp

import failgood.Test
import failgood.assert.containsExactlyInAnyOrder
import io.the.orm.dbio.DBResult
import io.the.orm.mapper.SimpleResultMapper
import io.the.orm.test.DBS
import io.the.orm.test.describeOnAllDbs
import io.the.orm.test.functional.USERS_SCHEMA
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList

@Test
object SchemaLoadingTest {
    val context =
        describeOnAllDbs(
            "detecting the table structure",
            DBS.databases,
            USERS_SCHEMA,
            ignored = UnlessEnv("NEXT")
        ) { createConnectionProvider ->
            it("can get the table structure") {
                val connectionProvider = createConnectionProvider()
                connectionProvider.withConnection {
                    val columns = it.createStatement(
                        "select column_name, data_type, character_maximum_length, column_default, is_nullable\n" +
                            "from INFORMATION_SCHEMA.COLUMNS where lower(table_name) = 'users'"
                    ).execute().mapToList<Columns>().toList()
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
                    assert(
                        columns.map { it.columnName.lowercase() }.containsExactlyInAnyOrder(
                            "id",
                            "email",
                            "is_cool",
                            "bio",
                            "favorite_color",
                            "birthday",
                            "weight",
                            "balance"
                        )
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
