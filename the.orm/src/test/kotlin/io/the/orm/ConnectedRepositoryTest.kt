package io.the.orm

import failgood.Test
import failgood.mock.mock
import failgood.mock.verify
import failgood.tests
import io.the.orm.dbio.ConnectionProvider
import io.the.orm.exp.example.products.ShopTest.tests
import io.the.orm.test.TestObjects.Entity
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isSameInstanceAs

@Test
class ConnectedRepositoryTest {
    val context = tests {
        val connection = mock<ConnectionProvider>()
        test("exposes Repository and Connection") {
            expectThat(ConnectedRepo.create<Entity>(connection)) {
                get { repo }.isA<Repo<Entity>>()
                get { this.connectionProvider }.isSameInstanceAs(connection)
            }
        }

        context("forwarding calls") {
            val repo = mock<Repo<Entity>>()
            val subject = ConnectedRepo(repo, connection)
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
                val id = 123L
                subject.findById(id)
                verify(repo) { findById(connection, id) }
            }
        }
    }
}
