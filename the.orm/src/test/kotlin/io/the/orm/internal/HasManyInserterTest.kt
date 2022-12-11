package io.the.orm.internal

import failgood.Test
import failgood.describe
import failgood.mock.call
import failgood.mock.getCalls
import failgood.mock.mock
import io.the.orm.PK
import io.the.orm.Repo
import io.the.orm.dbio.ConnectionProvider
import io.the.orm.exp.relations.BelongsTo
import io.the.orm.exp.relations.HasMany
import io.the.orm.exp.relations.belongsTo
import io.the.orm.exp.relations.hasMany
import io.the.orm.internal.classinfo.ClassInfo
import kotlin.test.assertEquals

@Test
object HasManyInserterTest {
    data class Belonging(val name: String, val entity: BelongsTo<Entity> = belongsTo(), val id: PK? = null)
    data class Entity(val name: String, val belongings: HasMany<Belonging>, val id: PK? = null)

    val context = describe<HasManyInserter<Entity>> {
        val connection = mock<ConnectionProvider>()
        val belonging = Belonging("belonging 1")
        val entity = Entity("entity-name", hasMany(belonging))
        val entityWithId = entity.copy(id = 42)

        val rootSimpleInserter = mock<Inserter<Entity>> {
            method { create(connection, entity) }.returns(entityWithId)
        }
        val belongingRepo = mock<Repo<Belonging>>()
        val subject =
            HasManyInserter(
                rootSimpleInserter,
                ClassInfo(Entity::class, setOf(Belonging::class)),
                listOf(belongingRepo),
                listOf(ClassInfo(Belonging::class, setOf(Entity::class)).belongsToRelations.single())

            )
        it("inserts the created object") {
            assert(subject.create(connection, entity) == entityWithId)
            assert(getCalls(rootSimpleInserter).singleOrNull() == call(Inserter<Entity>::create, connection, entity))
        }
        it("inserts the has many relations of the created entity") {
            subject.create(connection, entity)
            val belongingWithId = belonging.copy(entity = BelongsTo.Auto<Entity>().apply { id = 42 })
            assertEquals(
                getCalls(belongingRepo).singleOrNull(),
                call(Inserter<Belonging>::create, connection, belongingWithId)
            )
        }
    }
}
