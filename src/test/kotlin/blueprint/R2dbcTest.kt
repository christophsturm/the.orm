package blueprint

import dev.minutest.junit.JUnit5Minutests
import dev.minutest.rootContext
import io.r2dbc.spi.Connection
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.Result
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.test.runBlockingTest
import org.flywaydb.core.Flyway
import org.h2.jdbcx.JdbcDataSource
import strikt.api.expectThat
import strikt.assertions.isEqualTo


@Suppress("SqlNoDataSourceInspection", "SqlResolve")
@ExperimentalCoroutinesApi
class R2dbcTest : JUnit5Minutests {

    fun tests() = rootContext<Unit> {
        test("can select 42") {
            val dataSource = JdbcDataSource()
            dataSource.setURL("jdbc:h2:mem:r2dbc-test;DB_CLOSE_DELAY=-1")
            val flyway = Flyway.configure().dataSource(dataSource).load()
            flyway.migrate()
            val connectionFactory =
                ConnectionFactories.get("r2dbc:h2:mem:///r2dbc-test;DB_CLOSE_DELAY=-1")
            runBlockingTest {
                val connection: Connection = connectionFactory.create().awaitSingle()
                val firstInsertResult =
                    connection.createStatement("insert into USERS values(NULL, $1)").bind("$1", "user42")
                        .returnGeneratedValues().execute().awaitSingle()
                val secondInsertResult =
                    connection.createStatement("insert into USERS values(NULL, $1)").bind("$1", "user42")
                        .returnGeneratedValues().execute().awaitSingle()

                expectThat(getIntResult(firstInsertResult).toInt()).isEqualTo(1)
                expectThat(getIntResult(secondInsertResult).toInt()).isEqualTo(2)
            }
        }
    }

    private suspend fun getIntResult(result: Result): Integer {
        return result.map { a, _ ->
            a.get(0, Integer::class.java)
        }.awaitSingle()
    }
}
