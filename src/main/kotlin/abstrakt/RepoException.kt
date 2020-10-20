package abstrakt

public open class RepoException : RuntimeException {
    public constructor(message: String) : super(message)
    public constructor(message: String, cause: Throwable) : super(message, cause)
}

public class NotFoundException(message: String) : RepoException(message)

