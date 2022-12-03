package io.the.orm.exp.relations

import failgood.Test
import failgood.describe
import io.the.orm.PK
import io.the.orm.Repo
import io.the.orm.exp.testing.MockConnectionProvider

@Test
object HasManyTest {
    data class Entity(val name: String, val id: PK? = null)
    data class HolderOfEntity(val name: String, val nestedEntities: HasMany<Entity>, val id: PK? = null)

    val context = describe<HasMany<Entity>> {
        it("can create an entity with nested entities") {
            val holder = HolderOfEntity(
                "name",
                hasMany(setOf(Entity("nested entity 1"), Entity("nested entity 2")))
            )
            val repo = Repo.create<HolderOfEntity>()
            repo.create(MockConnectionProvider(), holder)
        }
    }
}
