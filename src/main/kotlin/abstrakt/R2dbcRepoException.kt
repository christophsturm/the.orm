package abstrakt

public open class R2dbcRepoException : RuntimeException {
    public constructor(message: String) : super(message)
    public constructor(message: String, cause: Throwable) : super(message, cause)
}

public class NotFoundException(message: String) : R2dbcRepoException(message)

