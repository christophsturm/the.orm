package r2dbcfun.test

import failfast.FailFast.runAllTests
import io.mockk.impl.JvmMockKGateway
import io.netty.resolver.dns.UnixResolverDnsServerAddressStreamProvider
import r2dbcfun.TestConfig.CI
import r2dbcfun.TestConfig.H2_ONLY
import reactor.blockhound.BlockHound
import java.io.File
import kotlin.concurrent.thread

fun main() {
    // spin up dependencies in separate threads to speedup test
    thread {
        JvmMockKGateway()
    }
    if (!H2_ONLY) {
        enableTestContainersReuse()
        // prepare all database containers async at startup
        val dbs = databases.map {
            thread {
                it.prepare()
            }
        }

        // and on CI wait for the containers to avoid running into test timeouts
        if (CI)
            dbs.map { it.join() }
    }
    BlockHound.builder().allowBlockingCallsInside(
        UnixResolverDnsServerAddressStreamProvider::class.java.canonicalName,
        "parseEtcResolverSearchDomains"
    ).install()

    runAllTests()
}


private fun enableTestContainersReuse() {
    val testContainersPropertiesFile = File("${System.getProperty("user.home")}/.testcontainers.properties")
    if (!testContainersPropertiesFile.exists())
        testContainersPropertiesFile.writeText("testcontainers.reuse.enable=true")
}

