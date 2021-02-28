package r2dbcfun.query

import failfast.describe
import io.mockk.mockk
import r2dbcfun.ResultMapper
import r2dbcfun.dbio.r2dbc.ConnectionProvider
import r2dbcfun.test.TestObjects.Entity

object QueryFactoryTest {
    val context = describe(QueryFactory::class) {
        val resultMapper = mockk<ResultMapper<Entity>>(relaxed = true)
        val queryFactory = QueryFactory(Entity::class, resultMapper, mockk(), mockk(), mockk())
        val connection = mockk<ConnectionProvider>()
        val condition = Entity::id.isEqualTo()
        test("can create query with one parameter") {
            val query = queryFactory.createQuery(condition)
            query.with(connection, 1)
        }
        test("can create query with two parameter") {
            val query = queryFactory.createQuery(condition, condition)
            query.with(connection, 1, 1)
        }
        test("can create query with three parameter") {
            val query = queryFactory.createQuery(condition, condition, condition)
            query.with(connection, 1, 1, 1)
        }
    }

}
