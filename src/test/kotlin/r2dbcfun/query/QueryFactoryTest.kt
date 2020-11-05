package r2dbcfun.query

import io.kotest.core.spec.style.FunSpec
import io.mockk.MockKMatcherScope
import io.mockk.coVerify
import io.mockk.mockk
import io.r2dbc.spi.Connection
import r2dbcfun.ResultMapper
import r2dbcfun.test.TestObjects.Entity

fun <T> MockKMatcherScope.seqEq(seq: Sequence<T>) = match<Sequence<T>> {
    it.toList() == seq.toList()
}

class QueryFactoryTest : FunSpec({
    context("typesafe query factory") {
        val finder = mockk<ResultMapper<Entity>>(relaxed = true)
        val queryFactory = QueryFactory(Entity::class, finder)
        val connection = mockk<Connection>(relaxed = true)
        val condition = Entity::id.isEqualTo()
        test("can create query with one parameter") {
            val query = queryFactory.createQuery(condition)
            query.with(connection, 1).find()
        }
        test("can create query with two parameter") {
            val query = queryFactory.createQuery(condition, condition)
            query.with(connection, 1, 1).find()
        }
        test("can create query with three parameter") {
            val query = queryFactory.createQuery(condition, condition, condition)
            query.with(connection, 1, 1, 1).find()
        }
    }

})
