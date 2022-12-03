package io.the.orm.exp

import failgood.Test
import failgood.describe
import io.the.orm.Repo
import io.the.orm.RepoImpl

object CompanionExp {
    val context = describe("companion object based api") {

    }
    data class Entity(val name: String) {
        companion object {
            val repo: Repo<Entity> = Repo.create()
        }
    }
}


