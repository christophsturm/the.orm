package io.the.orm.test.functional

import failgood.FailGood.runAllTests
import io.netty.resolver.dns.UnixResolverDnsServerAddressStreamProvider
import io.the.orm.test.DBS
import io.the.orm.test.TestUtilConfig.H2_ONLY
import reactor.blockhound.BlockHound
import java.io.File
import kotlin.concurrent.thread

fun main() {
    // spin up dependencies in separate threads to speedup test

    if (!H2_ONLY) {
        enableTestContainersReuse()
        // prepare all database containers async at startup
        thread {
            DBS.databases.map {
                it.prepare()
            }
        }
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

