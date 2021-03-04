package r2dbcfun.exp

import failfast.describe

data class Page(
    val id: Long?,
    val url: String,
    val title: String?,
    val description: String?,
    val ldJson: String?,
    val author: String?
)

data class Recipe(val id: Long?, val name: String, val description: String?, val pageId: Long)

data class RecipeIngredient(val id: Long?, val amount: String, val recipeId: Long, val ingredientId: Long)

object MultiRepoTest {
    val context = describe(MultiRepo::class) {
    }
}

class MultiRepo {

}
