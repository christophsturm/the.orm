package io.the.orm.mapper

import failgood.Test
import failgood.mock.mock
import failgood.mock.the
import failgood.tests
import io.the.orm.PKType
import io.the.orm.Repo
import io.the.orm.RepoImpl
import io.the.orm.RepoRegistry
import io.the.orm.dbio.ConnectionProvider
import io.the.orm.internal.classinfo.ClassInfo
import io.the.orm.query.Query
import io.the.orm.query.QueryWithParameters
import io.the.orm.relations.BelongsTo
import io.the.orm.relations.HasMany
import io.the.orm.relations.LazyHasMany
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.single

@Test
object RelationFetchingEntityCreatorTest {
    val tests = tests {
        val connectionProvider = mock<ConnectionProvider>()
        describe("belongs to relation") {
            data class ReferencedEntity(val name: String, val id: PKType? = null)
            val referencedEntity = ReferencedEntity("blah", 10)
            it("resolves belongs to entities that do not support lazy loading") {
                data class Entity(val referencedEntity: ReferencedEntity, val id: PKType? = null)

                val repository =
                    mock<Repo<ReferencedEntity>> {
                        method { findByIds(any(), any()) }.returns(mapOf(10L to referencedEntity))
                    }
                val classInfo = ClassInfo(Entity::class, setOf(ReferencedEntity::class))
                val entityInfo = classInfo.entityInfo
                val creator =
                    RelationFetchingEntityCreator(
                        listOf(repository),
                        StreamingEntityCreator(classInfo),
                        entityInfo,
                        entityInfo.hasManyRelations.map {
                            it.repo.queryFactory.createQuery(it.dbFieldName + "=ANY(?)")
                        }
                    )
                val result =
                    creator.toEntities(
                        flowOf(ResultLine(listOf(99L), listOf(10L))),
                        setOf(),
                        connectionProvider
                    )
                assert(result.single() == Entity(referencedEntity, 99))
            }
            describe("entities that support lazy loading") {
                data class Entity(
                    val referencedEntity: BelongsTo<ReferencedEntity>,
                    val id: PKType? = null
                )
                val repository = mock<Repo<ReferencedEntity>>()
                val classInfo = ClassInfo(Entity::class, setOf(ReferencedEntity::class))
                val creator =
                    RelationFetchingEntityCreator(
                        listOf(repository),
                        StreamingEntityCreator(classInfo),
                        classInfo.entityInfo,
                        classInfo.entityInfo.hasManyRelations.map {
                            it.repo.queryFactory.createQuery(it.dbFieldName + "=ANY(?)")
                        }
                    )
                it("does not resolve the relationship when it is not specified in fetchRelations") {
                    val result =
                        creator.toEntities(
                            flowOf(ResultLine(listOf(99L), listOf(10L))),
                            setOf(),
                            connectionProvider
                        )
                    assert(result.single() == Entity(BelongsTo.BelongsToNotLoaded(10), 99))
                }
                it("resolves the relationship when it is specified in fetchRelations") {
                    the(repository) {
                        method { findByIds(any(), any()) }.returns(mapOf(10L to referencedEntity))
                    }
                    val result =
                        creator.toEntities(
                            flowOf(ResultLine(listOf(99L), listOf(10L))),
                            setOf(Entity::referencedEntity),
                            connectionProvider
                        )
                    assert(result.single() == Entity(BelongsTo.BelongsToImpl(referencedEntity), 99))
                }
            }
        }
        describe("has many relations") {
            val referencedEntity1 = ReferencedEntity("blah", BelongsTo.BelongsToNotLoaded(10), 10)
            val referencedEntity2 = ReferencedEntity("blah", BelongsTo.BelongsToNotLoaded(10), 10)
            val repoRegistry = RepoRegistry(setOf(Entity::class, ReferencedEntity::class))
            val classInfo = (repoRegistry.getRepo(Entity::class) as RepoImpl).classInfo
            val queryWithParameters =
                mock<QueryWithParameters<*>> {
                    method {
                            findAndTransform<Map<PKType, Set<Any>>>(connectionProvider, setOf()) {
                                mapOf()
                            }
                        }
                        .returns(mapOf(99L to setOf(referencedEntity1, referencedEntity2)))
                }
            val queryMock = mock<Query<*>> { method { with() }.returns(queryWithParameters) }
            val creator =
                RelationFetchingEntityCreator(
                    listOf(),
                    StreamingEntityCreator(classInfo),
                    classInfo.entityInfo,
                    listOf(queryMock)
                )
            it(
                "does not resolve has many relations when they are not contained in fetchRelations"
            ) {
                val result =
                    creator.toEntities(
                        flowOf(ResultLine(listOf(99L), listOf())),
                        setOf(),
                        connectionProvider
                    )
                assertEquals(Entity(LazyHasMany(), 99), result.single())
            }
            it("resolves has many relations when they are contained in fetchRelations") {
                val result =
                    creator.toEntities(
                        flowOf(ResultLine(listOf(99L), listOf())),
                        setOf(Entity::referencedEntities),
                        connectionProvider
                    )
                assertEquals(
                    Entity(LazyHasMany(setOf(referencedEntity1, referencedEntity2)), 99),
                    result.single()
                )
            }
        }
    }
}

data class ReferencedEntity(
    val name: String,
    val entity: BelongsTo<Entity>,
    val id: PKType? = null
)

data class Entity(val referencedEntities: HasMany<ReferencedEntity>, val id: PKType? = null)
