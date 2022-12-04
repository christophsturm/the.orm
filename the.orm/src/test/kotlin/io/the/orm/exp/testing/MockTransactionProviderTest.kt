package io.the.orm.exp.testing

import failgood.Test
import failgood.describe
import io.the.orm.dbio.ConnectionProvider
import io.the.orm.dbio.TransactionProvider
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@Test
class MockTransactionProviderTest {
    val context = describe("the mock transaction provider") {
        val dbConnection = MockDbConnection()
        val connectionProvider = MockConnectionProvider(dbConnection)
        val subject: TransactionProvider = MockTransactionProvider(connectionProvider)
        it("calls the mock with the specified connection provider") {
            var connectionproviderPassedToBlock: ConnectionProvider? = null
            val result = subject.transaction {
                connectionproviderPassedToBlock = it
                "blah"
            }
            assertEquals("blah", result)
            assertEquals(connectionProvider, connectionproviderPassedToBlock)
        }
        it("records statements") {
            subject.transaction {
                it.withConnection {
                    val statement = it.createStatement("blah")
                    statement.execute()
                }
            }
            val singleStatement = assertNotNull(dbConnection.events.single())
            assert(singleStatement.sql == "blah")
            assert(singleStatement.events.single() == MockStatement.Executed(listOf(), sequenceOf()))
        }
    }
}
