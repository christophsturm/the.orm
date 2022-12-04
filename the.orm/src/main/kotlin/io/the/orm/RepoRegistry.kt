package io.the.orm

import kotlin.reflect.KClass

data class RepoRegistry(val entityRepos: Map<KClass<out Any>, Repo<out Any>>) {
    companion object {
        operator fun invoke(classes: List<KClass<out Any>>): RepoRegistry {
            val entityRepos: Map<KClass<out Any>, Repo<out Any>> =
                classes.associateBy({ it }, { RepoImpl(it, classes.toSet()) })
            return RepoRegistry(entityRepos)
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getRepo(kClass: KClass<T>) = entityRepos[kClass] as Repo<T>
}

inline fun <reified T : Any> RepoRegistry.getRepo() = getRepo(T::class)
