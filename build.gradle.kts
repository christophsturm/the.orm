import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
    id("com.github.ben-manes.versions") version "0.47.0"
    kotlin("plugin.serialization").version(io.the.orm.versions.kotlinVersion) apply false
    id("org.jmailen.kotlinter") version "3.14.0" apply false
    id("com.bnorm.power.kotlin-power-assert") version "0.13.0" apply false
}



tasks.named<DependencyUpdatesTask>("dependencyUpdates") {
    val filtered =
        listOf("alpha", "beta", "rc", "cr", "m", "preview", "dev", "eap")
            .map { qualifier -> Regex("(?i).*[.-]$qualifier[.\\d-]*.*") }
    resolutionStrategy {
        componentSelection {
            all {
                if (filtered.any { it.matches(candidate.version) }) {
                    reject("Release candidate")
                }
            }
        }
        // optional parameters
        checkForGradleUpdate = true
        outputFormatter = "json"
        outputDir = "build/dependencyUpdates"
        reportfileName = "report"
    }
}

tasks.wrapper { distributionType = Wrapper.DistributionType.ALL }
