package io.the.orm.mapper

import io.the.orm.PK
import io.the.orm.Repo
import io.the.orm.RepositoryException
import io.the.orm.dbio.ConnectionProvider
import io.the.orm.internal.classinfo.ClassInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList

internal class RelationFetchingEntityCreator<Entity : Any>(
    // one repo for every field in relation, in the same order
    private val repos: List<Repo<*>>,
    private val creator: StreamingEntityCreator<Entity>,
    val classInfo: ClassInfo<Entity>
) {
    val idFieldIndex = classInfo.simpleFieldInfo.indexOfFirst { it.dbFieldName == "id" }
    val hasManyQueries = classInfo.hasManyRelations.map {
        it.repo.queryFactory.createQuery(it.dbFieldName + "=ANY(?)")
    }
    fun toEntities(results: Flow<ResultLine>, connectionProvider: ConnectionProvider): Flow<Entity> {
        return flow {
            val pkList = if (classInfo.hasHasManyRelations)
                mutableListOf<PK>() else null
            val idLists = Array(repos.size) { mutableSetOf<PK>() }
            val resultsList = results.toList()
            resultsList.forEach { resultLine ->
                pkList?.add(resultLine.fields[idFieldIndex] as PK)
                resultLine.relations.forEachIndexed { idx, v ->
                    idLists[idx].add(v as PK)
                }
            }
            val belongsToRelations =
                idLists.mapIndexed { index, longs ->
                    val repo = repos[index]
                    val ids = longs.toList()
                    try {
                        repo.findByIds(connectionProvider, ids)
                    } catch (e: Exception) {
                        throw RepositoryException("unexpected error fetching ids $ids from $repo", e)
                    }
                }
            val hasManyRelations = if (pkList != null) {
                hasManyQueries.map { it.with(pkList.toTypedArray()).find(connectionProvider).toList() } // WIP
            } else null
            creator.toEntities(resultsList.asFlow(), belongsToRelations).collect {
                emit(it)
            }
        }
    }
}
