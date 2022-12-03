package io.the.orm.transaction

import failgood.Test
import failgood.describe
import failgood.mock.mock
import io.the.orm.ConnectedRepo
import io.the.orm.MultiRepo
import io.the.orm.PK
import io.the.orm.Repo
import io.the.orm.exp.testing.MockConnectionProvider
import io.the.orm.exp.testing.MockTransactionProvider
import kotlinx.coroutines.delay
import kotlin.test.assertEquals

@Test
object RepoTransactionProviderTest {
    val context = describe<RepoTransactionProvider> {
        data class Entity(val name: String, val id: PK?)
        data class Entity1(val name: String, val id: PK?)
        it("can start a transaction with one repo") {

            val repo = mock<Repo<Entity>>()
            var passedRepo: ConnectedRepo<Entity>? = null
            val repos = mock<MultiRepo> {
                method { getRepo(Entity::class) }.returns(repo)
            }

            val connectionProvider = MockConnectionProvider()
            val r = RepoTransactionProvider(repos, MockTransactionProvider(connectionProvider))
            val result = r.transaction { entityRepo: ConnectedRepo<Entity> ->
                delay(0)
                passedRepo = entityRepo
                "result"
            }
            assertEquals("result", result)
            assert(repo == passedRepo?.repo)
            assert(connectionProvider == passedRepo?.connectionProvider)
        }
        it("can start a transaction with two repo") {
            var passedRepo: ConnectedRepo<Entity>? = null
            var passedRepo1: ConnectedRepo<Entity1>? = null
            val repos = MultiRepo(listOf(Entity::class, Entity1::class))

            val connectionProvider = MockConnectionProvider()
            val r = RepoTransactionProvider(repos, MockTransactionProvider(connectionProvider))
            val result = r.transaction(
                Entity::class,
                Entity1::class
            ) { entityRepo: ConnectedRepo<Entity>, entity1Repo: ConnectedRepo<Entity1> ->
                delay(0)
                passedRepo = entityRepo
                passedRepo1 = entity1Repo
                "result"
            }
            assertEquals("result", result)
            assert(repos.getRepo(Entity::class) == passedRepo?.repo)
            assert(repos.getRepo(Entity1::class) == passedRepo1?.repo)
            assert(connectionProvider == passedRepo?.connectionProvider)
        }
    }
}
