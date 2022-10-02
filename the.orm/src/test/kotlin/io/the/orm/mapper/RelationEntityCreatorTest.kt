package io.the.orm.mapper

import failgood.Test
import failgood.describe
import failgood.mock.mock
import io.the.orm.Repository
import io.the.orm.internal.IDHandler
import io.the.orm.internal.classinfo.ClassInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlin.reflect.KClass

@Test
object RelationEntityCreatorTest {
    data class Entity(val referencedEntity: ReferencedEntity, val id: Long? = null) {
        data class ReferencedEntity(val name: String, val id: Long? = null)
    }

    val tests = describe<RelationEntityCreator<Entity>> {
        it("resolves entities") {
            val repository = mock<Repository<*>>()
            val creator = RelationEntityCreator<Entity>(mapOf(Entity.ReferencedEntity::class to repository))
            val classInfo = ClassInfo(Entity::class, IDHandler(Entity::class), setOf(Entity.ReferencedEntity::class))
            creator.toEntities(
                flowOf(
                    listOf(
                        ResultPair(
                            classInfo.propertyToFieldInfo[Entity::referencedEntity]!!,
                            10L
                        )
                    )
                )
            )
        }
    }
}

internal class RelationEntityCreator<Entity : Any>(mapOf: Map<KClass<*>, Repository<*>>) : EntityCreator<Entity> {
    override fun toEntities(parameters: Flow<List<ResultPair>>): Flow<Entity> {
        return flow {
            val parameterList = parameters.toList()
        }
    }
}
