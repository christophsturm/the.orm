package r2dbcfun

import io.r2dbc.spi.Connection
import kotlinx.coroutines.reactive.awaitSingle
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty1
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.full.memberFunctions

class R2dbcRepo<T : Any>(private val connection: Connection, kClass: KClass<out T>) {
    companion object {
        inline fun <reified T : Any> create(connection: Connection) = R2dbcRepo(connection, T::class)
    }

    private val properties = kClass.declaredMemberProperties

    @Suppress("UNCHECKED_CAST")
    private val copyConstructor: KFunction<T> = kClass.memberFunctions.single { it.name == "copy" } as KFunction<T>
    private val idParameter = copyConstructor.parameters.single { it.name == "id" }
    private val instanceParameter = copyConstructor.instanceParameter!!
    private val constructor = kClass.constructors.singleOrNull { it.visibility == KVisibility.PUBLIC }
        ?: throw RuntimeException("No public constructor found for ${kClass.simpleName}")

    private val constructorParameters = constructor.parameters
    private val tableName = "${kClass.simpleName!!.toLowerCase()}s"

    @Suppress("SqlResolve")
    private val selectByIdString =
        "select ${constructorParameters.joinToString { it.name!!.toSnakeCase() }} from $tableName where id=\$1"

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
            throw RuntimeException("error executing insert: $insertStatementString", e)
        }

        return copyConstructor.callBy(mapOf(idParameter to id, instanceParameter to instance))
    }


    suspend fun findById(id: Long): T {
        val result =
            try {
                connection.createStatement(selectByIdString).bind("$1", id).execute().awaitSingle()
            } catch (e: Exception) {
                throw RuntimeException("error executing insert: $selectByIdString", e)
            }
        val parameterMap = try {
            result.map { row, _ -> constructorParameters.map { it to row.get(it.name!!.toSnakeCase()) }.toMap() }
                .awaitSingle()
        } catch (e: NoSuchElementException) {
            throw NotFoundException("No $tableName found for id $id")
        }
        return constructor.callBy(parameterMap)
    }

}
