package io.the.orm

import failgood.describe
import io.the.orm.exp.relations.BelongsTo
import io.the.orm.test.describeOnAllDbs

object BelongsToTest {
    data class Page(
        val id: Long?,
        val url: String,
        val title: String?,
        val description: String?,
        val ldJson: String?,
        val author: String?
    )

    data class Recipe(val id: Long?, val name: String, val description: String?, val page: Page)

    val repo = RepoRegistry(listOf(HasManyTest.Page::class, HasManyTest.Book::class))

    val tests = describeOnAllDbs<BelongsTo<*>>(schema = HasManyTest.SCHEMA) {

    }
}
