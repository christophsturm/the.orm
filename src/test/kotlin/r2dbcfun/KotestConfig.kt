package r2dbcfun

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.spec.IsolationMode
import r2dbcfun.test.container
import java.io.File
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

@ExperimentalTime
@Suppress("unused")
object KotestConfig : AbstractProjectConfig() {
//    override val parallelism = Runtime.getRuntime().availableProcessors()
    override val isolationMode = IsolationMode.InstancePerTest

    override val timeout = if (TestConfig.PITEST) 100.seconds else if (TestConfig.CI) 20.seconds else 5.seconds
    override fun beforeAll() {
//        BlockHound.install()

        enableTestContainersReuse()
        container
    }

    private fun enableTestContainersReuse() {
        val testContainersPropertiesFile = File("${System.getProperty("user.home")}/.testcontainers.properties")
        if (!testContainersPropertiesFile.exists())
            testContainersPropertiesFile.writeText("testcontainers.reuse.enable=true")
    }
}
