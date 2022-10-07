package io.the.orm

import io.the.orm.dbio.ConnectionProvider
import io.the.orm.dbio.TransactionProvider
import kotlin.reflect.KClass

interface Repo {
    companion object {
        operator fun invoke(classes: List<KClass<out Any>>) = RepoImpl(classes)
    }

    @Suppress("UNCHECKED_CAST")
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
    val connectionProvider: ConnectionProvider
    val repo: Repo
}
suspend inline fun <reified T : Any> ConnectedRepo.create(entity: T): T = repo.create(connectionProvider, entity)
suspend inline fun <reified T : Any> ConnectedRepo.findById(id: PK): T = repo.findById(connectionProvider, id)

open class ConnectedRepoImpl internal constructor(
    override val connectionProvider: ConnectionProvider,
    override val repo: Repo
) : ConnectedRepo

class TransactionalRepo(override val connectionProvider: TransactionProvider, override val repo: Repo) :
    ConnectedRepo {
    constructor(connectionProvider: TransactionProvider, classes: List<KClass<out Any>>) : this(
        connectionProvider,
        Repo(classes)
    )

    suspend fun <R> transaction(function: suspend (ConnectedRepo) -> R): R =
        connectionProvider.transaction { transactionConnectionProvider ->
            function(ConnectedRepoImpl(transactionConnectionProvider, repo))
        }
}
