package io.the.orm.transaction

import io.the.orm.ConnectedRepo
import io.the.orm.MultiRepo
import io.the.orm.dbio.TransactionProvider
import kotlin.reflect.KClass

suspend inline fun <reified E1 : Any, Result> RepoTransactionProvider.transaction(
    noinline t: suspend (ConnectedRepo<E1>) -> Result
): Result =
    transaction(E1::class, t)

class RepoTransactionProvider(private val repos: MultiRepo, private val transactionProvider: TransactionProvider) {
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
    suspend fun <Entity : Any, E2 : Any, Result> transaction(
        clazz: KClass<Entity>,
        clazz2: KClass<E2>,
        t: suspend (ConnectedRepo<Entity>, ConnectedRepo<E2>) -> Result
    ): Result {
        val repo = repos.getRepo(clazz)
        val repo2 = repos.getRepo(clazz2)
        return transactionProvider.transaction {
            val connectedRepo = ConnectedRepo(repo, it)
            val connectedRepo2 = ConnectedRepo(repo2, it)
            t(connectedRepo, connectedRepo2)
        }
    }
}
