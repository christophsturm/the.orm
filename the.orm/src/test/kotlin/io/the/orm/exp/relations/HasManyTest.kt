package io.the.orm.exp.relations

import failgood.Ignored
import failgood.Test
import failgood.describe
import io.the.orm.PK
import io.the.orm.Repo
import io.the.orm.exp.testing.MockConnectionProvider

@Test
object HasManyTest {
    data class NestedEntity(val name: String)
    data class HolderOfNestedEntity(val name: String, val nestedEntities: HasMany<NestedEntity>, val id: PK? = null)

    private fun <T : Any> hasMany(list: Set<T>): HasMany<T> {
        return NewHasMany(list)
    }

    val context = describe<HasMany<NestedEntity>> {
        it("can create an entity with nested entities") {
            val holder = HolderOfNestedEntity(
                "name",
                hasMany(setOf(NestedEntity("nested entity 1"), NestedEntity("nested entity 2")))
            )
            val multiRepo = Repo.create<HolderOfNestedEntity>()
            multiRepo.create(MockConnectionProvider(), holder)
        }
    }
}

class NewHasMany<T : Any>(private val list: Set<T>) : HasMany<T>, Set<T> by list
class UnlessEnv(private val envVar: String) : Ignored {
    override fun isIgnored(): String? {
        return if (System.getenv(envVar) == null)
            "Ignored because env var $envVar is not set"
        else null
    }
}
