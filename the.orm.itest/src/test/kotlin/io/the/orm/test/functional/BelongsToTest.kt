package io.the.orm.test.functional

import failgood.describe

// currently not used, see ConnectedMultiRepoFunctionalTest
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

    val tests = describe("belongs to support") {
    }
}
