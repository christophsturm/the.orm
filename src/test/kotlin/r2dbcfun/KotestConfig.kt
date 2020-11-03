package r2dbcfun

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.spec.IsolationMode

object KotestConfig : AbstractProjectConfig() {
    override val parallelism = Runtime.getRuntime().availableProcessors()
    override val isolationMode = IsolationMode.InstancePerTest
}
