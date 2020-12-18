package r2dbcfun.test

import nanotest.Suite
import r2dbcfun.ConnectedRepositoryTest
import r2dbcfun.RepositoryTest
import r2dbcfun.test.functional.ConstraintViolationFunctionalTest
import r2dbcfun.test.functional.ExamplesTest
import r2dbcfun.test.functional.QueryFactoryFunctionalTest
import r2dbcfun.test.functional.RepositoryFunctionalTest
import r2dbcfun.test.functional.TransactionFunctionalTest

fun main() {
    Suite(
        listOf(
            TransactionFunctionalTest.context,
            ConnectedRepositoryTest.context,
            RepositoryTest.context,
            ExamplesTest.context,
            ConstraintViolationFunctionalTest.context,
            QueryFactoryFunctionalTest.context,
            RepositoryFunctionalTest.context,
        ),
    ).run().check()
}
