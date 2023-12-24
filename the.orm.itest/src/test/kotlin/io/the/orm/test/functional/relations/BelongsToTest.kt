package io.the.orm.test.functional.relations

import failgood.Test
import io.the.orm.PKType
import io.the.orm.RepoRegistry
import io.the.orm.relations.BelongsTo
import io.the.orm.relations.belongsTo
import io.the.orm.test.DBS
import io.the.orm.test.describeAll
import io.the.orm.test.fixture
import io.the.orm.transaction.RepoTransactionProvider
import kotlin.test.assertNotNull

@Test
object BelongsToTest {
    // this test uses the same schema as the HasManyTest but declares the BelongsTo part instead of
    // the HasMany

    val context =
        DBS.databases.describeAll(given = { it.fixture(HasManyTest.SCHEMA) }) {
            describe("entities that declare the relation directly", given = { given() }) {
                data class Book(val name: String, val id: PKType? = null)
                data class Chapter(val name: String, val book: Book, val id: PKType? = null)
                data class Sentence(
                    val content: String,
                    val chapter: Chapter,
                    val id: PKType? = null
                )
                describe(
                    "eager loading is not possible",
                    given = {
                        val repo = RepoRegistry(setOf(Chapter::class, Book::class, Sentence::class))
                        RepoTransactionProvider(repo, given().transactionProvider)
                    }
                ) {
                    it("will always load belongs to relations") {
                        given.transaction(Book::class, Chapter::class, Sentence::class) {
                            bookRepo,
                            chapterRepo,
                            sentenceRepo ->
                            val book = bookRepo.create(Book("TDD is ok"))
                            val chapter = chapterRepo.create(Chapter("Waterfalls are awful", book))
                            val sentence =
                                sentenceRepo.create(
                                    Sentence(
                                        "Except the Niagara falls, everybody loves those",
                                        chapter
                                    )
                                )
                            val loadedSentence = sentenceRepo.findById(sentence.id!!)
                            assert(sentence !== loadedSentence) // should be a new instance.
                            with(loadedSentence.chapter) {
                                assert(name == "Waterfalls are awful")
                                assert(book.name == "TDD is ok")
                            }
                            assert(
                                loadedSentence.content ==
                                    "Except the Niagara falls, everybody loves those"
                            )
                        }
                    }
                }
            }
            describe("entities that declare relations as BelongsTo<Entity>", given = { given() }) {
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
                describe(
                    "support lazy loading",
                    given = {
                        val repo = RepoRegistry(setOf(Chapter::class, Book::class, Sentence::class))
                        val repoProvider =
                            RepoTransactionProvider(repo, given().transactionProvider)
                        val sentence =
                            repoProvider.transaction(
                                Book::class,
                                Chapter::class,
                                Sentence::class
                            ) { bookRepo, chapterRepo, sentenceRepo ->
                                val book = bookRepo.create(Book("TDD is ok"))
                                val chapter =
                                    chapterRepo.create(
                                        Chapter("Waterfalls are awful", belongsTo(book))
                                    )
                                sentenceRepo.create(
                                    Sentence(
                                        "Except the Niagara falls, everybody loves those",
                                        belongsTo(chapter)
                                    )
                                )
                            }
                        Pair(sentence, repoProvider)
                    }
                ) {
                    it("does not load belongs to relations by default") {
                        val (sentence, repoProvider) = given
                        repoProvider.transaction(Sentence::class) { sentenceRepo ->
                            val loadedSentence = sentenceRepo.findById(sentence.id!!)
                            assert(sentence !== loadedSentence) // should be a new instance.
                            assert(
                                loadedSentence.content ==
                                    "Except the Niagara falls, everybody loves those"
                            )
                            val result = kotlin.runCatching { loadedSentence.chapter.get() }
                            assertNotNull(result.exceptionOrNull())
                        }
                    }
                    it("can load recursive belongs to relations when specified in fetchRelations") {
                        val (sentence, repoProvider) = given
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
                                    sentence.content ==
                                        "Except the Niagara falls, everybody loves those"
                                )
                            }
                        }
                    }
                }
            }
        }
}
