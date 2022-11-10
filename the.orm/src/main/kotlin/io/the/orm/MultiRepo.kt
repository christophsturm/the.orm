package io.the.orm

import io.the.orm.dbio.ConnectionProvider
import io.the.orm.dbio.TransactionProvider
import kotlin.reflect.KClass

inline operator fun <reified Entity : Any> MultiRepo.Companion.invoke(): Repo<Entity> =
    invoke(Entity::class)

interface MultiRepo {
    companion object {
        operator fun invoke(classes: List<KClass<out Any>>) = MultiRepoImpl(classes)
        operator fun <Entity : Any> invoke(entity: KClass<Entity>): Repo<Entity> =
            RepoImpl(entity)
    }

    fun <T : Any> getRepo(kClass: KClass<T>): Repo<T>
}

suspend inline fun <reified T : Any> MultiRepo.create(connectionProvider: ConnectionProvider, entity: T): T =
    getRepo(T::class).create(connectionProvider, entity)

suspend inline fun <reified T : Any> MultiRepo.findById(connectionProvider: ConnectionProvider, id: PK): T =
    getRepo(T::class).findById(connectionProvider, id)

inline fun <reified T : Any> MultiRepo.queryFactory() = getRepo(T::class).queryFactory

class MultiRepoImpl(classes: List<KClass<out Any>>) : MultiRepo {
    private val entityRepos: Map<KClass<out Any>, Repo<out Any>> =
        classes.associateBy({ it }, { RepoImpl(it, classes.toSet()) })

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getRepo(kClass: KClass<T>) = entityRepos[kClass] as Repo<T>
}

interface ConnectedMultiRepo {
    companion object {
        operator fun invoke(connectionProvider: ConnectionProvider, multiRepo: MultiRepo) =
            ConnectedMultiRepoImpl(connectionProvider, multiRepo)
    }

    val connectionProvider: ConnectionProvider
    val multiRepo: MultiRepo
}

suspend inline fun <reified T : Any> ConnectedMultiRepo.create(entity: T): T = multiRepo.create(connectionProvider, entity)
suspend inline fun <reified T : Any> ConnectedMultiRepo.findById(id: PK): T = multiRepo.findById(connectionProvider, id)

data class ConnectedMultiRepoImpl internal constructor(
    override val connectionProvider: ConnectionProvider,
    override val multiRepo: MultiRepo
) : ConnectedMultiRepo

interface TransactionalMultiRepo : ConnectedMultiRepo {
    companion object {
        operator fun invoke(connectionProvider: TransactionProvider, multiRepo: MultiRepo) =
            TransactionalMultiRepoImpl(connectionProvider, multiRepo)

        operator fun invoke(connectionProvider: TransactionProvider, classes: List<KClass<out Any>>) =
            TransactionalMultiRepoImpl(
                connectionProvider,
                MultiRepo(classes)
            )
    }

    suspend fun <R> transaction(function: suspend (ConnectedMultiRepo) -> R): R
}

class TransactionalMultiRepoImpl(override val connectionProvider: TransactionProvider, override val multiRepo: MultiRepo) :
    TransactionalMultiRepo {

    override suspend fun <R> transaction(function: suspend (ConnectedMultiRepo) -> R): R =
        connectionProvider.transaction { transactionConnectionProvider ->
            function(ConnectedMultiRepoImpl(transactionConnectionProvider, multiRepo))
        }
}
