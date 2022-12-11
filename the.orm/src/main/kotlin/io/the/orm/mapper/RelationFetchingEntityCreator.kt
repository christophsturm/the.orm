package io.the.orm.mapper

import io.the.orm.PK
import io.the.orm.Repo
import io.the.orm.RepositoryException
import io.the.orm.dbio.ConnectionProvider
import io.the.orm.exp.relations.BelongsTo
import io.the.orm.exp.relations.Relation
import io.the.orm.internal.classinfo.ClassInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlin.reflect.KProperty1

internal class RelationFetchingEntityCreator<Entity : Any>(
    // one repo for every field in relation, in the same order
    private val repos: List<Repo<*>>,
    private val creator: StreamingEntityCreator<Entity>,
    val classInfo: ClassInfo<Entity>
) {
    private val idFieldIndex = classInfo.simpleFieldInfo.indexOfFirst { it.dbFieldName == "id" }
    private val hasManyQueries = classInfo.hasManyRelations.map {
        it.repo.queryFactory.createQuery(it.dbFieldName + "=ANY(?)")
    }

    // properties for every relation. they will only be fetched when contained in fetchRelations
    private val hasManyProperties = classInfo.hasManyRelations.map { it.property }

    // if the property is not lazy it must always be fetched, and we indicate that by setting the value to null.
    private val belongsToProperties = classInfo.belongsToRelations.map { if (it.canBeLazy) it.property else null }
    fun toEntities(
        results: Flow<ResultLine>,
        fetchRelations: Set<KProperty1<*, Relation>>,
        connectionProvider: ConnectionProvider
    ): Flow<Entity> {
        return flow {
            val pkList = if (classInfo.hasHasManyRelations) mutableListOf<PK>() else null
            val idLists = Array(repos.size) { idx ->
                if (belongsToProperties[idx] == null || fetchRelations.contains(belongsToProperties[idx]))
                    mutableSetOf<PK>()
                else
                    null
            }
            val resultsList = results.toList()
            resultsList.forEach { resultLine ->
                pkList?.add(resultLine.fields[idFieldIndex] as PK)
                resultLine.relations.forEachIndexed { idx, v ->
                    idLists[idx]?.add(v as PK)
                }
            }
            val belongsToRelations = idLists.mapIndexed { index, longs ->
                if (longs != null) {
                    val repo = repos[index]
                    val ids = longs.toList()
                    val relatedEntities = try {
                        repo.findByIds(connectionProvider, ids)
                    } catch (e: Exception) {
                        throw RepositoryException("unexpected error fetching ids $ids from $repo", e)
                    }
                    if (belongsToProperties[index] == null)
                        relatedEntities
                    else
                        relatedEntities.mapValues { BelongsTo.BelongsToImpl(it.value) }
                } else null
            }
            val hasManyRelations = if (pkList != null) {
                hasManyQueries.withIndex().map { (index, query) ->
                    if (fetchRelations.contains(hasManyProperties[index])) query.with(pkList.toTypedArray())
                        .find(connectionProvider).toList()
                    else null
                } // WIP
            } else null
            creator.toEntities(resultsList.asFlow(), belongsToRelations).collect {
                emit(it)
            }
        }
    }
}
