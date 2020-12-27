package r2dbcfun.test

import failfast.FailFast.findTestClasses
import failfast.Suite
import r2dbcfun.test.functional.TransactionFunctionalTest

fun main() {
    Suite.fromClasses(findTestClasses(TransactionFunctionalTest::class)).run().check(false)
}

