package io.the.orm.query

import failgood.Test
import failgood.describe
import failgood.mock.mock
import io.the.orm.dbio.ConnectionProvider
import io.the.orm.internal.IDHandler
import io.the.orm.internal.Table
import io.the.orm.internal.classinfo.ClassInfo
import io.the.orm.mapper.ResultMapper
import io.the.orm.test.TestObjects.Entity

@Test
class QueryFactoryTest {
    val context = describe(QueryFactory::class) {
        val resultMapper = mock<ResultMapper<Entity>>()
        val queryFactory =
            QueryFactory(
                Table("table"),
                resultMapper,
                mock(),
                IDHandler(Entity::class),
                mock(),
                ClassInfo(Entity::class, setOf())
            )
        val connection = mock<ConnectionProvider>()
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
