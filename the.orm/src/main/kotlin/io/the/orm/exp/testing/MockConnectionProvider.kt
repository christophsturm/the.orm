package io.the.orm.exp.testing

import io.the.orm.dbio.ConnectionProvider
import io.the.orm.dbio.DBConnection
import io.the.orm.dbio.DBResult
import io.the.orm.dbio.DBRow
import io.the.orm.dbio.DBTransaction
import io.the.orm.dbio.Statement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class MockConnectionProvider(private val dbConnection: DBConnection = MockDbConnection()) : ConnectionProvider {
    override suspend fun <T> withConnection(function: suspend (DBConnection) -> T): T = function(dbConnection)
}

class MockDbConnection : DBConnection {
    override fun createStatement(sql: String): Statement = MockStatement(sql)

    override fun createInsertStatement(sql: String): Statement = MockStatement(sql)

    override suspend fun beginTransaction(): DBTransaction = MockTransaction()

    override suspend fun close() {
    }

    override suspend fun execute(sql: String) {
    }
}

class MockTransaction : DBTransaction {
    override suspend fun rollbackTransaction() {
        TODO("Not yet implemented")
    }

    override suspend fun commitTransaction() {
        TODO("Not yet implemented")
    }
}

class MockStatement(val sql: String) : Statement {
    override suspend fun execute(types: List<Class<*>>, values: Sequence<Any?>): DBResult = MockDBResult()

    override suspend fun executeBatch(types: List<Class<*>>, valuesList: Sequence<Sequence<Any?>>): Flow<DBResult> {
        return flowOf()
    }
}

data class MockDBResult(val rows: List<DBRow> = listOf(), val rowsUpdated: Int = 0, val id: Long = 0) : DBResult {
    override suspend fun rowsUpdated(): Int = rowsUpdated
    override suspend fun getId(): Long = id

    override suspend fun <T : Any> map(mappingFunction: (t: DBRow) -> T): Flow<T> {
        return flowOf()
    }
}
