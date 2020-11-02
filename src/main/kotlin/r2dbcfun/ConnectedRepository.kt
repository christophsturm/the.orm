package r2dbcfun

import io.r2dbc.spi.Connection

public class ConnectedRepository<T : Any>(private val repo: Repository<T>, private val connection: Connection) {
    public suspend fun create(entity: T): T = repo.create(connection, entity)
    public suspend fun update(entity: T): Unit = repo.update(connection, entity)
    public suspend fun findById(pk: PK): T = repo.findById(connection, pk)
}
