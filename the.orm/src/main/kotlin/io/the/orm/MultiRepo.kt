package io.the.orm

import kotlin.reflect.KClass



data class MultiRepo(val entityRepos: Map<KClass<out Any>, Repo<out Any>>) {
    companion object {
        operator fun invoke(classes: List<KClass<out Any>>): MultiRepo {
            val entityRepos: Map<KClass<out Any>, Repo<out Any>> =
                classes.associateBy({ it }, { RepoImpl(it, classes.toSet()) })
            return MultiRepo(entityRepos)
        }
    }
    fun <T : Any> getRepo(kClass: KClass<T>) = entityRepos[kClass] as Repo<T>
}

inline fun <reified T : Any> MultiRepo.getRepo() = getRepo(T::class)
