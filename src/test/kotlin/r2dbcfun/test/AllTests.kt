package r2dbcfun.test

import failfast.FailFast.findTestClasses
import failfast.Suite
import io.mockk.impl.JvmMockKGateway
import io.netty.resolver.dns.UnixResolverDnsServerAddressStreamProvider
import r2dbcfun.TestConfig.CI
import r2dbcfun.TestConfig.H2_ONLY
import r2dbcfun.test.functional.TransactionFunctionalTest
import reactor.blockhound.BlockHound
import java.io.File
import kotlin.concurrent.thread

fun main() {
    // spin up dependencies in separate threads
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

    val classes = findTestClasses(TransactionFunctionalTest::class)
    println(classes.joinToString { it.name })
    Suite.fromClasses(classes).run().check()
}


private fun enableTestContainersReuse() {
    val testContainersPropertiesFile = File("${System.getProperty("user.home")}/.testcontainers.properties")
    if (!testContainersPropertiesFile.exists())
        testContainersPropertiesFile.writeText("testcontainers.reuse.enable=true")
}

