package r2dbcfun.test

import failfast.FailFast.findTestClasses
import failfast.Suite
import io.mockk.impl.JvmMockKGateway
import io.netty.resolver.dns.UnixResolverDnsServerAddressStreamProvider
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
        thread {
            postgresqlcontainer
        }
    }
    BlockHound.builder().allowBlockingCallsInside(
        UnixResolverDnsServerAddressStreamProvider::class.java.canonicalName,
        "parseEtcResolverSearchDomains"
    ).install()

    Suite.fromClasses(findTestClasses(TransactionFunctionalTest::class)).run().check()
}


private fun enableTestContainersReuse() {
    val testContainersPropertiesFile = File("${System.getProperty("user.home")}/.testcontainers.properties")
    if (!testContainersPropertiesFile.exists())
        testContainersPropertiesFile.writeText("testcontainers.reuse.enable=true")
}

