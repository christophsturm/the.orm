package blueprint

import dev.minutest.junit.JUnit5Minutests
import dev.minutest.rootContext
import io.r2dbc.spi.Connection
import io.r2dbc.spi.ConnectionFactories
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.test.runBlockingTest
import org.flywaydb.core.Flyway
import org.h2.jdbcx.JdbcDataSource
import strikt.api.expectThat
import strikt.assertions.isEqualTo


@Suppress("SqlNoDataSourceInspection")
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
                val result = connection.createStatement("select 42").execute().awaitSingle()
                val field = result.map { a, _ ->
                    a.get(0, Integer::class.java)
                }.awaitSingle()
                expectThat(field.toInt()).isEqualTo(42)
            }
        }
    }
}
