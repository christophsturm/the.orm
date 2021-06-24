package io.the.orm.dbio.vertx

import io.the.orm.dbio.DBTransaction
import io.vertx.kotlin.coroutines.await
import io.vertx.sqlclient.Transaction

class VertxTransaction(private val transaction: Transaction) : DBTransaction {
    override suspend fun rollbackTransaction() {
        transaction.rollback().await()
    }

    override suspend fun commitTransaction() {
        transaction.commit().await()
    }

}
