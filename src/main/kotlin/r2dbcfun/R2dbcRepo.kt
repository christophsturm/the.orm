package r2dbcfun

import io.r2dbc.spi.Connection
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.full.memberFunctions

class R2dbcRepo<T : Any>(val connection: Connection, val kClass: KClass<out T>) {
    companion object {
        inline fun <reified T : Any> create(connection: Connection) = R2dbcRepo(connection, T::class)
    }

    val properties = kClass.declaredMemberProperties
    val copyConstructor = kClass.memberFunctions.single { it.name == "copy" }
    suspend fun create(instance: T): T {
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
            connection.createStatement(insertStatement),
            { idx, statement, field -> statement.bind(idx, field) })
        val id = statement.executeInsert()

        return copyConstructor.callBy(mapOf(copyConstructor.parameters.single { it.name == "id" } to id) + mapOf(
            copyConstructor.instanceParameter!! to instance
        )) as T


    }

}
