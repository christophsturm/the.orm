package r2dbcfun

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.spec.IsolationMode
import io.netty.resolver.dns.UnixResolverDnsServerAddressStreamProvider
import r2dbcfun.test.container
import reactor.blockhound.BlockHound
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
        BlockHound.builder().allowBlockingCallsInside(
            UnixResolverDnsServerAddressStreamProvider::class.java.canonicalName,
            "parseEtcResolverSearchDomains"
        ).install()

        if (!TestConfig.H2_ONLY) {  // if we run only on h2 we don't need docker at all.
            enableTestContainersReuse()
            container
        }
    }

    private fun enableTestContainersReuse() {
        val testContainersPropertiesFile = File("${System.getProperty("user.home")}/.testcontainers.properties")
        if (!testContainersPropertiesFile.exists())
            testContainersPropertiesFile.writeText("testcontainers.reuse.enable=true")
    }
}
