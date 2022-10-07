package io.the.orm

import io.the.orm.dbio.ConnectionProvider
import io.the.orm.dbio.TransactionProvider
import kotlin.reflect.KClass

class Repo(classes: List<KClass<out Any>>) {
    val entityRepos: Map<KClass<out Any>, SingleEntityRepo<out Any>> =
        classes.associateBy({ it }, { SingleEntityRepoImpl(it, classes.toSet()) })

    suspend inline fun <reified T : Any> create(connectionProvider: ConnectionProvider, entity: T): T =
        getRepo(T::class).create(connectionProvider, entity)

    suspend inline fun <reified T : Any> findById(connectionProvider: ConnectionProvider, id: PK): T =
        getRepo(T::class).findById(connectionProvider, id)

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : Any> getRepo(kClass: KClass<T>) = entityRepos[kClass] as SingleEntityRepo<T>

    inline fun <reified T : Any> queryFactory() = getRepo(T::class).queryFactory
}

open class ConnectedRepo internal constructor(
    open val connectionProvider: ConnectionProvider,
    val repo: Repo
) {
    suspend inline fun <reified T : Any> create(entity: T): T = repo.create(connectionProvider, entity)
    suspend inline fun <reified T : Any> findById(id: PK): T = repo.findById(connectionProvider, id)
}

class TransactionalRepo(override val connectionProvider: TransactionProvider, repo: Repo) :
    ConnectedRepo(connectionProvider, repo) {
    constructor(connectionProvider: TransactionProvider, classes: List<KClass<out Any>>) : this(
        connectionProvider,
        Repo(classes)
    )

    suspend fun <R> transaction(function: suspend (ConnectedRepo) -> R): R =
        connectionProvider.transaction { transactionConnectionProvider ->
            function(ConnectedRepo(transactionConnectionProvider, repo))
        }
}
