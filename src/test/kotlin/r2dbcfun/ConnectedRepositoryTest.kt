package r2dbcfun

import io.kotest.core.spec.style.FunSpec
import io.mockk.coVerify
import io.mockk.mockk
import io.r2dbc.spi.Connection
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo

class ConnectedRepositoryTest :
    FunSpec(
        {
            data class Entity(val id: Long? = null)
            context("ConnectedRepository") {
                val connection = mockk<Connection>()
                test("exposes Repository and Connection") {
                    expectThat(ConnectedRepository.create<Entity>(connection)) {
                        get { repository }.isA<Repository<Entity>>()
                        get { this.connection }.isEqualTo(connection)
                    }
                }

                context("forwarding calls") {
                    val repo = mockk<Repository<Entity>>(relaxed = true)
                    val subject = ConnectedRepository(repo, connection)
                    val entity = Entity()
                    test("create call") {
                        subject.create(entity)
                        coVerify { repo.create(connection, entity) }
                    }
                    test("update call") {
                        subject.update(entity)
                        coVerify { repo.update(connection, entity) }
                    }
                    test("findById call") {
                        data class MyPK(override val id: Long) : PK

                        val id = MyPK(1)
                        subject.findById(id)
                        coVerify { repo.findById(connection, id) }
                    }
                }
            }
        }
    )
