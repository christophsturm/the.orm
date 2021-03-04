package r2dbcfun.exp

import failfast.FailFast
import failfast.describe
import r2dbcfun.Repository
import r2dbcfun.dbio.ConnectionProvider
import r2dbcfun.query.isEqualTo
import r2dbcfun.test.DBS
import r2dbcfun.test.forAllDatabases
import kotlin.reflect.KClass

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
data class Ingredient(val id: Long?, val name: String)

fun main() {
    FailFast.runTest()
}

object ConnectedMultiRepoTest {
    val context = describe(ConnectedMultiRepo::class) {
        forAllDatabases(DBS.databases) {
            it("works") {
                val connection = it()
                val findIngredientByName =
                    Repository.create<Ingredient>().queryFactory.createQuery(Ingredient::name.isEqualTo())

                ConnectedMultiRepo(
                    connection,
                    listOf(Page::class, Recipe::class, RecipeIngredient::class, Ingredient::class)
                ).transaction { repo ->
                    val page = repo.create(Page(null, "url", "pageTitle", "description", "{}", "author"))
                    val recipe =
                        repo.create(Recipe(null, "Spaghetti Carbonara", "Wasser Salzen, Speck dazu, fertig", page.id!!))
                    val gurke = repo.create(Ingredient(null, "Gurke"))
                    val ingredient = repo.create(RecipeIngredient(null, "100g", recipe.id!!, gurke.id!!))
                }


            }
        }
    }
}

class ConnectedMultiRepo private constructor(
    val connectionProvider: ConnectionProvider,
    val repos: Map<KClass<out Any>, Repository<out Any>>
) {
    constructor(connectionProvider: ConnectionProvider, classes: List<KClass<out Any>>) : this(
        connectionProvider,
        classes.associateBy({ it }, { Repository(it) })
    )

    suspend inline fun <reified T : Any> create(entity: T): T {
        val repository = repos.get(T::class) as Repository<T>
        return repository.create(connectionProvider, entity)
    }

    suspend fun <R> transaction(function: suspend (ConnectedMultiRepo) -> R): R =
        connectionProvider.transaction { transactionConnectionProvider ->
            function(ConnectedMultiRepo(transactionConnectionProvider, repos))
        }


}
