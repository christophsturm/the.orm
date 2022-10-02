package io.the.orm

import failgood.Test
import failgood.describe
import failgood.mock.mock
import failgood.mock.verify
import io.the.orm.dbio.ConnectionProvider
import io.the.orm.test.TestObjects.Entity
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isSameInstanceAs

@Test
class ConnectedRepositoryTest {
    val context = describe(ConnectedRepository::class) {
        val connection = mock<ConnectionProvider>()
        test("exposes Repository and Connection") {
            expectThat(ConnectedRepository.create<Entity>(connection)) {
                get { repository }.isA<Repository<Entity>>()
                get { this.connectionProvider }.isSameInstanceAs(connection)
            }
        }

        context("forwarding calls") {
            val repo = mock<Repository<Entity>>()
            val subject = ConnectedRepository(repo, connection)
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
                val id = 1L
                subject.findById(id)
                verify(repo) { findById(connection, id) }
            }
        }
    }
}
