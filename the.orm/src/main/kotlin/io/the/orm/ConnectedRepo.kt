package io.the.orm

import io.the.orm.dbio.ConnectionProvider
import io.the.orm.dbio.TransactionProvider

open class ConnectedRepo<T : Any>(
    val repository: Repo<T>,
    open val connectionProvider: ConnectionProvider
) {
    companion object {
        inline fun <reified T : Any> create(connection: ConnectionProvider): ConnectedRepo<T> =
            ConnectedRepo(RepoImpl(T::class), connection)
    }

    suspend fun create(entity: T): T = repository.create(connectionProvider, entity)
    suspend fun update(entity: T): Unit = repository.update(connectionProvider, entity)
    suspend fun findById(pk: PK): T = repository.findById(connectionProvider, pk)
}

class TransactionalRepo<T : Any>(
    repository: Repo<T>,
    override val connectionProvider: TransactionProvider
) : ConnectedRepo<T>(repository, connectionProvider) {
    companion object {
        inline fun <reified T : Any> create(connection: TransactionProvider): TransactionalRepo<T> =
            TransactionalRepo(RepoImpl(T::class), connection)
    }

    suspend fun <R> transaction(function: suspend (ConnectedRepo<T>) -> R): R =
        connectionProvider.transaction { transactionConnectionProvider ->
            function(ConnectedRepo(repository, transactionConnectionProvider))
        }
}
