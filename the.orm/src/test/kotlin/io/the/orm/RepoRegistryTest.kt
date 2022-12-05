package io.the.orm

import failgood.Ignored
import failgood.Test
import failgood.describe
import io.the.orm.exp.relations.HasMany

@Test
class RepoRegistryTest {
    data class Page(
        val id: Long?,
        val url: String,
        val title: String?,
        val description: String?,
        val ldJson: String?,
        val author: String?,
        val recipes: HasMany<Recipe>
    )

    data class Recipe(val id: Long?, val name: String, val description: String?, val page: Page)

    val context = describe<RepoRegistry> {
        it("can be created with classes that reference each other") {
            RepoRegistry(setOf(Page::class, Recipe::class))
        }
    }
}
