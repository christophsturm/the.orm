package io.the.orm.mapper

import failgood.Test
import failgood.describe
import failgood.mock.mock
import io.the.orm.PK
import io.the.orm.Repository
import io.the.orm.dbio.ConnectionProvider
import io.the.orm.internal.IDHandler
import io.the.orm.internal.classinfo.ClassInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.toList

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
                method { findByIds(any(), any()) }.returns(mapOf(10L to referencedEntity))
            }
            val classInfo = ClassInfo(Entity::class, IDHandler(Entity::class), setOf(Entity.ReferencedEntity::class))
            val creator = RelationEntityCreator(listOf(repository), StreamingEntityCreator(classInfo))
            val result = creator.toEntities(
                flowOf(ResultLine(listOf(99L), listOf(10L))), connectionProvider
            )
            assert(result.single() == Entity(referencedEntity, 99))
        }
    }
}

internal class RelationEntityCreator<Entity : Any>(
    private val repos: List<Repository<*>>,
    val creator: EntityCreator<Entity>
) {
    fun toEntities(results: Flow<ResultLine>, connectionProvider: ConnectionProvider): Flow<Entity> {
        return flow {
            val idLists = Array(repos.size) { mutableSetOf<PK>() }
            val resultsList = results.toList()
            resultsList.forEach { resultLine ->
                resultLine.relations.forEachIndexed { idx, v ->
                    idLists[idx].add(v as PK)
                }
            }
            val relations =
                idLists.mapIndexed { index, longs ->
                    repos[index].findByIds(connectionProvider, longs.toList())
                }
            creator.toEntities(resultsList.asFlow(), relations).collect {
                emit(it)
            }
        }
    }
}
