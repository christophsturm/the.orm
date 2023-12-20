package io.the.orm.transaction

import io.the.orm.ConnectedRepo
import io.the.orm.RepoRegistry
import io.the.orm.dbio.TransactionProvider
import kotlin.reflect.KClass

suspend inline fun <reified E1 : Any, Result> RepoTransactionProvider.transaction(
    noinline t: suspend (ConnectedRepo<E1>) -> Result
): Result = transaction(E1::class, t)

class RepoTransactionProvider(
    private val repos: RepoRegistry,
    val transactionProvider: TransactionProvider
) {
    suspend fun <Entity : Any, Result> transaction(
        clazz: KClass<Entity>,
        t: suspend (ConnectedRepo<Entity>) -> Result
    ): Result {
        val repo = repos.getRepo(clazz)
        return transactionProvider.transaction {
            val connectedRepo = ConnectedRepo(repo, it)
            t(connectedRepo)
        }
    }

    suspend fun <Entity1 : Any, Entity2 : Any, Result> transaction(
        clazz1: KClass<Entity1>,
        clazz2: KClass<Entity2>,
        t: suspend (ConnectedRepo<Entity1>, ConnectedRepo<Entity2>) -> Result
    ): Result {
        val repo = repos.getRepo(clazz1)
        val repo2 = repos.getRepo(clazz2)
        return transactionProvider.transaction {
            val connectedRepo1 = ConnectedRepo(repo, it)
            val connectedRepo2 = ConnectedRepo(repo2, it)
            t(connectedRepo1, connectedRepo2)
        }
    }

    suspend fun <Entity1 : Any, Entity2 : Any, Entity3 : Any, Result> transaction(
        clazz1: KClass<Entity1>,
        clazz2: KClass<Entity2>,
        clazz3: KClass<Entity3>,
        t:
            suspend (
                ConnectedRepo<Entity1>, ConnectedRepo<Entity2>, ConnectedRepo<Entity3>
            ) -> Result
    ): Result {
        val repo = repos.getRepo(clazz1)
        val repo2 = repos.getRepo(clazz2)
        val repo3 = repos.getRepo(clazz3)
        return transactionProvider.transaction {
            val connectedRepo1 = ConnectedRepo(repo, it)
            val connectedRepo2 = ConnectedRepo(repo2, it)
            val connectedRepo3 = ConnectedRepo(repo3, it)
            t(connectedRepo1, connectedRepo2, connectedRepo3)
        }
    }
}
