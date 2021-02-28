package r2dbcfun

import r2dbcfun.dbio.ConnectionProvider

class ConnectedRepository<T : Any>(
    val repository: Repository<T>,
    val connectionProvider: ConnectionProvider
) {
    companion object {
        inline fun <reified T : Any> create(connection: ConnectionProvider): ConnectedRepository<T> =
            ConnectedRepository(Repository(T::class), connection)
    }

    suspend fun create(entity: T): T = repository.create(connectionProvider, entity)
    suspend fun update(entity: T): Unit = repository.update(connectionProvider, entity)
    suspend fun findById(pk: PK): T = repository.findById(connectionProvider, pk)
}
