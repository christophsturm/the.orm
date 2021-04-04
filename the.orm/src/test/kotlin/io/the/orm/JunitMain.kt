package io.the.orm

import failfast.junit.FailFastJunitTestEngine
import org.junit.platform.engine.discovery.DiscoverySelectors
import org.junit.platform.launcher.EngineFilter
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestPlan
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import org.junit.platform.launcher.core.LauncherFactory
import java.nio.file.Paths

fun main() {
    val selectors = DiscoverySelectors.selectClasspathRoots(
        setOf(Paths.get(object {}::class.java.protectionDomain.codeSource.location.toURI()))
    )
    val request = LauncherDiscoveryRequestBuilder.request()
        .filters(EngineFilter.includeEngines(FailFastJunitTestEngine().id))
        .selectors(selectors).build()
    LauncherFactory.create().execute(request, TListener())
}

class TListener : TestExecutionListener {
    override fun testPlanExecutionFinished(testPlan: TestPlan?) {
        println(testPlan)
    }
}
