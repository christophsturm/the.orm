package r2dbcfun.test

import failfast.Suite
import r2dbcfun.ConnectedRepositoryTest
import r2dbcfun.RepositoryTest
import r2dbcfun.exp.R2dbcTest
import r2dbcfun.exp.VertxTest
import r2dbcfun.internal.IDHandlerTest
import r2dbcfun.query.QueryFactoryTest
import r2dbcfun.test.functional.ConstraintViolationFunctionalTest
import r2dbcfun.test.functional.ExamplesTest
import r2dbcfun.test.functional.QueryFactoryFunctionalTest
import r2dbcfun.test.functional.RepositoryFunctionalTest
import r2dbcfun.test.functional.TransactionFunctionalTest
import r2dbcfun.transaction.TransactionTest
import r2dbcfun.util.StringUtilTest

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
            R2dbcTest.context,
            VertxTest.contetxt,
            IDHandlerTest.context,
            QueryFactoryTest.context,
            TransactionTest.context,
            StringUtilTest.context
        )
    ).run().check()
}

