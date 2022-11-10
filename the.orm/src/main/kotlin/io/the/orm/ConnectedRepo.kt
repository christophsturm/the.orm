package io.the.orm

import io.the.orm.dbio.ConnectionProvider
import io.the.orm.dbio.TransactionProvider

interface ConnectedRepo<T : Any> {
    val repo: Repo<T>
    val connectionProvider: ConnectionProvider

    companion object {
        inline fun <reified T : Any> create(connection: ConnectionProvider): ConnectedRepo<T> =
            ConnectedRepoImpl(RepoImpl(T::class), connection)
        operator fun <T : Any> invoke(repo: Repo<T>, connectionProvider: ConnectionProvider) =
            ConnectedRepoImpl(repo, connectionProvider)
    }

    suspend fun create(entity: T): T

    suspend fun update(entity: T)

    suspend fun findById(pk: PK): T
}

class ConnectedRepoImpl<T : Any>(override val repo: Repo<T>, override val connectionProvider: ConnectionProvider) :
    ConnectedRepo<T> {

    override suspend fun create(entity: T): T = repo.create(connectionProvider, entity)
    override suspend fun update(entity: T): Unit = repo.update(connectionProvider, entity)
    override suspend fun findById(pk: PK): T = repo.findById(connectionProvider, pk)
}

interface TransactionalRepo<T : Any> : ConnectedRepo<T> {
    companion object {
        inline fun <reified T : Any> create(connection: TransactionProvider): TransactionalRepo<T> =
            TransactionalRepoImpl(RepoImpl(T::class), connection)
        operator fun <T : Any> invoke(repo: Repo<T>, connection: TransactionProvider): TransactionalRepo<T> =
            TransactionalRepoImpl(repo, connection)
    }
    override val repo: Repo<T>
    override val connectionProvider: TransactionProvider
    suspend fun <R> transaction(function: suspend (ConnectedRepo<T>) -> R): R
}

class TransactionalRepoImpl<T : Any>(
    override val repo: Repo<T>,
    override val connectionProvider: TransactionProvider
) : TransactionalRepo<T> {
    override suspend fun create(entity: T): T = repo.create(connectionProvider, entity)
    override suspend fun update(entity: T): Unit = repo.update(connectionProvider, entity)
    override suspend fun findById(pk: PK): T = repo.findById(connectionProvider, pk)

    override suspend fun <R> transaction(function: suspend (ConnectedRepo<T>) -> R): R =
        connectionProvider.transaction { transactionConnectionProvider ->
            function(ConnectedRepoImpl(repo, transactionConnectionProvider))
        }
}
