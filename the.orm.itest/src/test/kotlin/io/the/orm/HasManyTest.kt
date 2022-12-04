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
    data class Sentence(val content: String, val id: PK? = null)
    data class Page(val name: String, val sentences: HasMany<Sentence>, val id: PK? = null)
    data class Book(val name: String, val nestedEntities: HasMany<Page>, val id: PK? = null)

    const val SCHEMA = """
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
    create sequence sentence_seq no maxvalue;
create table sentence
(
    id             bigint       not null default nextval('sentence_seq') primary key,
    page_id      bigint       not null,
    foreign key (page_id) references pages (id),
    content           text not null
);

"""

    val repo = RepoRegistry(listOf(Sentence::class, Page::class, Book::class))
    val context = describeOnAllDbs<HasMany<*>>(schema = SCHEMA) {
        it("can create an page with nested entities") {
            val pages: Set<Page> = setOf(
                Page(
                    "page 1", hasMany(
                        setOf(
                            Sentence("god is dead"),
                            Sentence("No small art is it to sleep: it is necessary for that purpose to keep awake all day.")
                        )
                    )
                ),
                Page(
                    "page 2",
                    hasMany(setOf(Sentence("god is still doing pretty badly"), Sentence("sleeping is still not easy")))
                )
            )
            val holder = Book(
                "Also Sprach Zarathustra",
                hasMany(pages)
            )
            RepoTransactionProvider(repo, it()).transaction(Book::class, Page::class) { bookRepo, pageRepo ->
                bookRepo.create(holder)
                // this is a hack to load all entities. query api really needs to be rethought
                val entities =
                    pageRepo.queryFactory.createQuery(Page::name.isNotNull()).with(pageRepo.connectionProvider, Unit)
                        .find()
                if (System.getenv("NEXT") != null)
                    assert(entities.map { it.name }.containsExactlyInAnyOrder("page 1", "page 2"))
            }
        }
    }
}
