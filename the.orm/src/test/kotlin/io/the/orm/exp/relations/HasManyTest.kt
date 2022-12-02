package io.the.orm.exp.relations

import failgood.Ignored
import failgood.Test
import failgood.describe
import io.the.orm.MultiRepo
import io.the.orm.PK
import io.the.orm.exp.testing.MockConnectionProvider

@Test
object HasManyTest {
    data class NestedEntity(val name: String)
    data class HolderOfNestedEntity(
        val name: String,
        val id: PK? = null
    ) {
        companion object {
            val nestedEntity = HasMany<NestedEntity, HolderOfNestedEntity>()
        }
    }

    val context = describe<HasMany<NestedEntity, HolderOfNestedEntity>> {
        it("can create an entity with nested entities") {
            val holder = HolderOfNestedEntity(
                "name"
            )
            val multiRepo = MultiRepo(HolderOfNestedEntity::class)
            multiRepo.create(MockConnectionProvider(), holder)
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
