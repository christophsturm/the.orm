package io.the.orm

import io.the.orm.internal.classinfo.ClassInfo
import kotlin.reflect.KClass

data class RepoRegistry(val entityRepos: Map<KClass<out Any>, Repo<out Any>>) {
    companion object {
        operator fun invoke(classes: Set<KClass<out Any>>): RepoRegistry {
            val classInfo = classes.associateBy({ it }) { ClassInfo(it, classes) }
            val entityRepos: Map<KClass<out Any>, RepoImpl<out Any>> =
                classes.associateBy({ it }, { RepoImpl(it, classInfo) })
            entityRepos.values.forEach { it.afterInit(entityRepos) }
            return RepoRegistry(entityRepos)
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getRepo(kClass: KClass<T>) = entityRepos[kClass] as Repo<T>
}

inline fun <reified T : Any> RepoRegistry.getRepo() = getRepo(T::class)

@Suppress("UNCHECKED_CAST")
fun <T : Any> Map<KClass<*>, RepoImpl<*>>.getRepo(c: KClass<T>): RepoImpl<T> = get(c) as RepoImpl<T>
