package io.the.orm.dbio

import io.the.orm.test.describeOnAllDbs

object TransactionProviderTest {
    val context = describeOnAllDbs<TransactionProvider> {
        val transactionProvider = it
        // nothing here yet
    }
}
