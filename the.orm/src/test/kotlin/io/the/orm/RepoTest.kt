package io.the.orm

import failgood.Test
import failgood.describe
import io.the.orm.exp.relations.HasMany

@Test
class RepoTest {
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

    val context = describe<Repo> {
        it("can be created with classes that reference each other") {
            Repo(listOf(Page::class, Recipe::class))
        }
        it("can return a query factory for a class") {
            Repo(listOf(Page::class, Recipe::class)).queryFactory<Page>()
        }
    }
}
