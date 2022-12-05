package io.the.orm

import failgood.Test
import failgood.assert.containsExactlyInAnyOrder
import io.the.orm.exp.relations.BelongsTo
import io.the.orm.exp.relations.HasMany
import io.the.orm.exp.relations.belongsTo
import io.the.orm.exp.relations.hasMany
import io.the.orm.query.isNotNull
import io.the.orm.test.describeOnAllDbs
import io.the.orm.transaction.RepoTransactionProvider

@Test
object HasManyTest {
    data class Sentence(val content: String, val chapter: BelongsTo<Chapter> = belongsTo(), val id: PK? = null)
    data class Chapter(
        val name: String,
        val sentences: HasMany<Sentence>,
        val book: BelongsTo<Book> = belongsTo(),
        val id: PK? = null
    )
    data class Book(val name: String, val chapters: HasMany<Chapter>, val id: PK? = null)

    const val SCHEMA = """
    create sequence book_seq no maxvalue;
create table books
(
    id             bigint       not null default nextval('book_seq') primary key,
    name           varchar(100) not null
);

    create sequence chapter_seq no maxvalue;
create table chapters
(
    id             bigint       not null default nextval('chapter_seq') primary key,
    book_id      bigint       not null,
    foreign key (book_id) references books (id),
    name           varchar(100) not null
);
    create sequence sentence_seq no maxvalue;
create table sentences
(
    id             bigint       not null default nextval('sentence_seq') primary key,
    chapter_id      bigint       not null,
    foreign key (chapter_id) references chapters (id),
    content           text not null
);

"""
/*
for has many we really need recursive saving because we don't know the id when we create the objects
 */

    val context = describeOnAllDbs<HasMany<*>>(schema = SCHEMA) {
        val repo = RepoRegistry(setOf(Sentence::class, Chapter::class, Book::class))
        it("can create an page with nested entities") {
            val chapters: Set<Chapter> = setOf(
                Chapter(
                    "page 1", hasMany(
                        setOf(
                            Sentence("god is dead"),
                            Sentence(
                                "No small art is it to sleep:" +
                                    " it is necessary for that purpose to keep awake all day."
                            )
                        )
                    )
                ),
                Chapter(
                    "page 2",
                    hasMany(
                        setOf(
                            Sentence("god is still doing pretty badly"),
                            Sentence("sleeping is still not easy")
                        )
                    )
                )
            )
            val holder = Book(
                "Also Sprach Zarathustra",
                hasMany(chapters)
            )
            RepoTransactionProvider(repo, it()).transaction(Book::class, Chapter::class) { bookRepo, pageRepo ->
                bookRepo.create(holder)
                // this is a hack to load all entities. query api really needs to be rethought
                val entities =
                    pageRepo.queryFactory.createQuery(Chapter::name.isNotNull()).with(pageRepo.connectionProvider, Unit)
                        .find()
                if (System.getenv("NEXT") != null)
                    assert(entities.map { it.name }.containsExactlyInAnyOrder("page 1", "page 2"))
            }
        }
    }
}
