package r2dbcfun

import io.r2dbc.spi.Connection
import r2dbcfun.r2dbc.ConnectionProvider

class ConnectedRepository<T : Any>(
    val repository: Repository<T>,
    val connection: ConnectionProvider
) {
    companion object {
        inline fun <reified T : Any> create(connection: Connection): ConnectedRepository<T> =
            create(ConnectionProvider(connection))

        inline fun <reified T : Any> create(connection: ConnectionProvider): ConnectedRepository<T> =
            ConnectedRepository(Repository(T::class), connection)
    }

    suspend fun create(entity: T): T = repository.create(connection, entity)
    suspend fun update(entity: T): Unit = repository.update(connection, entity)
    suspend fun findById(pk: PK): T = repository.findById(connection, pk)
}
