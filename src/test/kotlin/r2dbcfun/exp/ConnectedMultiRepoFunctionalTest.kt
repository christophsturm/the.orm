package r2dbcfun.exp

import failfast.FailFast
import failfast.describe
import r2dbcfun.Repository
import r2dbcfun.query.isEqualTo
import r2dbcfun.test.DBS
import r2dbcfun.test.forAllDatabases
import kotlin.reflect.KProperty1

data class Page(
    val id: Long?,
    val url: String,
    val title: String?,
    val description: String?,
    val ldJson: String?,
    val author: String?
) {
    companion object : ARecord() {
        fun findPageByUrl(url: String) = findBy(Page::url, url)
    }
}

open class ARecord {
    fun <Entity, Field> findBy(kProperty1: KProperty1<Entity, Field>, url: Field): Entity {
        throw NotImplementedError("stub")
    }

}


data class Recipe(val id: Long?, val name: String, val description: String?, val page: Page)
data class RecipeIngredient(val id: Long?, val amount: String, val recipeId: Long, val ingredientId: Long)
data class Ingredient(val id: Long?, val name: String)

fun main() {
    FailFast.runTest()
}

object ConnectedMultiRepoFunctionalTest {
    val context = describe(ConnectedMultiRepo::class, disabled = true) {
        forAllDatabases(DBS.databases) {
            it("works") {
                val connection = it()
                val findIngredientByName =
                    Repository.create<Ingredient>().queryFactory.createQuery(Ingredient::name.isEqualTo())

//                val findPageByUrl = repo.repository.queryFactory.createQuery(Page::url.isEqualTo())
                TransactionalMultiRepo(
                    connection,
                    listOf(Page::class, Recipe::class, RecipeIngredient::class, Ingredient::class)
                ).transaction { repo ->
                    // Recipe belongsTo Page
                    // recipe hasMany RecipeIngredient(s)
                    // recipe hasMany Ingredients through RecipeIngredients
                    val page = repo.create(Page(null, "url", "pageTitle", "description", "{}", "author"))
                    val recipe =
                        repo.create(Recipe(null, "Spaghetti Carbonara", "Wasser Salzen, Speck dazu, fertig", page))
                    val gurke = findIngredientByName.with(repo.connectionProvider, "gurke")
                        .findOrCreate { Ingredient(null, "Gurke") }
                    repo.create(RecipeIngredient(null, "100g", recipe.id!!, gurke.id!!))
                }


            }
        }
    }
}

