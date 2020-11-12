package r2dbcfun

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.spec.IsolationMode
import reactor.blockhound.BlockHound
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

@ExperimentalTime
@Suppress("unused")
object KotestConfig : AbstractProjectConfig() {
    override val parallelism = Runtime.getRuntime().availableProcessors()
    override val isolationMode = IsolationMode.InstancePerTest

    override val timeout = 10.seconds
    override fun beforeAll() {
        BlockHound.install()
    }
}
