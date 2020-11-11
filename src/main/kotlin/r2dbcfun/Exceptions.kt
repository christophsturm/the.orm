package r2dbcfun

public open class RepositoryException : RuntimeException {
    public constructor(message: String) : super(message)
    public constructor(message: String, cause: Throwable) : super(message, cause)
}

public class NotFoundException(message: String) : RepositoryException(message)
