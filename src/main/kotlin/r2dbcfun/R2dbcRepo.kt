package r2dbcfun

import io.r2dbc.spi.Connection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitSingle
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty1
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.memberProperties

class R2dbcRepo<T : Any>(private val connection: Connection, kClass: KClass<out T>) {
    companion object {
        inline fun <reified T : Any> create(connection: Connection) = R2dbcRepo(connection, T::class)
    }

    private val tableName = "${kClass.simpleName!!.toLowerCase()}s"


    private val properties = kClass.declaredMemberProperties

    // to call the copy function
    @Suppress("UNCHECKED_CAST")
    private val copyFunction: KFunction<T> = kClass.memberFunctions.single { it.name == "copy" } as KFunction<T>
    private val idParameter = copyFunction.parameters.single { it.name == "id" }
    private val instanceParameter = copyFunction.instanceParameter!!

    init {
        val kclass = idParameter.type.classifier as KClass<*>

        if (kclass != Long::class)
            throw R2dbcRepoException("Id Column type was ${kclass}, but must be ${Long::class}")
    }

    suspend fun create(instance: T): T {
        @Suppress("UNCHECKED_CAST")
        val propertiesWithValues =
            properties.associateBy({ it }, { it.getter.call(instance) })
                .filterValues { it != null } as Map<KProperty1<out T, *>, Any>

        val fieldNames = propertiesWithValues.keys.joinToString { it.name.toSnakeCase() }
        val fieldPlaceHolders = (1..propertiesWithValues.keys.size).joinToString { idx -> "$$idx" }
        val insertStatementString = "INSERT INTO $tableName($fieldNames) values ($fieldPlaceHolders)"

        val statement = propertiesWithValues.values.foldIndexed(
            connection.createStatement(insertStatementString),
            { idx, statement, field -> statement.bind(idx, field) })

        val id = try {
            statement.executeInsert()
        } catch (e: Exception) {
            throw R2dbcRepoException("error executing insert: $insertStatementString", e)
        }

        return copyFunction.callBy(mapOf(idParameter to id, instanceParameter to instance))
    }


    @Suppress("UNCHECKED_CAST")
    private val idProperty = kClass.memberProperties.single { it.name == "id" } as KProperty1<T, Any>
    suspend fun findById(id: Long): T {
        return try {
            findBy(idProperty, id).single()
        } catch (e: NoSuchElementException) {
            throw NotFoundException("No $tableName found for id $id")
        }
    }

    private val constructor = kClass.constructors.singleOrNull { it.visibility == KVisibility.PUBLIC }
        ?: throw RuntimeException("No public constructor found for ${kClass.simpleName}")
    private val constructorParameters = constructor.parameters

    @Suppress("SqlResolve")
    private val selectString =
        "select ${constructorParameters.joinToString { it.name!!.toSnakeCase() }} from $tableName where "

    suspend fun <V> findBy(property: KProperty1<T, V>, value: V): Flow<T> {
        val query = selectString + property.name.toSnakeCase() + "=$1"
        val result =
            try {
                connection.createStatement(query).bind("$1", value).execute().awaitSingle()
            } catch (e: Exception) {
                throw R2dbcRepoException("error executing select: $query", e)
            }
        return result.map { row, _ ->
            constructor.callBy(constructorParameters.map { it to row.get(it.name!!.toSnakeCase()) }.toMap())
        }.asFlow()
    }
}
