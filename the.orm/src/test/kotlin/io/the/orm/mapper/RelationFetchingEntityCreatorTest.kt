package io.the.orm.mapper

import failgood.Test
import failgood.describe
import failgood.mock.mock
import io.the.orm.PK
import io.the.orm.SingleEntityRepo
import io.the.orm.dbio.ConnectionProvider
import io.the.orm.internal.classinfo.ClassInfo
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.single

@Test
object RelationFetchingEntityCreatorTest {
    data class Entity(val referencedEntity: ReferencedEntity, val id: PK? = null) {
        data class ReferencedEntity(val name: String, val id: PK? = null)
    }

    val tests = describe<RelationFetchingEntityCreator<Entity>> {
        it("resolves entities") {
            val connectionProvider = mock<ConnectionProvider>()
            val referencedEntity = Entity.ReferencedEntity("blah", 10)
            val repository = mock<SingleEntityRepo<Entity.ReferencedEntity>> {
                method { findByIds(any(), any()) }.returns(mapOf(10L to referencedEntity))
            }
            val classInfo = ClassInfo(Entity::class, setOf(Entity.ReferencedEntity::class))
            val creator = RelationFetchingEntityCreator(listOf(repository), StreamingEntityCreator(classInfo))
            val result = creator.toEntities(
                flowOf(ResultLine(listOf(99L), listOf(10L))), connectionProvider
            )
            assert(result.single() == Entity(referencedEntity, 99))
        }
    }
}
