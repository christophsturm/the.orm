package r2dbcfun

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.spec.IsolationMode
import reactor.blockhound.BlockHound

@Suppress("unused")
object KotestConfig : AbstractProjectConfig() {
    override val parallelism = Runtime.getRuntime().availableProcessors()
    override val isolationMode = IsolationMode.InstancePerTest

    override fun beforeAll() {
        BlockHound.install()
    }
}
