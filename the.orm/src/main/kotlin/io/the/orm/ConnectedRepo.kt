package io.the.orm

import io.the.orm.dbio.ConnectionProvider
import io.the.orm.dbio.TransactionProvider
import io.the.orm.query.QueryFactory
import io.the.orm.relations.Relation
import kotlin.reflect.KProperty1

interface ConnectedRepo<T : Any> {
    val repo: Repo<T>
    val queryFactory: QueryFactory<T>
    val connectionProvider: ConnectionProvider

    companion object {
        inline fun <reified T : Any> create(connection: ConnectionProvider): ConnectedRepo<T> =
            Impl(RepoImpl(T::class), connection)
        operator fun <T : Any> invoke(repo: Repo<T>, connectionProvider: ConnectionProvider) =
            Impl(repo, connectionProvider)
    }

    suspend fun create(entity: T): T

    suspend fun update(entity: T)

    suspend fun findById(pk: PKType, fetchRelations: Set<KProperty1<*, Relation>> = setOf()): T
    suspend fun findByIds(pk: List<PKType>, fetchRelations: Set<KProperty1<*, Relation>> = setOf()): Map<PKType, T>
    class Impl<T : Any>(override val repo: Repo<T>, override val connectionProvider: ConnectionProvider) :
        ConnectedRepo<T> {

        override suspend fun create(entity: T): T = repo.create(connectionProvider, entity)
        override suspend fun update(entity: T): Unit = repo.update(connectionProvider, entity)
        override val queryFactory: QueryFactory<T> = repo.queryFactory

        override suspend fun findById(pk: PKType, fetchRelations: Set<KProperty1<*, Relation>>): T =
            repo.findById(connectionProvider, pk, fetchRelations)

        override suspend fun findByIds(pk: List<PKType>, fetchRelations: Set<KProperty1<*, Relation>>): Map<PKType, T> =
            repo.findByIds(connectionProvider, pk, fetchRelations)
    }
}

interface TransactionalRepo<T : Any> : ConnectedRepo<T> {
    companion object {
        inline fun <reified T : Any> create(connection: TransactionProvider): TransactionalRepo<T> =
            TransactionalRepoImpl(RepoImpl(T::class), connection)
        operator fun <T : Any> invoke(repo: Repo<T>, connection: TransactionProvider): TransactionalRepo<T> =
            TransactionalRepoImpl(repo, connection)
    }
    override val repo: Repo<T>
    override val connectionProvider: TransactionProvider
    suspend fun <R> transaction(function: suspend (ConnectedRepo<T>) -> R): R
}

class TransactionalRepoImpl<T : Any>private constructor(
    override val repo: Repo<T>,
    override val connectionProvider: TransactionProvider,
    private val connectedRepo: ConnectedRepo<T>
) : TransactionalRepo<T>, ConnectedRepo<T> by connectedRepo {
    constructor(repo: Repo<T>, connectionProvider: TransactionProvider) :
        this(repo, connectionProvider, ConnectedRepo(repo, connectionProvider))

    override suspend fun <R> transaction(function: suspend (ConnectedRepo<T>) -> R): R =
        connectionProvider.transaction { transactionConnectionProvider ->
            function(ConnectedRepo.Impl(repo, transactionConnectionProvider))
        }
}
