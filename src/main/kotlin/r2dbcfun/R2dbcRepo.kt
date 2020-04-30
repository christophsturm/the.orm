package r2dbcfun

import io.r2dbc.spi.Connection
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.full.memberFunctions

class R2dbcRepo<T : Any>(private val connection: Connection, private val kClass: KClass<out T>) {
    companion object {
        inline fun <reified T : Any> create(connection: Connection) = R2dbcRepo(connection, T::class)
    }

    private val properties = kClass.declaredMemberProperties

    @Suppress("UNCHECKED_CAST")
    private val copyConstructor: KFunction<T> = kClass.memberFunctions.single { it.name == "copy" } as KFunction<T>
    private val idParameter = copyConstructor.parameters.single { it.name == "id" }
    private val instanceParameter = copyConstructor.instanceParameter!!

    suspend fun create(instance: T): T {
        @Suppress("UNCHECKED_CAST")
        val propertiesWithValues =
            properties.associateBy({ it }, { it.getter.call(instance) })
                .filterValues { it != null } as Map<KProperty1<out T, *>, Any>

        val insertStatementString =
            propertiesWithValues.keys.joinToString(
                prefix = "INSERT INTO ${kClass.simpleName}s(",
                postfix = ") values ("
            ) { it.name } +
                    propertiesWithValues.keys.mapIndexed { idx, _ -> "$${idx + 1}" }.joinToString(postfix = ")")

        val statement = propertiesWithValues.values.foldIndexed(
            connection.createStatement(insertStatementString),
            { idx, statement, field -> statement.bind(idx, field) })

        val id = statement.executeInsert()

        return copyConstructor.callBy(mapOf(idParameter to id, instanceParameter to instance))
    }

}
