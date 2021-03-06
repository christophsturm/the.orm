package r2dbcfun.dbio.vertx

import io.vertx.reactivex.sqlclient.Transaction
import kotlinx.coroutines.rx2.await
import r2dbcfun.dbio.DBTransaction

class VertxTransaction(private val transaction: Transaction) : DBTransaction {
    override suspend fun rollbackTransaction() {
        transaction.rxRollback().await()
    }

    override suspend fun commitTransaction() {
        transaction.rxCommit().await()
    }

}
