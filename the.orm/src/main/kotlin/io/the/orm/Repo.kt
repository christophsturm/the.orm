package io.the.orm

import io.the.orm.dbio.ConnectionProvider
import io.the.orm.dbio.TransactionProvider
import kotlin.reflect.KClass

inline operator fun <reified Entity : Any> Repo.Companion.invoke(): SingleEntityRepo<Entity> = invoke(Entity::class)

interface Repo {
    companion object {
        operator fun invoke(classes: List<KClass<out Any>>) = RepoImpl(classes)
        operator fun <Entity : Any> invoke(entity: KClass<Entity>): SingleEntityRepo<Entity> =
            SingleEntityRepoImpl(entity)
    }

    fun <T : Any> getRepo(kClass: KClass<T>): SingleEntityRepo<T>
}

suspend inline fun <reified T : Any> Repo.create(connectionProvider: ConnectionProvider, entity: T): T =
    getRepo(T::class).create(connectionProvider, entity)

suspend inline fun <reified T : Any> Repo.findById(connectionProvider: ConnectionProvider, id: PK): T =
    getRepo(T::class).findById(connectionProvider, id)

inline fun <reified T : Any> Repo.queryFactory() = getRepo(T::class).queryFactory

class RepoImpl(classes: List<KClass<out Any>>) : Repo {
    private val entityRepos: Map<KClass<out Any>, SingleEntityRepo<out Any>> =
        classes.associateBy({ it }, { SingleEntityRepoImpl(it, classes.toSet()) })

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getRepo(kClass: KClass<T>) = entityRepos[kClass] as SingleEntityRepo<T>
}

interface ConnectedRepo {
    companion object {
        operator fun invoke(connectionProvider: ConnectionProvider, repo: Repo) =
            ConnectedRepoImpl(connectionProvider, repo)
    }

    val connectionProvider: ConnectionProvider
    val repo: Repo
}

suspend inline fun <reified T : Any> ConnectedRepo.create(entity: T): T = repo.create(connectionProvider, entity)
suspend inline fun <reified T : Any> ConnectedRepo.findById(id: PK): T = repo.findById(connectionProvider, id)

data class ConnectedRepoImpl internal constructor(
    override val connectionProvider: ConnectionProvider,
    override val repo: Repo
) : ConnectedRepo

interface TransactionalRepo : ConnectedRepo {
    companion object {
        operator fun invoke(connectionProvider: TransactionProvider, repo: Repo) =
            TransactionalRepoImpl(connectionProvider, repo)

        operator fun invoke(connectionProvider: TransactionProvider, classes: List<KClass<out Any>>) =
            TransactionalRepoImpl(
                connectionProvider,
                Repo(classes)
            )
    }

    suspend fun <R> transaction(function: suspend (ConnectedRepo) -> R): R
}

class TransactionalRepoImpl(override val connectionProvider: TransactionProvider, override val repo: Repo) :
    TransactionalRepo {

    override suspend fun <R> transaction(function: suspend (ConnectedRepo) -> R): R =
        connectionProvider.transaction { transactionConnectionProvider ->
            function(ConnectedRepoImpl(transactionConnectionProvider, repo))
        }
}
