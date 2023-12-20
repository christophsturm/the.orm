package io.the.orm.mapper

import io.the.orm.PKType
import io.the.orm.Repo
import io.the.orm.RepositoryException
import io.the.orm.dbio.ConnectionProvider
import io.the.orm.internal.classinfo.ClassInfo
import io.the.orm.query.Query
import io.the.orm.relations.BelongsTo
import io.the.orm.relations.Relation
import kotlin.reflect.KProperty1
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList

internal class RelationFetchingEntityCreator<Entity : Any>(
    // one repo for every field in relation, in the same order
    private val belongsToRepos: List<Repo<*>>,
    private val creator: StreamingEntityCreator<Entity>,
    private val classInfo: ClassInfo<Entity>,
    private val hasManyQueries: List<Query<*>>
) {
    private val idFieldIndex = classInfo.simpleFields.indexOfFirst { it.dbFieldName == "id" }
    private val hasManyRemoteFields =
        classInfo.hasManyRelations.map { fieldInfo ->
            val remoteFieldInfo =
                fieldInfo.classInfo.belongsToRelations.singleOrNull {
                    it.relatedClass == classInfo.kClass
                }
                    ?: throw RepositoryException(
                        "BelongsTo field for HasMany relation " +
                            "${classInfo.name}.${fieldInfo.property.name} not found in ${fieldInfo.classInfo.name}." +
                            " Currently you need to declare both sides of the relation"
                    )
            if (!remoteFieldInfo.canBeLazy)
                throw RepositoryException(
                    "${remoteFieldInfo.name} " +
                        "must be lazy (BelongsTo<Type> instead of Type) to avoid circular dependencies"
                )

            remoteFieldInfo.property
        }

    // properties for every relation. they will only be fetched when contained in fetchRelations
    private val hasManyProperties = classInfo.hasManyRelations.map { it.property }

    // if the property is not lazy it must always be fetched, and we indicate that by setting the
    // value to null.
    private val belongsToProperties =
        classInfo.belongsToRelations.map { if (it.canBeLazy) it.property else null }

    fun toEntities(
        results: Flow<ResultLine>,
        fetchRelations: Set<KProperty1<*, Relation>>,
        connectionProvider: ConnectionProvider
    ): Flow<Entity> {
        return flow {
            val pkList = if (classInfo.hasHasManyRelations) mutableListOf<PKType>() else null
            val idLists =
                Array(belongsToRepos.size) { idx ->
                    if (
                        belongsToProperties[idx] == null ||
                            fetchRelations.contains(belongsToProperties[idx])
                    )
                        mutableSetOf<PKType>()
                    else null
                }
            val resultsList = results.toList()
            resultsList.forEach { resultLine ->
                pkList?.add(resultLine.fields[idFieldIndex] as PKType)
                resultLine.relations.forEachIndexed { idx, v -> idLists[idx]?.add(v as PKType) }
            }
            val belongsToRelations =
                idLists.mapIndexed { index, longs ->
                    if (longs != null) {
                        val repo = belongsToRepos[index]
                        val ids = longs.toList()
                        val relatedEntities =
                            try {
                                repo.findByIds(connectionProvider, ids)
                            } catch (e: Exception) {
                                throw RepositoryException(
                                    "unexpected error fetching ids $ids from $repo",
                                    e
                                )
                            }
                        if (belongsToProperties[index] == null) relatedEntities
                        else relatedEntities.mapValues { BelongsTo.BelongsToImpl(it.value) }
                    } else null
                }
            val hasManyRelations =
                if (pkList != null) {
                    hasManyQueries.withIndex().map { (index, query) ->
                        if (fetchRelations.contains(hasManyProperties[index]))
                            query.with(pkList.toTypedArray()).findAndTransform<
                                Map<PKType, Set<Entity>>
                            >(
                                connectionProvider,
                                fetchRelations
                            ) { flow: Flow<Any> ->
                                val result = LinkedHashMap<PKType, MutableSet<Entity>>()
                                flow.collect {
                                    @Suppress("UNCHECKED_CAST")
                                    val prop: KProperty1<Any, BelongsTo.BelongsToNotLoaded<*>> =
                                        hasManyRemoteFields[index]
                                            as KProperty1<Any, BelongsTo.BelongsToNotLoaded<*>>
                                    val pk = prop(it).pk
                                    val set = result.getOrPut(pk) { mutableSetOf() }
                                    @Suppress("UNCHECKED_CAST") set.add(it as Entity)
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
