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
    id("info.solidsoft.pitest")
    `maven-publish`
    kotlin("plugin.serialization")
    id("org.jmailen.kotlinter") version "3.12.0"
    id("com.bnorm.power.kotlin-power-assert")
}



dependencies {
    implementation(enforcedPlatform("org.jetbrains.kotlin:kotlin-bom:$kotlinVersion"))
    implementation(enforcedPlatform("io.netty:netty-bom:$nettyVersion"))
    implementation("io.netty:netty-resolver-dns-native-macos:${nettyVersion}:osx-aarch_64")
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))

    api("io.r2dbc:r2dbc-spi:0.9.1.RELEASE")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("io.r2dbc:r2dbc-pool:0.9.2.RELEASE")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:$coroutinesVersion")
    implementation("io.vertx:vertx-lang-kotlin-coroutines:$vertxVersion")
    implementation("io.vertx:vertx-pg-client:$vertxVersion")


    testImplementation("io.strikt:strikt-core:$striktVersion")
    testImplementation("dev.failgood:failgood:$failgoodVersion")


    testImplementation("com.christophsturm:randolf:0.2.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$serializationVersion")

    testRuntimeOnly("io.r2dbc:r2dbc-h2:1.0.0.RELEASE")
    testImplementation(kotlin("test"))
}
val needsRedefinition = JavaVersion.current().ordinal >= JavaVersion.VERSION_13.ordinal
val testMain = tasks.register("testMain", JavaExec::class) {
    mainClass.set("io.the.orm.test.AllTestsKt")
    classpath = sourceSets["test"].runtimeClasspath
    if (needsRedefinition)
        jvmArgs = mutableListOf("-XX:+AllowRedefinitionToAddDeleteMethods")
}
tasks.register("autoTest", JavaExec::class) {
    mainClass.set("io.the.orm.test.AutoTestKt")
    classpath = sourceSets["test"].runtimeClasspath
    if (needsRedefinition)
        jvmArgs = mutableListOf("-XX:+AllowRedefinitionToAddDeleteMethods")
}

tasks.check {
    dependsOn(testMain)
}

configure<com.bnorm.power.PowerAssertGradleExtension> {
    functions = listOf("kotlin.assert", "kotlin.test.assertTrue", "kotlin.test.assertNotNull")
}
