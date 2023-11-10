@file:Suppress("unused")

package io.the.orm.exp

import failgood.describe
import io.the.orm.Repo

object CompanionExperiment {
    val context = describe("companion object based api") {
    }
    data class Entity(val name: String) {
        companion object {
            val repo: Repo<Entity> = Repo.create()
        }
    }
}
