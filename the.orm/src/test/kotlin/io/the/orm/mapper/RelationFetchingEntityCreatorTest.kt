package io.the.orm.mapper

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
import io.the.orm.query.QueryFactory
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.single
import kotlin.test.assertEquals

@Test
object RelationFetchingEntityCreatorTest {

    val tests = describe<RelationFetchingEntityCreator<*>> {
        val connectionProvider = mock<ConnectionProvider>()
        describe("belongs to relation") {
            it("resolves belongs to entities that do not support lazy loading") {
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
                    classInfo,
                    classInfo.hasManyRelations.map {
                        it.repo.queryFactory.createQuery(it.dbFieldName + "=ANY(?)")
                    }
                )
                val result = creator.toEntities(
                    flowOf(ResultLine(listOf(99L), listOf(10L))), setOf(), connectionProvider
                )
                assert(result.single() == Entity(referencedEntity, 99))
            }
        }
        describe("has many relations") {
            val referencedEntity1 = ReferencedEntity("blah", BelongsTo.BelongsToNotLoaded(Entity::class, 10), 10)
            val referencedEntity2 = ReferencedEntity("blah", BelongsTo.BelongsToNotLoaded(Entity::class, 10), 10)
            val repository = mock<Repo<ReferencedEntity>> {
//                method { findByIds(any(), any()) }.returns(mapOf(10L to referencedEntity))
            }
            val repoRegistry = RepoRegistry(setOf(Entity::class, ReferencedEntity::class))
            val classInfo = (repoRegistry.getRepo(Entity::class) as RepoImpl).classInfo
            val queryWithParameters = mock<QueryFactory<out Any>.QueryWithParameters> {
                method { findAndTransform<Map<PK, Set<Any>>>(connectionProvider, setOf()) { mapOf() } }.returns(
                    mapOf(
                        99L to setOf(referencedEntity1, referencedEntity2)
                    )
                )
            }
            val queryMock = mock<QueryFactory<out Any>.Query> {
                method { with() }.returns(queryWithParameters)
            }
            val creator = RelationFetchingEntityCreator(
                listOf(),
                StreamingEntityCreator(classInfo),
                classInfo,
                listOf(queryMock)
            )
            it("does not resolve has many relations when they are not contained in fetchRelations") {
                val result = creator.toEntities(
                    flowOf(ResultLine(listOf(99L), listOf())), setOf(), connectionProvider
                )
                assertEquals(Entity(LazyHasMany(), 99), result.single())
            }
            it("resolves has many relations when they are contained in fetchRelations") {
                val result = creator.toEntities(
                    flowOf(ResultLine(listOf(99L), listOf())), setOf(Entity::referencedEntities), connectionProvider
                )
                assertEquals(Entity(LazyHasMany(setOf(referencedEntity1, referencedEntity2)), 99), result.single())
            }
        }
    }
}

data class ReferencedEntity(val name: String, val entity: BelongsTo<Entity>, val id: PK? = null)
data class Entity(val referencedEntities: HasMany<ReferencedEntity>, val id: PK? = null)
