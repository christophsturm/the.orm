package r2dbcfun

import io.r2dbc.spi.Connection
import io.r2dbc.spi.Result
import io.r2dbc.spi.Statement
import kotlinx.coroutines.reactive.awaitSingle
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.full.memberFunctions

suspend fun Result.singleInt(): Int {
    return this.map { row, _ ->
        row.get(0, Integer::class.java)!!.toInt()
    }.awaitSingle()
}

suspend fun Statement.executeInsert() = this.returnGeneratedValues().execute().awaitSingle().singleInt()
suspend fun <T : Any> Connection.create(instance: T, kClass: KClass<out T>): T {
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
