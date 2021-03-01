package r2dbcfun.dbio.vertx

import r2dbcfun.dbio.DBConnection
import r2dbcfun.dbio.DBResult
import r2dbcfun.dbio.Statement

class VertxConnection : DBConnection {
    override suspend fun executeSelect(parameterValues: Sequence<Any>, sql: String): DBResult {
        TODO("Not yet implemented")
    }

    override suspend fun beginTransaction() {
        TODO("Not yet implemented")
    }

    override suspend fun commitTransaction() {
        TODO("Not yet implemented")
    }

    override fun createStatement(sql: String): Statement {
        TODO("Not yet implemented")
    }

    override suspend fun rollbackTransaction() {
        TODO("Not yet implemented")
    }

}
