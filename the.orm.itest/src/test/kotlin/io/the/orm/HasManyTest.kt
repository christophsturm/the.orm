package io.the.orm

import failgood.Test
import failgood.assert.containsExactlyInAnyOrder
import io.the.orm.exp.relations.HasMany
import io.the.orm.exp.relations.hasMany
import io.the.orm.query.isNotNull
import io.the.orm.test.describeOnAllDbs
import io.the.orm.transaction.RepoTransactionProvider

@Test
object HasManyTest {
    data class Page(val name: String, val id: PK? = null)
    data class Book(val name: String, val nestedEntities: HasMany<Page>, val id: PK? = null)

    private const val SCHEMA = """
    create sequence book_seq no maxvalue;
create table books
(
    id             bigint       not null default nextval('book_seq') primary key,
    name           varchar(100) not null
);

    create sequence page_seq no maxvalue;
create table pages
(
    id             bigint       not null default nextval('page_seq') primary key,
    book_id      bigint       not null,
    foreign key (book_id) references books (id),
    name           varchar(100) not null
);

"""

    val repo = RepoRegistry(listOf(Page::class, Book::class))
    val context = describeOnAllDbs<HasMany<Page>>(schema = SCHEMA) {
        it("can create an page with nested entities") {
            val holder = Book(
                "name",
                hasMany(setOf(Page("page 1"), Page("page 2")))
            )
            RepoTransactionProvider(repo, it()).transaction(Book::class, Page::class) { bookRepo, pageRepo ->
                bookRepo.create(holder)
                // this is a hack to load all entities. query api really needs a rethought
                val entities =
                    pageRepo.queryFactory.createQuery(Page::name.isNotNull()).with(pageRepo.connectionProvider, Unit)
                        .find()
                if (System.getenv("NEXT") != null)
                    assert(entities.map { it.name }.containsExactlyInAnyOrder("page 1", "page 2"))
            }
        }
    }
}
