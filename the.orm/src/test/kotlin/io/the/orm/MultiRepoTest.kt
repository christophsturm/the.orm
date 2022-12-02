package io.the.orm

import failgood.Test
import failgood.describe
import io.the.orm.exp.relations.HasMany

@Test
class MultiRepoTest {
    data class Page(
        val id: Long?,
        val url: String,
        val title: String?,
        val description: String?,
        val ldJson: String?,
        val author: String?
    ) {
        companion object {
            val recipes = HasMany<Recipe, Page>()
        }
    }

    data class Recipe(val id: Long?, val name: String, val description: String?, val page: Page)

    val context = describe<MultiRepo> {
        it("can be created with classes that reference each other") {
            MultiRepo(listOf(Page::class, Recipe::class))
        }
        it("can return a query factory for a class") {
            MultiRepo(listOf(Page::class, Recipe::class)).queryFactory<Page>()
        }
    }
}
