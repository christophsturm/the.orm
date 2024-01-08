package io.the.orm

import failgood.Test
import failgood.testsAbout
import io.the.orm.relations.BelongsTo
import io.the.orm.relations.HasMany

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

    data class Recipe(
        val id: Long?,
        val name: String,
        val description: String?,
        val page: BelongsTo<Page>
    )

    val context =
        testsAbout(RepoRegistry::class) {
            it("can be created with classes that reference each other") {
                RepoRegistry(setOf(Page::class, Recipe::class))
            }
            it("adds the repo to remote field infos") {
                val registry = RepoRegistry(setOf(Page::class, Recipe::class))
                val classInfo = (registry.getRepo(Page::class) as RepoImpl<Page>).classInfo
                assert(classInfo.entityInfo.hasManyRelations.singleOrNull()?.repo is RepoImpl<*>)
            }
        }
}
