package r2dbcfun

import kotlin.reflect.KProperty1

public open class RepositoryException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause) {
}

public class NotFoundException(message: String) : RepositoryException(message)
public class UniqueConstraintViolatedException(
    message: String,
    cause: Throwable?,
    fieldName: KProperty1<*, *>?
) : DataIntegrityViolationException(message, cause, fieldName)

public open class DataIntegrityViolationException(
    message: String,
    cause: Throwable?,
    public val fieldName: KProperty1<*, *>?
) :
    RepositoryException(message, cause) {

}
