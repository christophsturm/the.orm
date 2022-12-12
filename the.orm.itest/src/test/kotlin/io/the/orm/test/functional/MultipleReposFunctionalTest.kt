package io.the.orm.test.functional

import failgood.Test
import io.the.orm.PK
import io.the.orm.RepoRegistry
import io.the.orm.getRepo
import io.the.orm.query.isEqualTo
import io.the.orm.relations.BelongsTo
import io.the.orm.relations.HasMany
import io.the.orm.relations.belongsTo
import io.the.orm.relations.hasMany
import io.the.orm.test.DBS
import io.the.orm.test.describeOnAllDbs
import io.the.orm.transaction.RepoTransactionProvider

@Test
object MultipleReposFunctionalTest {
    private const val SCHEMA = """
    create sequence pages_id_seq no maxvalue;
    create table pages
    (
        id          bigint        not null default nextval('pages_id_seq'),
        url         varchar(4096) not null unique,
        title       varchar(2048),
        description text,
        ld_json     text,
        author      varchar(2048)
    );

    alter table pages
        add primary key (id);

    create sequence ingredients_id_seq no maxvalue;
    create table INGREDIENTS
    (
        ID   BIGINT default NEXTVAL('ingredients_id_seq') not null primary key,
        NAME VARCHAR(1024) unique
    );

    create sequence recipes_id_seq no maxvalue;
    create table RECIPES
    (
        ID          BIGINT default NEXTVAL('recipes_id_seq') not null primary key,
        PAGE_ID     bigint                                   not null,
        NAME        VARCHAR(4096)                            NOT NULL,
        DESCRIPTION text,
        CONSTRAINT FK_RECIPES_PAGES FOREIGN KEY (PAGE_ID) REFERENCES pages
    );

    create sequence recipe_ingredients_id_seq no maxvalue;
    create table recipe_ingredients
    (
        ID            BIGINT default NEXTVAL('recipe_ingredients_id_seq') not null primary key,
        RECIPE_ID     BIGINT                                              NOT NULL,
        INGREDIENT_ID BIGINT                                              NOT NULL,
        AMOUNT        VARCHAR(100)                                        NOT NULL,
        CONSTRAINT FK_RECIPE_INGREDIENTS_RECIPES FOREIGN KEY (RECIPE_ID) REFERENCES recipes,
        CONSTRAINT FK_RECIPE_INGREDIENTS_INGREDIENTS FOREIGN KEY (INGREDIENT_ID) REFERENCES INGREDIENTS
    )

"""

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

    val context =
        // testing support to run the tests on all supported databases
        describeOnAllDbs(RepoTransactionProvider::class, DBS.databases, SCHEMA) {
            val transactionProvider = it()

            // the RepoRegistry is created at startup and lists all entity classes
            val repoRegistry = RepoRegistry(
                setOf(Page::class, Recipe::class, RecipeIngredient::class, Ingredient::class)
            )
            val repoTransactionProvider = RepoTransactionProvider(repoRegistry, transactionProvider)
            it("can write and query") {
                // this is how to create a query
                val findIngredientByName =
                    repoRegistry.getRepo<Ingredient>().queryFactory.createQuery(Ingredient::name.isEqualTo())

                // here we start a transaction that involves Page and Recipe
                repoTransactionProvider.transaction(Page::class, Recipe::class) { pageRepo, recipeRepo ->
                    val page = pageRepo
                        .create(
                            Page("url", "pageTitle", "description", "{}", "author")
                        )
                    // create or
                    val ingredients = setOf(RecipeIngredient("1", findIngredientByName.with("Gurke")
                        .findOrCreate(pageRepo.connectionProvider) { Ingredient("Gurke") }
                    ), RecipeIngredient("100g", findIngredientByName.with("Butter")
                            .findOrCreate(pageRepo.connectionProvider) { Ingredient("Butter") }
                        ))
                    val recipe =
                        recipeRepo.create(
                            Recipe(
                                "Spaghetti Carbonara",
                                "Wasser Salzen, Speck dazu, fertig",
                                belongsTo(page),
                                ingredients = hasMany(ingredients)
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
        }
}
