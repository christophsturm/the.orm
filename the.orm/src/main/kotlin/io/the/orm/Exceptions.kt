package io.the.orm

import kotlin.reflect.KProperty1

open class OrmException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)

class NotFoundException(message: String) : OrmException(message)

class UniqueConstraintViolatedException(
    message: String,
    cause: Throwable?,
    field: KProperty1<*, *>?,
    val fieldValue: Any?
) :
    DataIntegrityViolationException(
        message + " field:${field?.name} value:$fieldValue",
        cause,
        field
    )

open class DataIntegrityViolationException(
    message: String,
    cause: Throwable?,
    val field: KProperty1<*, *>?
) : OrmException(message, cause)

/** If you receive this exception its probably a bug. please submit an issue */
class UnexpectedDatabaseErrorException(message: String, cause: Throwable?) :
    OrmException(message, cause)
