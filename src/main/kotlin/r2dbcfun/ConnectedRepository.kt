package r2dbcfun

import r2dbcfun.dbio.ConnectionProvider
import r2dbcfun.dbio.TransactionProvider

open class ConnectedRepository<T : Any>(
    val repository: Repository<T>,
    open val connectionProvider: ConnectionProvider
) {
    companion object {
        inline fun <reified T : Any> create(connection: ConnectionProvider): ConnectedRepository<T> =
            ConnectedRepository(RepositoryImpl(T::class), connection)
    }

    suspend fun create(entity: T): T = repository.create(connectionProvider, entity)
    suspend fun update(entity: T): Unit = repository.update(connectionProvider, entity)
    suspend fun findById(pk: PK): T = repository.findById(connectionProvider, pk)
}

class TransactionalRepository<T : Any>(
    repository: Repository<T>,
    override val connectionProvider: TransactionProvider
) : ConnectedRepository<T>(repository, connectionProvider) {
    companion object {
        inline fun <reified T : Any> create(connection: TransactionProvider): TransactionalRepository<T> =
            TransactionalRepository(RepositoryImpl(T::class), connection)
    }

    suspend fun <R> transaction(function: suspend (ConnectedRepository<T>) -> R): R =
        connectionProvider.transaction { transactionConnectionProvider ->
            function(ConnectedRepository(repository, transactionConnectionProvider))
        }
}
