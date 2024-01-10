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
object DataBaseMetadataTest {
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
                        // language=PostgreSQL
                        println(
                            conn.executeToList(
                                """SELECT *
FROM (SELECT n.nspname,
             c.relname,
             a.attname,
             a.atttypid,
             a.attnotnull OR (t.typtype = 'd' AND t.typnotnull)            AS attnotnull,
             a.atttypmod,
             a.attlen,
             t.typtypmod,
             row_number() OVER (PARTITION BY a.attrelid ORDER BY a.attnum) AS attnum,
             nullif(a.attidentity, '')                                     as attidentity,
             nullif(a.attgenerated, '')                                    as attgenerated,
             pg_catalog.pg_get_expr(def.adbin, def.adrelid)                AS adsrc,
             dsc.description,
             t.typbasetype,
             t.typtype
      FROM pg_catalog.pg_namespace n
               JOIN pg_catalog.pg_class c ON (c.relnamespace = n.oid)
               JOIN pg_catalog.pg_attribute a ON (a.attrelid = c.oid)
               JOIN pg_catalog.pg_type t ON (a.atttypid = t.oid)
               LEFT JOIN pg_catalog.pg_attrdef def ON (a.attrelid = def.adrelid AND a.attnum = def.adnum)
               LEFT JOIN pg_catalog.pg_description dsc ON (c.oid = dsc.objoid AND a.attnum = dsc.objsubid)
               LEFT JOIN pg_catalog.pg_class dc ON (dc.oid = dsc.classoid AND dc.relname = 'pg_class')
               LEFT JOIN pg_catalog.pg_namespace dn ON (dc.relnamespace = dn.oid AND dn.nspname = 'pg_catalog')
      WHERE c.relkind in ('r', 'p', 'v', 'f', 'm')
        and a.attnum > 0
        AND NOT a.attisdropped
        AND c.relname LIKE 'users') c
WHERE true
ORDER BY nspname, c.relname, attnum """
                            )
                        )
                    }
                }

                it("can get tables") {
                    given.transactionProvider.withConnection { conn ->
                        val tables = getTables(conn)
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
                        assertEquals("users", tables.single().tableName)
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

    private suspend fun getTables(conn: DBConnection) =
        conn
            .createStatement(
                """SELECT NULL              AS TABLE_CAT,
           n.nspname         AS TABLE_SCHEM,
           c.relname         AS TABLE_NAME,
           CASE n.nspname ~ '^pg_' OR n.nspname = 'information_schema'
               WHEN true THEN CASE
                                  WHEN n.nspname = 'pg_catalog' OR n.nspname = 'information_schema' THEN CASE c.relkind
                                                                                                             WHEN 'r'
                                                                                                                 THEN 'SYSTEM TABLE'
                                                                                                             WHEN 'v'
                                                                                                                 THEN 'SYSTEM VIEW'
                                                                                                             WHEN 'i'
                                                                                                                 THEN 'SYSTEM INDEX'
                                                                                                             ELSE NULL END
                                  WHEN n.nspname = 'pg_toast' THEN CASE c.relkind
                                                                       WHEN 'r' THEN 'SYSTEM TOAST TABLE'
                                                                       WHEN 'i' THEN 'SYSTEM TOAST INDEX'
                                                                       ELSE NULL END
                                  ELSE CASE c.relkind
                                           WHEN 'r' THEN 'TEMPORARY TABLE'
                                           WHEN 'p' THEN 'TEMPORARY TABLE'
                                           WHEN 'i' THEN 'TEMPORARY INDEX'
                                           WHEN 'S' THEN 'TEMPORARY SEQUENCE'
                                           WHEN 'v' THEN 'TEMPORARY VIEW'
                                           ELSE NULL END END
               WHEN false THEN CASE c.relkind
                                   WHEN 'r' THEN 'TABLE'
                                   WHEN 'p' THEN 'PARTITIONED TABLE'
                                   WHEN 'i' THEN 'INDEX'
                                   WHEN 'P' then 'PARTITIONED INDEX'
                                   WHEN 'S' THEN 'SEQUENCE'
                                   WHEN 'v' THEN 'VIEW'
                                   WHEN 'c' THEN 'TYPE'
                                   WHEN 'f' THEN 'FOREIGN TABLE'
                                   WHEN 'm' THEN 'MATERIALIZED VIEW'
                                   ELSE NULL END
               ELSE NULL END AS TABLE_TYPE,
           d.description     AS REMARKS,
           ''                as TYPE_CAT,
           ''                as TYPE_SCHEM,
           ''                as TYPE_NAME,
           ''                AS SELF_REFERENCING_COL_NAME,
           ''                AS REF_GENERATION
    FROM pg_catalog.pg_namespace n,
         pg_catalog.pg_class c
             LEFT JOIN pg_catalog.pg_description d
                       ON (c.oid = d.objoid AND d.objsubid = 0 and d.classoid = 'pg_class'::regclass)
    WHERE c.relnamespace = n.oid""" +

                    //  AND n.nspname LIKE 'schemaPattern'
                    //  AND c.relname LIKE 'tableNamePattern'
                    """  AND (false OR (c.relkind = 'r' AND n.nspname !~ '^pg_' AND n.nspname <> 'information_schema'))
    ORDER BY TABLE_TYPE, TABLE_SCHEM, TABLE_NAME """
            )
            .execute()
            .mapToList<Table>()
            .toList()
}

private suspend fun DBConnection.executeToList(sql: String) =
    createStatement(sql).execute().asMapFlow().toList().joinToString("\n")

private suspend inline fun <reified T : Any> DBResult.mapToList(): Flow<T> {
    val mapper = SimpleResultMapper.forClass(T::class)
    return mapper.mapQueryResult(this)
}

data class Table(
    val refGeneration: String,
    val typeName: String,
    val typeSchem: String,
    val typeCat: String,
    val tableCat: String?,
    val tableName: String,
    val selfReferencingColName: String,
    val remarks: String?,
    val tableType: String
)

data class Columns(
    val columnName: String,
    val dataType: String,
    val characterMaximumLength: Long?,
    val isNullable: String
)
