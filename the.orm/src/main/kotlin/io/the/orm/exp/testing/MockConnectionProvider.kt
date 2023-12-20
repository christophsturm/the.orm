package io.the.orm.exp.testing

import io.the.orm.dbio.ConnectionProvider
import io.the.orm.dbio.DBConnection
import io.the.orm.dbio.DBResult
import io.the.orm.dbio.DBRow
import io.the.orm.dbio.DBTransaction
import io.the.orm.dbio.Statement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class MockConnectionProvider(private val dbConnection: DBConnection = MockDbConnection()) :
    ConnectionProvider {
    override suspend fun <T> withConnection(function: suspend (DBConnection) -> T): T =
        function(dbConnection)
}

class MockDbConnection : DBConnection {
    val events = mutableListOf<MockStatement>()

    override fun createStatement(sql: String): Statement = report(MockStatement(sql))

    private fun report(mockStatement: MockStatement): Statement {
        events.add(mockStatement)
        return mockStatement
    }

    override fun createInsertStatement(sql: String): Statement = MockStatement(sql)

    override suspend fun beginTransaction(): DBTransaction = MockTransaction()

    override suspend fun close() {}

    override suspend fun execute(sql: String) {}
}

class MockTransaction : DBTransaction {
    override suspend fun rollbackTransaction() {}

    override suspend fun commitTransaction() {}
}

class MockStatement(val sql: String) : Statement {
    data class Executed(val types: List<Class<*>>, val values: List<Any?>)

    val events = mutableListOf<Executed>()

    override suspend fun execute(types: List<Class<*>>, values: List<Any?>): DBResult {
        events.add(Executed(types, values))
        return MockDBResult()
    }

    override suspend fun executeBatch(
        types: List<Class<*>>,
        valuesList: List<List<Any?>>
    ): Flow<DBResult> {
        return flowOf()
    }
}

data class MockDBResult(
    val rows: List<DBRow> = listOf(),
    val rowsUpdated: Long = 0,
    val id: Long = 0
) : DBResult {
    override suspend fun rowsUpdated(): Long = rowsUpdated

    override suspend fun getId(): Long = id

    override fun asMapFlow(): Flow<Map<String, Any?>> {
        return flowOf()
    }

    override fun asListFlow(expectedLength: Int): Flow<List<Any?>> {
        return flowOf()
    }

    override suspend fun <T : Any> map(mappingFunction: (t: DBRow) -> T): Flow<T> {
        return flowOf()
    }
}
