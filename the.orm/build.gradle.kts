@file:Suppress("ConstantConditionIf", "GradlePackageUpdate")

import io.the.orm.versions.coroutinesVersion
import io.the.orm.versions.failgoodVersion
import io.the.orm.versions.kotlinVersion
import io.the.orm.versions.nettyVersion
import io.the.orm.versions.serializationVersion
import io.the.orm.versions.striktVersion
import io.the.orm.versions.vertxVersion


plugins {
    id("the.orm.common")
    java
    kotlin("jvm")
    id("info.solidsoft.pitest") version("1.15.0")
    `maven-publish`
    kotlin("plugin.serialization")
    id("com.bnorm.power.kotlin-power-assert")
}



dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:$kotlinVersion"))
    implementation(platform("io.netty:netty-bom:$nettyVersion"))
    implementation(platform("org.jetbrains.kotlinx:kotlinx-coroutines-bom:$coroutinesVersion"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-bom:1.8.0")

    implementation("io.netty:netty-resolver-dns-native-macos:${nettyVersion}:osx-aarch_64")
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))

    api("io.r2dbc:r2dbc-spi:1.0.0.RELEASE")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("io.r2dbc:r2dbc-pool:1.0.1.RELEASE")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:$coroutinesVersion")
    implementation("io.vertx:vertx-lang-kotlin-coroutines:$vertxVersion")
    implementation("io.vertx:vertx-pg-client:$vertxVersion")


    testImplementation("io.strikt:strikt-core:$striktVersion")
    testImplementation("dev.failgood:failgood:$failgoodVersion")


    testImplementation("com.christophsturm:randolf:0.2.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$serializationVersion")

    testRuntimeOnly("io.r2dbc:r2dbc-h2:1.0.0.RELEASE")
    // unused, but r2dbc-h2 uses an old dep that reports security vulnerabilities
    testRuntimeOnly("com.h2database:h2:2.2.224")

    testImplementation(kotlin("test"))
}
tasks.register("autoTest", JavaExec::class) {
    mainClass.set("io.the.orm.test.AutoTestKt")
    classpath = sourceSets["test"].runtimeClasspath
}

configure<com.bnorm.power.PowerAssertGradleExtension> {
    functions = listOf("kotlin.assert", "kotlin.test.assertTrue", "kotlin.test.assertNotNull")
}

plugins.withId("info.solidsoft.pitest") {
    configure<info.solidsoft.gradle.pitest.PitestPluginExtension> {
        mutators.set(listOf("ALL"))
        jvmArgs.set(listOf("-Xmx512m")) // necessary on CI
        avoidCallsTo.set(setOf("kotlin.jvm.internal", "kotlin.Result"))
        targetClasses.set(setOf("io.the.orm.*")) // by default "${project.group}.*"
        targetTests.set(setOf("io.the.orm.*", "io.the.orm.**.*"))
        pitestVersion.set("1.10.0")
        threads.set(
            System.getenv("PITEST_THREADS")?.toInt() ?: Runtime.getRuntime().availableProcessors()
        )
        outputFormats.set(setOf("XML", "HTML"))
    }
}
