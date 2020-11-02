package r2dbcfun

import io.kotest.core.spec.style.FunSpec
import io.mockk.coVerify
import io.mockk.mockk
import io.r2dbc.spi.Connection

class ConnectedRepositoryTest : FunSpec({
    data class Entity(val id: Long? = null)
    context("ConnectedRepository") {
        val connection = mockk<Connection>()
        test("wraps a Repository and has a Connection") {
            ConnectedRepository(Repository.create<Entity>(), connection)
        }
        context("forwarding calls") {
            val repo = mockk<Repository<Entity>>(relaxed = true)
            val subject = ConnectedRepository(repo, connection)
            val entity = Entity()
            test("create call") {
                subject.create(entity)
                coVerify {
                    repo.create(connection, entity)
                }
            }
            test("update call") {
                subject.update(entity)
                coVerify {
                    repo.update(connection, entity)
                }
            }
            test("findById call") {
                data class MyPK(override val id: Long) : PK

                val id = MyPK(1)
                subject.findById(id)
                coVerify {
                    repo.findById(connection, id)
                }
            }

        }
    }
})


