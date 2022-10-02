package io.the.orm.mapper

import failgood.Test
import failgood.describe
import failgood.mock.mock
import io.the.orm.Repository
import io.the.orm.dbio.ConnectionProvider
import io.the.orm.internal.IDHandler
import io.the.orm.internal.classinfo.ClassInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.toList
import kotlin.reflect.KClass

@Test
object RelationEntityCreatorTest {
    data class Entity(val referencedEntity: ReferencedEntity, val id: Long? = null) {
        data class ReferencedEntity(val name: String, val id: Long? = null)
    }

    val tests = describe<RelationEntityCreator<Entity>>(disabled = System.getenv("NEXT") == null) {
        it("resolves entities") {
            val connectionProvider = mock<ConnectionProvider>()
            val referencedEntity = Entity.ReferencedEntity("blah", 10)
            val repository = mock<Repository<Entity.ReferencedEntity>> {
                method { findByIds(connectionProvider, listOf(10)) }.returns(listOf(referencedEntity))
            }
            val creator = RelationEntityCreator<Entity>(mapOf(Entity.ReferencedEntity::class to repository))
            val classInfo = ClassInfo(Entity::class, IDHandler(Entity::class), setOf(Entity.ReferencedEntity::class))
            val result = creator.toEntities(
                flowOf(
                    listOf(
                        ResultPair(
                            classInfo.propertyToFieldInfo[Entity::referencedEntity]!!,
                            10L
                        ),
                        ResultPair(classInfo.propertyToFieldInfo[Entity::id]!!, 99)
                    )
                ),
                connectionProvider
            )
            assert(result.single() == Entity(referencedEntity, 99))
        }
    }
}

internal class RelationEntityCreator<Entity : Any>(mapOf: Map<KClass<*>, Repository<*>>) : EntityCreator<Entity> {
    override fun toEntities(results: Flow<List<ResultPair>>, connectionProvider: ConnectionProvider): Flow<Entity> {
        return flow {
            results.toList().forEach {
            }
        }
    }
}
