package io.the.orm

import failgood.Test
import io.the.orm.relations.BelongsTo
import io.the.orm.relations.HasMany
import io.the.orm.relations.LazyHasMany
import io.the.orm.relations.belongsTo
import io.the.orm.relations.hasMany
import io.the.orm.test.describeOnAllDbs
import io.the.orm.transaction.RepoTransactionProvider
import kotlinx.coroutines.flow.toSet
import kotlin.test.assertEquals

@Test
object HasManyTest {
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
    data class Sentence(val content: String, val chapter: BelongsTo<Chapter> = belongsTo(), val id: PK? = null)
    data class Chapter(
        val name: String,
        val sentences: HasMany<Sentence>,
        val book: BelongsTo<Book> = belongsTo(),
        val id: PK? = null
    )

    data class Book(val name: String, val chapters: HasMany<Chapter> = hasMany(), val id: PK? = null)

    /*
    for has many we really need recursive saving because we don't know the id when we create the objects
     */
    val context = describeOnAllDbs<HasMany<*>>(schema = SCHEMA) {
        val repo = RepoRegistry(setOf(Sentence::class, Chapter::class, Book::class))
        val transactionProvider = it()
        it("can write and read an entity with an empty has many relation") {
            RepoTransactionProvider(repo, transactionProvider).transaction(Book::class) { bookRepo ->
                val book = bookRepo.create(Book("a book without chapters"))
                bookRepo.findById(book.id!!)
            }
        }
        it("can create an entity with nested has many relations") {
            // the whole hierarchy is created outside the transaction and needs no access to a repo
            val book = book()
            RepoTransactionProvider(repo, transactionProvider).transaction(Book::class) { bookRepo ->
                bookRepo.create(book)
                val result = bookRepo.connectionProvider.withConnection { conn ->
                    conn.createStatement("select content from SENTENCES").execute()
                        .map { it.get("content", String::class.java)!! }.toSet()
                }
                assertEquals(
                    result, setOf(
                        "god is dead",
                        "No small art is it to sleep:" + " it is necessary for that purpose to keep awake all day.",
                        "god is still doing pretty badly",
                        "sleeping is still not easy"
                    )
                )
            }
        }
        it("can load has many") {
            val holder = book()
            RepoTransactionProvider(repo, transactionProvider).transaction(Book::class) { bookRepo ->
                val id = bookRepo.create(holder).id!!
                val reloaded = bookRepo.findById(id, fetchRelations = setOf(Book::chapters, Chapter::sentences))
                assertEquals(setOf(
                    "god is dead",
                    "No small art is it to sleep:" + " it is necessary for that purpose to keep awake all day.",
                    "god is still doing pretty badly",
                    "sleeping is still not easy"
                ), reloaded.chapters.flatMap { it.sentences.map { it.content } }.toSet())
            }
        }
        it("does not load has many when it is not specified to be fetched") {
            val holder = book()
            RepoTransactionProvider(repo, transactionProvider).transaction(Book::class) { bookRepo ->
                val id = bookRepo.create(holder).id!!
                val reloaded = bookRepo.findById(id, fetchRelations = setOf())
                assert(reloaded.chapters is LazyHasMany)
            }
        }
    }

    private fun book(): Book {
        val chapters: Set<Chapter> = setOf(
            Chapter(
                "page 1", hasMany(
                    setOf(
                        Sentence("god is dead"), Sentence(
                            "No small art is it to sleep:" + " it is necessary for that purpose to keep awake all day."
                        )
                    )
                )
            ), Chapter(
                "page 2", hasMany(
                    setOf(
                        Sentence("god is still doing pretty badly"), Sentence("sleeping is still not easy")
                    )
                )
            )
        )
        return Book("Also Sprach Zarathustra", hasMany(chapters))
    }
}
