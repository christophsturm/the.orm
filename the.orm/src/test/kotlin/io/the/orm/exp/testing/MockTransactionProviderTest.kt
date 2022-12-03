package io.the.orm.exp.testing

import failgood.Test
import failgood.describe
import io.the.orm.dbio.ConnectionProvider
import io.the.orm.dbio.TransactionProvider
import kotlin.test.assertEquals

@Test
class MockTransactionProviderTest {
    val context = describe("the mock transaction provider") {
        it("calls the mock with the specified connection provider") {
            var connectionproviderPassedToBlock: ConnectionProvider? = null
            val suppliedConnectionProvider = MockConnectionProvider()
            val t: TransactionProvider = MockTransactionProvider(suppliedConnectionProvider)
            val result = t.transaction {
                connectionproviderPassedToBlock = it
                "blah"
            }
            assertEquals("blah", result)
            assertEquals(suppliedConnectionProvider, connectionproviderPassedToBlock)
        }
    }
}
