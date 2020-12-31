package r2dbcfun.internal

import io.r2dbc.spi.R2dbcDataIntegrityViolationException
import r2dbcfun.DataIntegrityViolationException
import r2dbcfun.UniqueConstraintViolatedException
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

internal class ExceptionInspector<T : Any>(private val tableName: String, kClass: KClass<T>) {
    private val fieldNamesProperties = kClass.memberProperties.associateBy { it.name.toLowerCase() }
    fun r2dbcDataIntegrityViolationException(e: R2dbcDataIntegrityViolationException): DataIntegrityViolationException {
        return UniqueConstraintViolatedException(e.message!!, e.cause, computeAffectedField(e))

    }

    private fun computeAffectedField(e: R2dbcDataIntegrityViolationException): KProperty1<T, *>? {
        val message = e.message!!

        val fieldString = when {

            // h2: Unique index or primary key violation: "PUBLIC.CONSTRAINT_INDEX_4 ON PUBLIC.USERS(EMAIL) VALUES 1"; SQL statement:
            message.contains("Unique index or primary key violation") -> message.substringAfter("PUBLIC.${tableName.toUpperCase()}(")
                .substringBefore(")")
            // psql: duplicate key value violates unique constraint "users_email_key"
            message.startsWith("duplicate key value violates unique constraint") -> message.substringAfter("constraint \"${tableName}_")
                .substringBefore("_key\"")
            else -> null
        }
        return fieldNamesProperties[fieldString?.toLowerCase()]
    }
}
