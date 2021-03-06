package r2dbcfun.internal

import io.r2dbc.spi.R2dbcDataIntegrityViolationException
import io.vertx.pgclient.PgException
import r2dbcfun.DataIntegrityViolationException
import r2dbcfun.UniqueConstraintViolatedException
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

internal class ExceptionInspector<T : Any>(private val table: Table<T>, kClass: KClass<T>) {
    private val fieldNamesProperties = kClass.memberProperties.associateBy { it.name.toLowerCase() }
    fun r2dbcDataIntegrityViolationException(
        e: R2dbcDataIntegrityViolationException,
        instance: T
    ): DataIntegrityViolationException {
        val field = computeAffectedField(e.message!!)
        val value = field?.invoke(instance)
        return UniqueConstraintViolatedException(e.message!!, e.cause, field, value)

    }

    private fun computeAffectedField(message: String): KProperty1<T, *>? {

        val lowerCasedMessage = message.toLowerCase()
        val fieldString = when {

            // h2: Unique index or primary key violation: "PUBLIC.CONSTRAINT_INDEX_4 ON PUBLIC.USERS(EMAIL) VALUES 1"; SQL statement:
            lowerCasedMessage.contains("unique index or primary key violation") -> lowerCasedMessage.substringAfter("public.${table.name}(")
                .substringBefore(")")
            // psql: duplicate key value violates unique constraint "users_email_key"
            lowerCasedMessage.startsWith("duplicate key value violates unique constraint") -> lowerCasedMessage.substringAfter(
                "constraint \"${table}_"
            )
                .substringBefore("_key\"")
            else -> null
        }
        return fieldNamesProperties[fieldString?.toLowerCase()]
    }

    fun pgException(e: PgException, instance: T): DataIntegrityViolationException {
        val field = computeAffectedField(e.errorMessage)
        val value = field?.invoke(instance)
        return UniqueConstraintViolatedException(e.errorMessage, e.cause, field, value)

    }
}
