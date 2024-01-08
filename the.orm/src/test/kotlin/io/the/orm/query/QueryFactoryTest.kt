package io.the.orm.query

import failgood.Test
import failgood.mock.mock
import failgood.tests
import io.the.orm.internal.IDHandler
import io.the.orm.internal.classinfo.EntityInfo
import io.the.orm.internal.classinfo.Table
import io.the.orm.mapper.ResultMapper
import io.the.orm.test.TestObjects.Entity

@Test
class QueryFactoryTest {
    val context = tests {
        val resultMapper = mock<ResultMapper<Entity>>()
        val entityInfo = EntityInfo("entity", Table("entity"), listOf())
        val queryFactory =
            QueryFactory(resultMapper, mock(), IDHandler(Entity::class), mock(), entityInfo)
        val condition = Entity::id.isEqualTo()
        test("can create query with one parameter") {
            val query = queryFactory.createQuery(condition)
            query.with(1)
        }
        test("can create query with two parameter") {
            val query = queryFactory.createQuery(condition, condition)
            query.with(1, 1)
        }
        test("can create query with three parameter") {
            val query = queryFactory.createQuery(condition, condition, condition)
            query.with(1, 1, 1)
        }
    }
}
