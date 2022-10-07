package io.the.orm.exp

import io.the.orm.PK
import io.the.orm.SingleEntityRepo
import io.the.orm.SingleEntityRepoImpl
import io.the.orm.dbio.ConnectionProvider
import io.the.orm.dbio.TransactionProvider
import kotlin.reflect.KClass

class MultiRepo(classes: List<KClass<out Any>>) {
    val repos: Map<KClass<out Any>, SingleEntityRepo<out Any>> =
        classes.associateBy({ it }, { SingleEntityRepoImpl(it, classes.toSet()) })

    suspend inline fun <reified T : Any> create(connectionProvider: ConnectionProvider, entity: T): T =
        getRepo(T::class).create(connectionProvider, entity)

    suspend inline fun <reified T : Any> findById(connectionProvider: ConnectionProvider, id: PK): T =
        getRepo(T::class).findById(connectionProvider, id)

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : Any> getRepo(kClass: KClass<T>) = repos[kClass] as SingleEntityRepo<T>

    inline fun <reified T : Any> queryFactory() = getRepo(T::class).queryFactory
}

open class ConnectedMultiRepo internal constructor(
    open val connectionProvider: ConnectionProvider,
    val repo: MultiRepo
) {
    suspend inline fun <reified T : Any> create(entity: T): T = repo.create(connectionProvider, entity)
    suspend inline fun <reified T : Any> findById(id: PK): T = repo.findById(connectionProvider, id)
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
