package io.the.orm.test.functional.exp

import failgood.Test
import io.the.orm.exp.ConnectedMultiRepo
import io.the.orm.exp.TransactionalMultiRepo
import io.the.orm.query.isEqualTo
import io.the.orm.test.DBS
import io.the.orm.test.describeOnAllDbs

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
    val context = describeOnAllDbs(ConnectedMultiRepo::class, DBS.databases, SCHEMA, disabled = true) {
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
