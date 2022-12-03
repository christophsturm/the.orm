package io.the.orm.transaction

import io.the.orm.ConnectedRepo
import io.the.orm.MultiRepo
import io.the.orm.dbio.TransactionProvider
import kotlin.reflect.KClass

suspend inline fun <reified Entity : Any, Result> RepoTransactionProvider.transaction(
    noinline t: suspend (ConnectedRepo<Entity>) -> Result
): Result =
    transaction(Entity::class, t)

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
}
