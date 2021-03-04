package r2dbcfun.exp

import failfast.FailFast
import failfast.describe
import r2dbcfun.Repository
import r2dbcfun.dbio.ConnectionProvider
import r2dbcfun.dbio.TransactionProvider
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

data class Recipe(val id: Long?, val name: String, val description: String?, val page: Page)
data class RecipeIngredient(val id: Long?, val amount: String, val recipeId: Long, val ingredientId: Long)
data class Ingredient(val id: Long?, val name: String)

fun main() {
    FailFast.runTest()
}

object ConnectedMultiRepoTest {
    val context = describe(ConnectedMultiRepo::class, disabled = true) {
        forAllDatabases(DBS.databases) {
            it("works") {
                val connection = it()
                val findIngredientByName =
                    Repository.create<Ingredient>().queryFactory.createQuery(Ingredient::name.isEqualTo())

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

open class ConnectedMultiRepo internal constructor(
    open val connectionProvider: ConnectionProvider,
    val repos: Map<KClass<out Any>, Repository<out Any>>
) {
    constructor(connectionProvider: ConnectionProvider, classes: List<KClass<out Any>>) : this(
        connectionProvider,
        classes.associateBy({ it }, { Repository(it) })
    )

    @Suppress("UNCHECKED_CAST")
    suspend inline fun <reified T : Any> create(entity: T): T {
        @Suppress("UNCHECKED_CAST")
        val repository = repos[T::class] as Repository<T>
        return repository.create(connectionProvider, entity)
    }


}

class TransactionalMultiRepo(
    override val connectionProvider: TransactionProvider,
    repos: Map<KClass<out Any>, Repository<out Any>>
) : ConnectedMultiRepo(connectionProvider, repos) {
    constructor(connectionProvider: TransactionProvider, classes: List<KClass<out Any>>) : this(
        connectionProvider,
        classes.associateBy({ it }, { Repository(it) })
    )

    suspend fun <R> transaction(function: suspend (ConnectedMultiRepo) -> R): R =
        connectionProvider.transaction { transactionConnectionProvider ->
            function(ConnectedMultiRepo(transactionConnectionProvider, repos))
        }

}
