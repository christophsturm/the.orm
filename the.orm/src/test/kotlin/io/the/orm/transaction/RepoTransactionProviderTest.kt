package io.the.orm.transaction

import failgood.Test
import failgood.describe
import io.the.orm.ConnectedRepo
import io.the.orm.MultiRepo
import io.the.orm.PK
import io.the.orm.exp.testing.MockConnectionProvider
import io.the.orm.exp.testing.MockTransactionProvider
import kotlinx.coroutines.delay
import kotlin.test.assertEquals

@Test
object RepoTransactionProviderTest {
    val context = describe<RepoTransactionProvider> {
        data class Entity1(val name: String, val id: PK?)
        data class Entity2(val name: String, val id: PK?)
        data class Entity3(val name: String, val id: PK?)

        var passedRepo1: ConnectedRepo<Entity1>? = null
        var passedRepo2: ConnectedRepo<Entity2>? = null
        var passedRepo3: ConnectedRepo<Entity3>? = null
        val repos = MultiRepo(listOf(Entity1::class, Entity2::class, Entity3::class))
        val connectionProvider = MockConnectionProvider()
        val r = RepoTransactionProvider(repos, MockTransactionProvider(connectionProvider))
        it("can start a transaction with one repo") {
            val result = r.transaction { entity1Repo: ConnectedRepo<Entity1> ->
                delay(0)
                passedRepo1 = entity1Repo
                "result"
            }
            assertEquals("result", result)
            assert(repos.getRepo(Entity1::class) == passedRepo1?.repo)
            assert(connectionProvider == passedRepo1?.connectionProvider)
        }
        it("can start a transaction with two repo") {
            val result = r.transaction(
                Entity1::class, Entity2::class
            ) { entity1Repo: ConnectedRepo<Entity1>, entity2Repo: ConnectedRepo<Entity2> ->
                delay(0)
                passedRepo1 = entity1Repo
                passedRepo2 = entity2Repo
                "result"
            }
            assertEquals("result", result)
            assert(repos.getRepo(Entity1::class) == passedRepo1?.repo)
            assert(repos.getRepo(Entity2::class) == passedRepo2?.repo)
            assert(connectionProvider == passedRepo1?.connectionProvider)
        }
        it("can start a transaction with three repo") {
            val result = r.transaction(
                Entity1::class, Entity2::class, Entity3::class
            ) { entity1Repo: ConnectedRepo<Entity1>,
                entity2Repo: ConnectedRepo<Entity2>,
                entity3Repo: ConnectedRepo<Entity3> ->
                delay(0)
                passedRepo1 = entity1Repo
                passedRepo2 = entity2Repo
                passedRepo3 = entity3Repo
                "result"
            }
            assertEquals("result", result)
            assert(repos.getRepo(Entity1::class) == passedRepo1?.repo)
            assert(repos.getRepo(Entity2::class) == passedRepo2?.repo)
            assert(repos.getRepo(Entity3::class) == passedRepo3?.repo)
            assert(connectionProvider == passedRepo1?.connectionProvider)
        }
    }
}
