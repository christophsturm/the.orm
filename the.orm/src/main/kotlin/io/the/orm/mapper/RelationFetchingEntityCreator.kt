package io.the.orm.mapper

import io.the.orm.PK
import io.the.orm.Repo
import io.the.orm.dbio.ConnectionProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList

internal class RelationFetchingEntityCreator<Entity : Any>(
    // one repo for every field in relation, in the same order
    private val repos: List<Repo<*>>,
    private val creator: EntityCreator<Entity>
) {
    fun toEntities(results: Flow<ResultLine>, connectionProvider: ConnectionProvider): Flow<Entity> {
        return flow {
            val idLists = Array(repos.size) { mutableSetOf<PK>() }
            val resultsList = results.toList()
            resultsList.forEach { resultLine ->
                resultLine.relations.forEachIndexed { idx, v ->
                    idLists[idx].add(v as PK)
                }
            }
            val relations =
                idLists.mapIndexed { index, longs ->
                    repos[index].findByIds(connectionProvider, longs.toList())
                }
            creator.toEntities(resultsList.asFlow(), relations).collect {
                emit(it)
            }
        }
    }
}
