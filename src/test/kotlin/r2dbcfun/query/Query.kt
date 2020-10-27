package r2dbcfun.query

import io.r2dbc.spi.Connection
import r2dbcfun.ClassInfo
import r2dbcfun.R2dbcRepo
import r2dbcfun.toSnakeCase
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties

// internal api
open class Query<T : Any>(kClass: KClass<T>, vararg val conditions: Condition<*>) {
    private val classInfo = ClassInfo(kClass)
    internal val snakeCaseForProperty =
        kClass.declaredMemberProperties.associateBy({ it }, { it.name.toSnakeCase() })

    @Suppress("SqlResolve")
    private val tableName = "${kClass.simpleName!!.toSnakeCase().toLowerCase()}s"

    internal val selectString = run {

        val queryString =
            conditions.joinToString(separator = " and ") { "${snakeCaseForProperty[it.prop]} ${it.conditionString}" }
        "select ${classInfo.fieldInfo.joinToString { it.snakeCaseName }} from $tableName where " + queryString
    }
    private val finder = R2dbcRepo(kClass).finder


    suspend fun find(connection: Connection, vararg parameter: Any): Any =
        finder.findBy(parameter.asList(), connection, selectString)

}
