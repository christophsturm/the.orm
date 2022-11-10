package io.the.orm

import io.the.orm.dbio.ConnectionProvider
import io.the.orm.dbio.TransactionProvider
import kotlin.reflect.KClass

inline operator fun <reified Entity : Any> MultiRepo.Companion.invoke(): SingleEntityRepo<Entity> =
    invoke(Entity::class)

interface MultiRepo {
    companion object {
        operator fun invoke(classes: List<KClass<out Any>>) = MultiRepoImpl(classes)
        operator fun <Entity : Any> invoke(entity: KClass<Entity>): SingleEntityRepo<Entity> =
            SingleEntityRepoImpl(entity)
    }

    fun <T : Any> getRepo(kClass: KClass<T>): SingleEntityRepo<T>
}

suspend inline fun <reified T : Any> MultiRepo.create(connectionProvider: ConnectionProvider, entity: T): T =
    getRepo(T::class).create(connectionProvider, entity)

suspend inline fun <reified T : Any> MultiRepo.findById(connectionProvider: ConnectionProvider, id: PK): T =
    getRepo(T::class).findById(connectionProvider, id)

inline fun <reified T : Any> MultiRepo.queryFactory() = getRepo(T::class).queryFactory

class MultiRepoImpl(classes: List<KClass<out Any>>) : MultiRepo {
    private val entityRepos: Map<KClass<out Any>, SingleEntityRepo<out Any>> =
        classes.associateBy({ it }, { SingleEntityRepoImpl(it, classes.toSet()) })

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getRepo(kClass: KClass<T>) = entityRepos[kClass] as SingleEntityRepo<T>
}

interface ConnectedRepo {
    companion object {
        operator fun invoke(connectionProvider: ConnectionProvider, multiRepo: MultiRepo) =
            ConnectedRepoImpl(connectionProvider, multiRepo)
    }

    val connectionProvider: ConnectionProvider
    val multiRepo: MultiRepo
}

suspend inline fun <reified T : Any> ConnectedRepo.create(entity: T): T = multiRepo.create(connectionProvider, entity)
suspend inline fun <reified T : Any> ConnectedRepo.findById(id: PK): T = multiRepo.findById(connectionProvider, id)

data class ConnectedRepoImpl internal constructor(
    override val connectionProvider: ConnectionProvider,
    override val multiRepo: MultiRepo
) : ConnectedRepo

interface TransactionalRepo : ConnectedRepo {
    companion object {
        operator fun invoke(connectionProvider: TransactionProvider, multiRepo: MultiRepo) =
            TransactionalRepoImpl(connectionProvider, multiRepo)

        operator fun invoke(connectionProvider: TransactionProvider, classes: List<KClass<out Any>>) =
            TransactionalRepoImpl(
                connectionProvider,
                MultiRepo(classes)
            )
    }

    suspend fun <R> transaction(function: suspend (ConnectedRepo) -> R): R
}

class TransactionalRepoImpl(override val connectionProvider: TransactionProvider, override val multiRepo: MultiRepo) :
    TransactionalRepo {

    override suspend fun <R> transaction(function: suspend (ConnectedRepo) -> R): R =
        connectionProvider.transaction { transactionConnectionProvider ->
            function(ConnectedRepoImpl(transactionConnectionProvider, multiRepo))
        }
}
