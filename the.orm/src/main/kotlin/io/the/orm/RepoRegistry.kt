package io.the.orm

import io.the.orm.internal.classinfo.ClassInfo
import kotlin.reflect.KClass

/**
 * The Repo Registry knows about all the classes in your model classes can have relations to other
 * classes inside the registry
 */
class RepoRegistry
private constructor(private val entityRepos: Map<KClass<out Any>, Repo<out Any>>) {
    companion object {
        /** create a repo registry from a set of classes */
        operator fun invoke(classes: Set<KClass<out Any>>): RepoRegistry {
            val classInfos = classes.associateBy({ it }) { ClassInfo(it, classes) }
            val entityRepos: Map<KClass<out Any>, RepoImpl<out Any>> =
                classes.associateBy({ it }, { RepoImpl(it, classInfos) })
            classInfos.values.forEach { it.afterInit(entityRepos) }
            entityRepos.values.forEach { it.afterInit() }
            return RepoRegistry(entityRepos)
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getRepo(kClass: KClass<T>) = entityRepos[kClass] as Repo<T>
}

inline fun <reified T : Any> RepoRegistry.getRepo() = getRepo(T::class)
