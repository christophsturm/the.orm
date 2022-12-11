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
    private val belongsToRepos: List<Repo<*>>,
    private val creator: StreamingEntityCreator<Entity>,
    val classInfo: ClassInfo<Entity>
) {
    private val idFieldIndex = classInfo.simpleFieldInfo.indexOfFirst { it.dbFieldName == "id" }
    private val hasManyQueries = classInfo.hasManyRelations.map {
        it.repo.queryFactory.createQuery(it.dbFieldName + "=ANY(?)")
    }
    private val hasManyRemoteFields = classInfo.hasManyRelations.map { fieldInfo ->
        val remoteFieldInfo = fieldInfo.classInfo.belongsToRelations.singleOrNull {
            it.relatedClass == classInfo.kClass
        }
            ?: throw RepositoryException(
                "Corresponding BelongsTo field for HasMany relation " +
                        "${classInfo.name}.${fieldInfo.property.name} not found in ${fieldInfo.classInfo.name}." +
                        " Currently you need to declare both sides of the relation"
            )
        remoteFieldInfo.property
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
            val idLists = Array(belongsToRepos.size) { idx ->
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
                    val repo = belongsToRepos[index]
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
                        .findAndTransform(connectionProvider, fetchRelations) { flow: Flow<Any> ->
                            val result = LinkedHashMap<PK, MutableSet<Entity>>()
                            flow.collect {
                                @Suppress("UNCHECKED_CAST")
                                val prop: KProperty1<Any, BelongsTo.BelongsToNotLoaded<*>> =
                                    hasManyRemoteFields[index] as KProperty1<Any, BelongsTo.BelongsToNotLoaded<*>>
                                val any = prop(it).pk
                                val set = result.getOrPut(any) { mutableSetOf() }
                                set.add(it as Entity)
                            }
                            result
                        }
                    else null
                }
            } else null
            creator.toEntities(resultsList.asFlow(), belongsToRelations, hasManyRelations).collect {
                emit(it)
            }
        }
    }
}
