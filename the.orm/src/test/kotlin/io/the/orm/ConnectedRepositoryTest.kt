package io.the.orm

import failfast.describe
import failfast.mock.mock
import failfast.mock.verify
import io.the.orm.dbio.ConnectionProvider
import io.the.orm.test.TestObjects.Entity
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isSameInstanceAs

object ConnectedRepositoryTest {
    val context = describe(io.the.orm.ConnectedRepository::class) {
        val connection = mock<ConnectionProvider>()
        test("exposes Repository and Connection") {
            expectThat(io.the.orm.ConnectedRepository.create<Entity>(connection)) {
                get { repository }.isA<io.the.orm.Repository<Entity>>()
                get { this.connectionProvider }.isSameInstanceAs(connection)
            }
        }

        context("forwarding calls") {
            val repo = mock<io.the.orm.Repository<Entity>>()
            val subject = io.the.orm.ConnectedRepository(repo, connection)
            val entity = Entity()
            test("create call") {
                subject.create(entity)
                verify(repo) { create(connection, entity) }
            }
            test("update call") {
                subject.update(entity)
                verify(repo) { update(connection, entity) }
            }
            test("findById call") {
                data class MyPK(override val id: Long) : io.the.orm.PK

                val id = MyPK(1)
                subject.findById(id)
                verify(repo) { findById(connection, id) }
            }
        }
    }
}
