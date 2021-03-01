package r2dbcfun.dbio.vertx

import failfast.describe
import r2dbcfun.dbio.ConnectionProvider

object VertxConnectionTest {
    val context = describe(VertxConnection::class) {
        it("works with ConnectionProvider") {
            ConnectionProvider(VertxConnection())
        }
    }
}
