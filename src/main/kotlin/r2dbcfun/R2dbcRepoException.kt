package r2dbcfun

open class R2dbcRepoException : RuntimeException {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}

class NotFoundException(message: String) : R2dbcRepoException(message)

