package io.the.orm.dbio.vertx

import io.the.orm.dbio.DBTransaction
import io.vertx.reactivex.sqlclient.Transaction
import kotlinx.coroutines.rx2.await

class VertxTransaction(private val transaction: Transaction) : DBTransaction {
    override suspend fun rollbackTransaction() {
        transaction.rxRollback().await()
    }

    override suspend fun commitTransaction() {
        transaction.rxCommit().await()
    }

}
