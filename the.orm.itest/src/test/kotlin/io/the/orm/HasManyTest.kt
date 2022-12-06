package io.the.orm

import failgood.Test
import io.the.orm.exp.relations.BelongsTo
import io.the.orm.exp.relations.HasMany
import io.the.orm.exp.relations.belongsTo
import io.the.orm.exp.relations.hasMany
import io.the.orm.test.describeOnAllDbs
import io.the.orm.transaction.RepoTransactionProvider
import kotlinx.coroutines.flow.toSet
import kotlin.test.assertEquals

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
            // the whole hierarchy is created outside the transaction and needs no access to a repo
            val holder = book()
            RepoTransactionProvider(repo, it()).transaction(Book::class) { bookRepo ->
                bookRepo.create(holder)
                val result = bookRepo.connectionProvider.withConnection { conn ->
                    conn.createStatement("select content from SENTENCES").execute()
                        .map { it.get("content", String::class.java)!! }.toSet()
                }
                assertEquals(
                    result, setOf(
                        "god is dead", "No small art is it to sleep:" +
                            " it is necessary for that purpose to keep awake all day.",
                        "god is still doing pretty badly", "sleeping is still not easy"

                    )
                )
            }
        }
        it("can load has many") {
            // the whole hierarchy is created outside the transaction and needs no access to a repo
            val holder = book()
            RepoTransactionProvider(repo, it()).transaction(Book::class) { bookRepo ->
                val id = bookRepo.create(holder).id!!
                val reloaded = bookRepo.findById(id)
                assert(reloaded.chapters.map { it.book }
                    == listOf("Also Sprach Zarathustra", "Also Sprach Zarathustra"))
            }
        }
    }

    private fun book(): Book {
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
        return Book("Also Sprach Zarathustra", hasMany(chapters))
    }
}
