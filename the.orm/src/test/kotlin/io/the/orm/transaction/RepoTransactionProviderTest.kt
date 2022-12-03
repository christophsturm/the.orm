package io.the.orm.transaction

import failgood.Test
import failgood.describe
import failgood.mock.mock
import io.the.orm.ConnectedRepo
import io.the.orm.MultiRepo
import io.the.orm.Repo
import io.the.orm.exp.testing.MockConnectionProvider
import io.the.orm.exp.testing.MockTransactionProvider
import kotlinx.coroutines.delay
import kotlin.test.assertEquals

@Test
object RepoTransactionProviderTest {
    val context = describe<RepoTransactionProvider> {
        it("can start a transaction with one repo") {
            data class Entity(val name: String)

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
    }
}
