package r2dbcfun.test

import failfast.FailFast.findTestClasses
import failfast.Suite
import io.mockk.impl.JvmMockKGateway
import r2dbcfun.TestConfig.H2_ONLY
import r2dbcfun.test.functional.TransactionFunctionalTest
import kotlin.concurrent.thread

fun main() {
    // spin up dependencies in separate threads
    thread {
        JvmMockKGateway()
    }
    if (!H2_ONLY)
        thread {
            postgresqlcontainer
        }
    Suite.fromClasses(findTestClasses(TransactionFunctionalTest::class)).run().check()
}

