package io.the.orm

import failgood.Test
import io.the.orm.relations.BelongsTo
import io.the.orm.relations.belongsTo
import io.the.orm.test.describeOnAllDbs
import io.the.orm.transaction.RepoTransactionProvider
import kotlin.test.assertNotNull

@Test
object BelongsToTest {
    // this test uses the same schema as the HasManyTest but declared as BelongsTo instead of
    // hasMany

    val context =
        describeOnAllDbs<BelongsTo<*>>(schema = HasManyTest.SCHEMA) { connection ->
            describe(
                "with always eager loading (declared as the Entity instead of BelongsTo<Entity>)"
            ) {
                data class Book(val name: String, val id: PKType? = null)
                data class Chapter(val name: String, val book: Book, val id: PKType? = null)
                data class Sentence(
                    val content: String,
                    val chapter: Chapter,
                    val id: PKType? = null
                )

                val repo = RepoRegistry(setOf(Chapter::class, Book::class, Sentence::class))
                val repoProvider = RepoTransactionProvider(repo, connection)
                it("can load recursive belongs to relations") {
                    repoProvider.transaction(Book::class, Chapter::class, Sentence::class) {
                        bookRepo,
                        chapterRepo,
                        sentenceRepo ->
                        val book = bookRepo.create(Book("TDD is ok"))
                        val chapter = chapterRepo.create(Chapter("Waterfalls are awful", book))
                        val sentence =
                            sentenceRepo.create(
                                Sentence("Except the Niagara falls, everbody loves those", chapter)
                            )
                        val loadedSentence = sentenceRepo.findById(sentence.id!!)
                        assert(sentence !== loadedSentence) // should be a new instance.
                        with(loadedSentence.chapter) {
                            assert(name == "Waterfalls are awful")
                            assert(book.name == "TDD is ok")
                        }
                        assert(
                            loadedSentence.content ==
                                "Except the Niagara falls, everbody loves those"
                        )
                    }
                }
            }
            describe("when lazy loading is supported (BelongsTo<Entity>)") {
                data class Book(val name: String, val id: PKType? = null)
                data class Chapter(
                    val name: String,
                    val book: BelongsTo<Book>,
                    val id: PKType? = null
                )
                data class Sentence(
                    val content: String,
                    val chapter: BelongsTo<Chapter>,
                    val id: PKType? = null
                )

                val repo = RepoRegistry(setOf(Chapter::class, Book::class, Sentence::class))
                val repoProvider = RepoTransactionProvider(repo, connection)
                val sentence =
                    repoProvider.transaction(Book::class, Chapter::class, Sentence::class) {
                        bookRepo,
                        chapterRepo,
                        sentenceRepo ->
                        val book = bookRepo.create(Book("TDD is ok"))
                        val chapter =
                            chapterRepo.create(Chapter("Waterfalls are awful", belongsTo(book)))
                        sentenceRepo.create(
                            Sentence(
                                "Except the Niagara falls, everbody loves those",
                                belongsTo(chapter)
                            )
                        )
                    }

                it("does not load belongs to relations by default") {
                    repoProvider.transaction(Sentence::class) { sentenceRepo ->
                        val loadedSentence = sentenceRepo.findById(sentence.id!!)
                        assert(sentence !== loadedSentence) // should be a new instance.
                        assert(
                            loadedSentence.content ==
                                "Except the Niagara falls, everbody loves those"
                        )
                        val result = kotlin.runCatching { loadedSentence.chapter.get() }
                        assertNotNull(result.exceptionOrNull())
                    }
                }
                it("can load recursive belongs to relations when specified in fetchRelations") {
                    repoProvider.transaction(Sentence::class) { sentenceRepo ->
                        val loadedSentence =
                            sentenceRepo.findById(
                                sentence.id!!,
                                fetchRelations = setOf(Sentence::chapter, Chapter::book)
                            )
                        assert(sentence !== loadedSentence) // should be a new instance.
                        with(loadedSentence.chapter) {
                            assert(get().name == "Waterfalls are awful")
                            assert(
                                sentence.content == "Except the Niagara falls, everbody loves those"
                            )
                        }
                    }
                }
            }
        }
}
