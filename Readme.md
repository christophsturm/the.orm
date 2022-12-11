# the.orm

A non-blocking ORM library for Kotlin that supports H2SQL and PostgreSQL via Vert.x and R2DBC

# In a nutshell:
* Entities are normal data classes
* No Annotations
* Supports immutable entities
* HasMany and BelongsTo Relations
* Really fast testing support. Its own tests suite runs in 10 seconds against 3 different databases (h2-r2dbc, h2-psql, vertx-psql)

## Is this for me?

the.orm is very opinionated and not for you if you
need to support a legacy database structure or if you really want to be super creative with your sql schema.
its more like rails' active-record and less like sql-alchemy.
A lot is currently missing but a lot is already working so feel free to try it out.

docs are currently also non-existant but take a look at [MultipleReposFunctionalTest](the.orm.itest/src/test/kotlin/io/the/orm/test/functional/MultipleReposFunctionalTest.kt)
and the other tests in the itest module to see how to use it.

here's the interesting pieces from that test:
```kotlin
data class Page(
    val url: String,
    val title: String?,
    val description: String?,
    val ldJson: String?,
    val author: String?,
    val recipes: HasMany<Recipe> = hasMany(),
    val id: PK? = null
)

data class Recipe(
    val name: String,
    val description: String?,
    val page: BelongsTo<Page> = belongsTo(),
    val ingredients: HasMany<RecipeIngredient> = hasMany(),
    val id: PK? = null
)

data class RecipeIngredient(
    val amount: String,
    val ingredient: Ingredient,
    val recipe: BelongsTo<Recipe> = belongsTo(),
    val id: PK? = null
)

data class Ingredient(val name: String, val id: Long? = null)

// list all entity classes
val repoRegistry = RepoRegistry(
    setOf(Page::class, Recipe::class, RecipeIngredient::class, Ingredient::class)
)
val repoTransactionProvider = RepoTransactionProvider(repoRegistry, transactionProvider)
it("can write and query") {
    // this is how to create a query
    val findIngredientByName =
        repoRegistry.getRepo<Ingredient>().queryFactory.createQuery(Ingredient::name.isEqualTo())

    // here we start a transaction that involves Page and Recipe
    repoTransactionProvider.transaction(
        Page::class,
        Recipe::class
    ) { pageRepo, recipeRepo ->
        val page = pageRepo
            .create(
                Page("url", "pageTitle", "description", "{}", "author")
            )
        val recipe =
            recipeRepo.create(
                Recipe(
                    "Spaghetti Carbonara",
                    "Wasser Salzen, Speck dazu, fertig",
                    belongsTo(page),
                    ingredients = hasMany(setOf(RecipeIngredient("1", findIngredientByName.with("Gurke")
                        .findOrCreate(pageRepo.connectionProvider) { Ingredient("Gurke") }
                    ), RecipeIngredient("100g", findIngredientByName.with("Butter")
                            .findOrCreate(pageRepo.connectionProvider) { Ingredient("Butter") }
                        )))
                )
            )

        // fetchRelations indicates what relations should be loaded. relations are never loaded lazy
        val reloadedRecipe =
            recipeRepo.findById(recipe.id!!, fetchRelations = setOf(Recipe::ingredients, Recipe::page))

        assert(reloadedRecipe.ingredients.map { it.amount + " " + it.ingredient.name }
            == listOf("1 Gurke", "100g Butter"))
        assert(reloadedRecipe.page.get().url == "url")
    }
}

```

Planned but missing:
* schema migration support (you can use flyway in the meantime)
* UUID PK support. (currently only long is supported)
* query support clearly needs a big api change
