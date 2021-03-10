package r2dbcfun.exp

import r2dbcfun.Repository
import r2dbcfun.dbio.ConnectionProvider
import r2dbcfun.dbio.TransactionProvider
import kotlin.reflect.KClass

class MultiRepo(classes: List<KClass<out Any>>) {
    val repos: Map<KClass<out Any>, Repository<out Any>> =
        classes.associateBy({ it }, { Repository(it, classes.toSet()) })

    suspend inline fun <reified T : Any> create(connectionProvider: ConnectionProvider, entity: T): T =
        getRepo(T::class).create(connectionProvider, entity)

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : Any> getRepo(kClass: KClass<T>) = repos[kClass] as Repository<T>

    inline fun <reified T : Any> queryFactory() = getRepo(T::class).queryFactory

}

open class ConnectedMultiRepo internal constructor(
    open val connectionProvider: ConnectionProvider,
    val repo: MultiRepo
) {
    @Suppress("UNCHECKED_CAST")
    suspend inline fun <reified T : Any> create(entity: T): T = repo.create(connectionProvider, entity)
}

class TransactionalMultiRepo(
    override val connectionProvider: TransactionProvider,
    repo: MultiRepo
) : ConnectedMultiRepo(connectionProvider, repo) {
    constructor(connectionProvider: TransactionProvider, classes: List<KClass<out Any>>) : this(
        connectionProvider,
        MultiRepo(classes)
    )

    suspend fun <R> transaction(function: suspend (ConnectedMultiRepo) -> R): R =
        connectionProvider.transaction { transactionConnectionProvider ->
            function(ConnectedMultiRepo(transactionConnectionProvider, repo))
        }

}
