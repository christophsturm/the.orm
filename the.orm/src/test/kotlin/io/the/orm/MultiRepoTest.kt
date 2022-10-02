package io.the.orm

import failgood.Test
import failgood.describe
import io.the.orm.exp.MultiRepo
import io.the.orm.exp.relations.BelongsTo
import io.the.orm.exp.relations.HasMany

@Test
class MultiRepoTest {
    data class Page(
        val id: Long?,
        val url: String,
        val title: String?,
        val description: String?,
        val ldJson: String?,
        val author: String?,
        val recipes: HasMany<Recipe>
    )

    data class Recipe(val id: Long?, val name: String, val description: String?, val page: BelongsTo<Page>)

    val context = describe<MultiRepo>(disabled = System.getenv("NEXT") == null) {
        it("can be created with classes that reference each other") {
            MultiRepo(listOf(Page::class, Recipe::class))
        }
        it("can return a query factory for a class") {
            MultiRepo(listOf(Page::class, Recipe::class)).queryFactory<Page>()
        }
    }
}
