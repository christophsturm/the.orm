package io.the.orm.internal

import failgood.Test
import failgood.mock.call
import failgood.mock.getCalls
import failgood.mock.mock
import failgood.tests
import io.the.orm.PKType
import io.the.orm.Repo
import io.the.orm.dbio.ConnectionProvider
import io.the.orm.internal.classinfo.ClassInfo
import io.the.orm.relations.BelongsTo
import io.the.orm.relations.HasMany
import io.the.orm.relations.belongsTo
import io.the.orm.relations.hasMany
import kotlin.test.assertEquals

@Test
object HasManyInserterTest {
    data class Belonging(
        val name: String,
        val entity: BelongsTo<Entity> = belongsTo(),
        val id: PKType? = null
    )

    data class Entity(val name: String, val belongings: HasMany<Belonging>, val id: PKType? = null)

    val context = tests {
        val connection = mock<ConnectionProvider>()
        val belonging = Belonging("belonging 1")
        val e = Entity("entity-name", hasMany(belonging))
        val entity = EntityWrapper(e)
        val entityWithId = EntityWrapper(e.copy(id = 42))

        val rootSimpleInserter =
            mock<Inserter<Entity>> { method { create(connection, entity) }.returns(entityWithId) }
        val belongingRepo = mock<Repo<Belonging>>()
        val entityClassInfo = ClassInfo(Entity::class, setOf(Belonging::class))
        val belongingClassInfo = ClassInfo(Belonging::class, setOf(Entity::class))
        entityClassInfo.hasManyRelations.single().setRepo(Entity::class, belongingRepo, belongingClassInfo)
        val subject =
            HasManyInserter(
                rootSimpleInserter,
                listOf(
                    belongingClassInfo.belongsToRelations.single()
                ),
                entityClassInfo.hasManyRelations,
                entityClassInfo.idFieldOrThrow()
            )
        it("inserts the created object") {
            assert(subject.create(connection, entity) == entityWithId)
            assert(
                getCalls(rootSimpleInserter).singleOrNull() ==
                    call(Inserter<Entity>::create, connection, entity)
            )
        }
        it("inserts the has many relations of the created entity") {
            subject.create(connection, entity)
            val belongingWithId =
                belonging.copy(entity = BelongsTo.AutoGetFromHasMany<Entity>().apply { id = 42 })
            assertEquals(
                call(Repo<Belonging>::create, connection, belongingWithId),
                getCalls(belongingRepo).singleOrNull()
            )
        }
    }
}
