package io.the.orm

import kotlin.reflect.KClass

interface MultiRepo {
    companion object {
        operator fun invoke(classes: List<KClass<out Any>>) = MultiRepoImpl(classes)
    }

    fun <T : Any> getRepo(kClass: KClass<T>): Repo<T>
}

class MultiRepoImpl(classes: List<KClass<out Any>>) : MultiRepo {
    private val entityRepos: Map<KClass<out Any>, Repo<out Any>> =
        classes.associateBy({ it }, { RepoImpl(it, classes.toSet()) })

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getRepo(kClass: KClass<T>) = entityRepos[kClass] as Repo<T>
}

inline fun <reified T : Any> MultiRepo.getRepo() = getRepo(T::class)
