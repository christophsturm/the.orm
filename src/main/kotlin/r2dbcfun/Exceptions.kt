package r2dbcfun

import kotlin.reflect.KProperty1

public open class RepositoryException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

public class NotFoundException(message: String) : RepositoryException(message)
public class UniqueConstraintViolatedException(
    message: String,
    cause: Throwable?,
    field: KProperty1<*, *>?,
    public val fieldValue: Any?
) : DataIntegrityViolationException(message + " field:${field?.name} value:$fieldValue", cause, field)

public open class DataIntegrityViolationException(
    message: String,
    cause: Throwable?,
    public val field: KProperty1<*, *>?
) :
    RepositoryException(message, cause)
