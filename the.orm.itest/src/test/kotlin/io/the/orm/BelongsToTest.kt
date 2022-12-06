package io.the.orm

import failgood.Test
import io.the.orm.exp.relations.BelongsTo
import io.the.orm.test.describeOnAllDbs
import io.the.orm.transaction.RepoTransactionProvider

@Test
object BelongsToTest {
    // this test uses the same schema as the HasManyTest but declared as BelongsTo instead of hasMany

    val context = describeOnAllDbs<BelongsTo<*>>(schema = HasManyTest.SCHEMA) {
        val connection = it()
        describe("with always eager loading (declared as the Entity instead of BelongsTo<Entity>)") {
            data class Book(val name: String, val id: PK? = null)
            data class Chapter(val name: String, val book: Book, val id: PK? = null)
            data class Sentence(val content: String, val chapter: Chapter, val id: PK? = null)

            val repo = RepoRegistry(setOf(Chapter::class, Book::class, Sentence::class))
            val repoProvider = RepoTransactionProvider(repo, connection)
            it("can load recursive belongs to relations") {
                repoProvider.transaction(
                    Book::class,
                    Chapter::class,
                    Sentence::class
                ) { bookRepo, chapterRepo, sentenceRepo ->
                    val book = bookRepo.create(Book("TDD is ok"))
                    val chapter = chapterRepo.create(Chapter("Waterfalls are awful", book))
                    val sentence =
                        sentenceRepo.create(Sentence("Except the Niagara falls, everbody loves those", chapter))
                    val loadedSentence = sentenceRepo.findById(sentence.id!!)
                    assert(sentence !== loadedSentence) // should be a new instance.
                    with(sentence.chapter) {
                        assert(name == "Waterfalls are awful")
                        assert(sentence.content == "Except the Niagara falls, everbody loves those")
                    }
                }
            }
        }
    }
}
