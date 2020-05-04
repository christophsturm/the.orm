package r2dbcfun

open class R2dbcRepoException(message: String) : RuntimeException(message)
class NotFoundException(message: String) : R2dbcRepoException(message)

