package io.the.orm

import failfast.FailFast
import failfast.describe
import io.the.orm.exp.MultiRepo

fun main() {
    FailFast.runTest()
}

data class Page(
    val id: Long?,
    val url: String,
    val title: String?,
    val description: String?,
    val ldJson: String?,
    val author: String?
)

data class Recipe(val id: Long?, val name: String, val description: String?, val page: Page)

object MultiRepoTest {
    val context = describe(MultiRepo::class) {
        it("can be created with classes that reference each other") {
            MultiRepo(listOf(Page::class, Recipe::class))
        }
        it("can return a query factory for a class") {
            MultiRepo(listOf(Page::class, Recipe::class)).queryFactory<Page>()
        }
    }
}
