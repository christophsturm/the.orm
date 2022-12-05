package io.the.orm.internal

import failgood.Test
import failgood.describe
import failgood.mock.call
import failgood.mock.getCalls
import failgood.mock.mock
import io.the.orm.PK
import io.the.orm.dbio.ConnectionProvider
import io.the.orm.exp.relations.HasMany
import io.the.orm.exp.relations.hasMany
import io.the.orm.internal.classinfo.ClassInfo
import kotlin.test.assertNotNull

@Test
object HasManyInserterTest {
    data class Belonging(val name: String, val id: PK? = null)
    data class Entity(val name: String, val belongings: HasMany<Belonging>, val id: PK? = null)

    val context = describe<HasMany<Entity>> {

        val rootSimpleInserter = mock<Inserter<Entity>>()
        val belongingInserter = mock<Inserter<Belonging>>()
        val subject =
            HasMany(rootSimpleInserter, ClassInfo(Entity::class, setOf(Belonging::class)), listOf(belongingInserter))
        val connection = mock<ConnectionProvider>()
        val belonging = Belonging("belonging 1")
        val entity = Entity("entity-name", hasMany(belonging))
        it("inserts the created object") {
            subject.create(connection, entity)
            val call = assertNotNull(getCalls(rootSimpleInserter).singleOrNull())
            assert(call == call(Inserter<Entity>::create, connection, entity))
        }
        it("inserts the has many relations of the created entity") {
            subject.create(connection, entity)
            val call = assertNotNull(getCalls(belongingInserter).singleOrNull())
            assert(call == call(Inserter<Belonging>::create, connection, belonging))
        }
    }
}

internal class HasMany<Entity : Any>(
    private val rootSimpleInserter: Inserter<Entity>,
    private val classInfo: ClassInfo<Entity>,
    private val belongingsInserters: List<Inserter<*>>
) : Inserter<Entity> {
    override suspend fun create(connection: ConnectionProvider, instance: Entity): Entity {
        rootSimpleInserter.create(connection, instance)
        classInfo.hasManyRelations.forEachIndexed { index, remoteFieldInfo ->
            val inserter = belongingsInserters[index] as Inserter<Any>
            val hasMany = remoteFieldInfo.property.call(instance) as HasMany<*>
            hasMany.forEach { e ->
                inserter.create(connection, e)
            }
        }
        return instance
    }
}
