@file:Suppress("SqlResolve")

package r2dbcfun

import dev.minutest.junit.JUnit5Minutests
import dev.minutest.rootContext
import io.r2dbc.spi.Connection
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.Result
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toCollection
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.test.runBlockingTest
import org.flywaydb.core.Flyway
import org.h2.jdbcx.JdbcDataSource
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.full.memberFunctions


data class User(val id: Int? = null, val name: String, val email: String?)

@ExperimentalCoroutinesApi
class R2dbcTest : JUnit5Minutests {

    fun tests() = rootContext<ConnectionFactory> {
        fixture {
            val dataSource = JdbcDataSource()
            dataSource.setURL("jdbc:h2:mem:r2dbc-test;DB_CLOSE_DELAY=-1")
            val flyway = Flyway.configure().dataSource(dataSource).load()
            flyway.migrate()
            ConnectionFactories.get("r2dbc:h2:mem:///r2dbc-test;DB_CLOSE_DELAY=-1")
        }

        test("can insert values and select result") {
            runBlockingTest {
                val connection: Connection = fixture.create().awaitSingle()
                val firstId =
                    connection.createStatement("insert into USERS(name) values($1)").bind("$1", "belle")
                        .executeInsert()
                val secondId =
                    connection.createStatement("insert into USERS(name) values($1)").bind("$1", "sebastian")
                        .executeInsert()

                val selectResult: Result = connection.createStatement("select * from USERS").execute().awaitSingle()
                val namesFlow = selectResult.map { row, _ -> row.get("NAME", String::class.java) }.asFlow()
                val names = namesFlow.toCollection(mutableListOf())
                expectThat(firstId).isEqualTo(1)
                expectThat(secondId).isEqualTo(2)
                expectThat(names).containsExactly("belle", "sebastian")
            }
        }
        test("can insert data class") {
            runBlockingTest {
                val connection: Connection = fixture.create().awaitSingle()
                val instance = User(name = "chris", email = "my email")
                val user = connection.create(User(name = "chris", email = "my email"), instance::class)
                expectThat(user).isEqualTo(User(3, "chris", "my email"))
            }
        }
    }

}

private suspend fun <T : Any> Connection.create(instance: T, kClass: KClass<out T>): T {
    val properties = kClass.declaredMemberProperties
    val propertiesMap = properties.associateBy({ it }, { it: KProperty1<out T, *> -> it.getter.call(instance) })
    val propertiesWithValues =
        propertiesMap
            .filterValues { it != null }

    val insertStatement =
        propertiesWithValues.keys.joinToString(
            prefix = "INSERT INTO ${kClass.simpleName}s(",
            postfix = ") values ("
        ) { it.name } +
                propertiesWithValues.keys.mapIndexed { idx, _ -> "$${idx + 1}" }.joinToString(postfix = ")")

    val statement = propertiesWithValues.values.foldIndexed(
        createStatement(insertStatement),
        { idx, statement, field -> statement.bind(idx, field) })
    val id = statement.executeInsert()
    val copyConstructor = kClass.memberFunctions.single { it.name == "copy" }

    return copyConstructor.callBy(mapOf(copyConstructor.parameters.single { it.name == "id" } to id) + mapOf(
        copyConstructor.instanceParameter!! to instance
    )) as T


}
