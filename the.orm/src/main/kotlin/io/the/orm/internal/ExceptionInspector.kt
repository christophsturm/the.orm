package io.the.orm.internal

import io.r2dbc.spi.R2dbcDataIntegrityViolationException
import io.the.orm.DataIntegrityViolationException
import io.the.orm.UnexpectedDatabaseErrorException
import io.the.orm.UniqueConstraintViolatedException
import io.the.orm.internal.classinfo.Table
import io.vertx.pgclient.PgException
import java.util.Locale
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

internal class ExceptionInspector<T : Any>(private val table: Table, kClass: KClass<T>) {
    private val fieldNamesProperties =
        kClass.memberProperties.associateBy { it.name.lowercase(Locale.getDefault()) }

    @Suppress("UNCHECKED_CAST")
    fun r2dbcDataIntegrityViolationException(
        e: R2dbcDataIntegrityViolationException,
        instance: EntityWrapper
    ): DataIntegrityViolationException =
        r2dbcDataIntegrityViolationException(e, instance.entity as T)

    fun r2dbcDataIntegrityViolationException(
        e: R2dbcDataIntegrityViolationException,
        instance: T
    ): DataIntegrityViolationException {
        val field =
            computeAffectedField(e.message!!)
                ?: throw UnexpectedDatabaseErrorException(e.message!!, e)

        val value = field.invoke(instance)
        return UniqueConstraintViolatedException(e.message!!, e.cause, field, value)
    }

    private fun computeAffectedField(message: String): KProperty1<T, *>? {
        val lowerCasedMessage = message.lowercase(Locale.getDefault())
        val fieldString =
            when {
                // h2: Unique index or primary key violation: "PUBLIC.CONSTRAINT_INDEX_4 ON
                // PUBLIC.USERS(EMAIL) VALUES 1"; SQL statement:
                lowerCasedMessage.contains("unique index or primary key violation") ->
                    lowerCasedMessage
                        .substringAfter("public.${table.name}(")
                        .substringBefore(")")
                        .substringBefore(" ")
                // psql: duplicate key value violates unique constraint "users_email_key"
                lowerCasedMessage.startsWith("duplicate key value violates unique constraint") ->
                    lowerCasedMessage
                        .substringAfter("constraint \"${table.name}_")
                        .substringBefore("_key\"")
                else -> null
            }
        return fieldNamesProperties[fieldString?.lowercase(Locale.getDefault())]
    }

    @Suppress("UNCHECKED_CAST")
    fun pgException(e: PgException, instance: EntityWrapper): Exception =
        (pgException(e, instance.entity as T))

    fun pgException(e: PgException, instance: T): Exception {
        val field = computeAffectedField(e.errorMessage) ?: return e
        val value = field.invoke(instance)
        return UniqueConstraintViolatedException(e.errorMessage, e.cause, field, value)
    }
}
