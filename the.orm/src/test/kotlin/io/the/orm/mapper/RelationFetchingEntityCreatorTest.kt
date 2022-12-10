package io.the.orm.mapper

import failgood.Ignored
import failgood.Test
import failgood.describe
import failgood.mock.mock
import io.the.orm.PK
import io.the.orm.Repo
import io.the.orm.RepoImpl
import io.the.orm.RepoRegistry
import io.the.orm.dbio.ConnectionProvider
import io.the.orm.exp.relations.BelongsTo
import io.the.orm.exp.relations.HasMany
import io.the.orm.exp.relations.LazyHasMany
import io.the.orm.internal.classinfo.ClassInfo
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.single

@Test
object RelationFetchingEntityCreatorTest {

    val tests = describe<RelationFetchingEntityCreator<*>> {
        val connectionProvider = mock<ConnectionProvider>()
        it("resolves belongs to entities") {
            data class ReferencedEntity(val name: String, val id: PK? = null)
            data class Entity(val referencedEntity: ReferencedEntity, val id: PK? = null)

            val referencedEntity = ReferencedEntity("blah", 10)
            val repository = mock<Repo<ReferencedEntity>> {
                method { findByIds(any(), any()) }.returns(mapOf(10L to referencedEntity))
            }
            val classInfo = ClassInfo(Entity::class, setOf(ReferencedEntity::class))
            val creator = RelationFetchingEntityCreator(
                listOf(repository),
                StreamingEntityCreator(classInfo),
                classInfo
            )
            val result = creator.toEntities(
                flowOf(ResultLine(listOf(99L), listOf(10L))), connectionProvider
            )
            assert(result.single() == Entity(referencedEntity, 99))
        }
        it(
            "resolves hasMany relations",
            ignored = if (System.getenv("NEXT") == null) Ignored.Because("NEXT") else null
        ) {

            val referencedEntity1 = ReferencedEntity("blah", BelongsTo.BelongsToNotLoaded(Entity::class, 10), 10)
            val referencedEntity2 = ReferencedEntity("blah", BelongsTo.BelongsToNotLoaded(Entity::class, 10), 10)
            val repository = mock<Repo<ReferencedEntity>> {
//                method { findByIds(any(), any()) }.returns(mapOf(10L to referencedEntity))
            }
            val repoRegistry = RepoRegistry(setOf(Entity::class, ReferencedEntity::class))
            val classInfo = (repoRegistry.getRepo(Entity::class) as RepoImpl).classInfo
            val creator = RelationFetchingEntityCreator(
                listOf(repository),
                StreamingEntityCreator(classInfo),
                classInfo
            )
            val result = creator.toEntities(
                flowOf(ResultLine(listOf(99L), listOf(10L))), connectionProvider
            )
            assert(result.single() == Entity(LazyHasMany(setOf(referencedEntity1, referencedEntity2)), 99))
        }
    }
}
data class ReferencedEntity(val name: String, val entity: BelongsTo<Entity>, val id: PK? = null)
data class Entity(val referencedEntities: HasMany<ReferencedEntity>, val id: PK? = null)
