package io.the.orm.test.functional.exp

import failgood.Ignored
import failgood.Test
import io.the.orm.Repository
import io.the.orm.exp.ConnectedMultiRepo
import io.the.orm.exp.TransactionalMultiRepo
import io.the.orm.exp.relations.HasMany
import io.the.orm.query.isEqualTo
import io.the.orm.test.DBS
import io.the.orm.test.describeOnAllDbs
import kotlin.test.assertNotNull

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

@Test
object ConnectedMultiRepoFunctionalTest {
    data class Page(
        val url: String,
        val title: String?,
        val description: String?,
        val ldJson: String?,
        val author: String?,
        val id: Long? = null
    )

    data class Recipe(
        val name: String,
        val description: String?,
        val page: Page,
        val ingredients: HasMany<RecipeIngredient>? = null,
        val id: Long? = null
    )

    data class RecipeIngredient(
        val amount: String,
        val recipe: Recipe,
        val ingredient: Ingredient,
        val id: Long? = null
    )

    data class Ingredient(val name: String, val id: Long? = null)

    val context =
        describeOnAllDbs(ConnectedMultiRepo::class, DBS.databases, SCHEMA, ignored = ExceptEnv("NEXT")) {
            val connection = it()
            val transactionalMultiRepo = TransactionalMultiRepo(
                connection,
                listOf(Page::class, Recipe::class, RecipeIngredient::class, Ingredient::class)
            )
            it("can write Entities that have BelongsTo relations") {
                transactionalMultiRepo.transaction { repo ->
                    val page = repo.create(Page("url", "pageTitle", "description", "{}", "author"))
                    repo.create(
                        Recipe(
                            "Spaghetti Carbonara",
                            "Wasser Salzen, Speck dazu, fertig",
                            page
                        )
                    )
                }
            }
            it("can write and query") {
                val findIngredientByName =
                    Repository.create<Ingredient>().queryFactory.createQuery(Ingredient::name.isEqualTo())

//                val findPageByUrl = repo.repository.queryFactory.createQuery(Page::url.isEqualTo())
                transactionalMultiRepo.transaction { repo ->
                    // recipe hasMany RecipeIngredient(s)
                    // recipe hasMany Ingredients through RecipeIngredients
                    val page = repo.create(Page("url", "pageTitle", "description", "{}", "author"))
                    val recipe =
                        repo.create(
                            Recipe(
                                "Spaghetti Carbonara",
                                "Wasser Salzen, Speck dazu, fertig",
                                page
                            )
                        )
                    val gurke = findIngredientByName.with(repo.connectionProvider, "gurke")
                        .findOrCreate { Ingredient("Gurke") }
                    val createdIngredient = repo.create(RecipeIngredient("100g", recipe, gurke))
                    val reloadedIngredient = repo.findById<RecipeIngredient>(createdIngredient.id!!)
                    val recipeIngredient = repo.create(RecipeIngredient("2", recipe, gurke))
                    assert(createdIngredient == reloadedIngredient)
                    val reloadedRecipe = repo.findById<Recipe>(recipe.id!!)
                    with(assertNotNull(reloadedRecipe.ingredients)) {
                        assert(contains(recipeIngredient))
                    }
                }
            }
        }
}

class ExceptEnv(val s: String) : Ignored {
    override fun isIgnored(): String? {
        return if (System.getenv(s) == null)
            "Ignored because env var $s is not set"
        else null
    }

}
