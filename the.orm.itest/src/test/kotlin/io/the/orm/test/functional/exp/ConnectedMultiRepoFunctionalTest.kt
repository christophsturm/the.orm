package io.the.orm.test.functional.exp

import failgood.Test
import io.the.orm.exp.ConnectedMultiRepo
import io.the.orm.exp.TransactionalMultiRepo
import io.the.orm.query.isEqualTo
import io.the.orm.test.DBS
import io.the.orm.test.describeOnAllDbs

data class Page(
    val id: Long?,
    val url: String,
    val title: String?,
    val description: String?,
    val ldJson: String?,
    val author: String?
)
data class Recipe(val id: Long?, val name: String, val description: String?, val page: Page)
data class RecipeIngredient(val id: Long?, val amount: String, val recipeId: Long, val ingredientId: Long)
data class Ingredient(val id: Long?, val name: String)

@Test
class ConnectedMultiRepoFunctionalTest {
    val context = describeOnAllDbs(ConnectedMultiRepo::class, DBS.databases, disabled = true) {
        it("works") {
            val connection = it()
            val findIngredientByName =
                io.the.orm.Repository.create<Ingredient>().queryFactory.createQuery(Ingredient::name.isEqualTo())

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

