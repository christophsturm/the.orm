package r2dbcfun

public open class RepositoryException(
    message: String,
    cause: Throwable? = null,
    public val sqlState: String? = null,
    public val errorCode: Int? = null
) : RuntimeException(message, cause) {
}

public class NotFoundException(message: String) : RepositoryException(message)
public class DataIntegrityViolationException(message: String, cause: Throwable?, sqlState: String?, errorCode: Int) :
    RepositoryException(message, cause, sqlState, errorCode)
