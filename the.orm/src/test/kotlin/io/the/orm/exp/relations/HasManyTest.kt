package io.the.orm.exp.relations

import failgood.Ignored
import failgood.Test
import failgood.describe
import io.the.orm.Repo
import io.the.orm.exp.testing.MockConnectionProvider

@Test
class HasManyTest {
    data class NestedEntity(val name: String)
    data class HolderOfNestedEntity(val name: String, val nestedEntities: HasMany<NestedEntity>)

    private fun <T : Any> hasMany(list: List<T>): HasMany<T> {
        return TODO()
    }

    val context = describe<HasMany<NestedEntity>>(ignored = UnlessEnv("NEXT")) {
        it("can create an entity with nested entities") {
            val holder = HolderOfNestedEntity(
                "name",
                hasMany(listOf(NestedEntity("nested entity 1"), NestedEntity("nested entity 2")))
            )
            val repo = Repo(HolderOfNestedEntity::class)
            repo.create(MockConnectionProvider(), holder)
        }
    }
}
class UnlessEnv(private val envVar: String) : Ignored {
    override fun isIgnored(): String? {
        return if (System.getenv(envVar) == null)
            "Ignored because env var $envVar is not set"
        else null
    }
}
