package r2dbcfun

import io.r2dbc.spi.Connection

public class ConnectedRepository<T : Any>(
    public val repository: Repository<T>,
    public val connection: ConnectionProvider
) {
    public companion object {
        public inline fun <reified T : Any> create(connection: Connection): ConnectedRepository<T> =
            create(ConnectionProvider(connection))

        public inline fun <reified T : Any> create(connection: ConnectionProvider): ConnectedRepository<T> =
            ConnectedRepository(Repository(T::class), connection)
    }

    public suspend fun create(entity: T): T = repository.create(connection, entity)
    public suspend fun update(entity: T): Unit = repository.update(connection, entity)
    public suspend fun findById(pk: PK): T = repository.findById(connection, pk)
}
