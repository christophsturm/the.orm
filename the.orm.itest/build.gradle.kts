@file:Suppress("ConstantConditionIf", "GradlePackageUpdate")

import com.adarshr.gradle.testlogger.theme.ThemeType.MOCHA_PARALLEL

import io.the.orm.versions.blockHoundVersion
import io.the.orm.versions.coroutinesVersion
import io.the.orm.versions.failgoodVersion
import io.the.orm.versions.log4j2Version
import io.the.orm.versions.serializationVersion
import io.the.orm.versions.striktVersion
import io.the.orm.versions.vertxVersion


plugins {
    id("the.orm.common")
    id("com.adarshr.test-logger") version "3.2.0"
    java
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("org.jmailen.kotlinter")
    id("com.bnorm.power.kotlin-power-assert")
}




dependencies {
    implementation(project(":the.orm"))
    implementation(project(":the.orm.testutil"))
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))


    implementation("io.r2dbc:r2dbc-pool:1.0.0.RELEASE")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:$coroutinesVersion")


    implementation("io.vertx:vertx-lang-kotlin-coroutines:$vertxVersion")
    implementation("io.vertx:vertx-pg-client:$vertxVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-rx2:$coroutinesVersion")


    testImplementation("io.strikt:strikt-core:$striktVersion")
    testImplementation("dev.failgood:failgood:$failgoodVersion")


    testRuntimeOnly("io.r2dbc:r2dbc-h2:1.0.0.RELEASE")
    testRuntimeOnly("com.h2database:h2:2.1.214")

    // database creation uses jdbc currently. not sure if it should use vert or r2dbc
    testRuntimeOnly("org.postgresql:r2dbc-postgresql:1.0.1.RELEASE")
    testImplementation("com.christophsturm:randolf:0.2.2")


    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-debug:$coroutinesVersion")
    testImplementation("io.projectreactor.tools:blockhound:$blockHoundVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$serializationVersion")

    testImplementation("org.apache.logging.log4j:log4j-core:$log4j2Version")
    testImplementation("org.apache.logging.log4j:log4j-api:$log4j2Version")
    testImplementation("org.apache.logging.log4j:log4j-jul:$log4j2Version")
    testImplementation("org.apache.logging.log4j:log4j-slf4j-impl:$log4j2Version")
    testImplementation(kotlin("test"))
}
val testMain = tasks.register("testMain", JavaExec::class) {
    mainClass.set("io.the.orm.test.functional.AllTestsKt")
    classpath = sourceSets["test"].runtimeClasspath
}

tasks.withType<Test> {
    useJUnitPlatform()
    outputs.upToDateWhen { false }
}
configure<com.adarshr.gradle.testlogger.TestLoggerExtension> {
    theme = MOCHA_PARALLEL
    showSimpleNames = true
}

configure<com.bnorm.power.PowerAssertGradleExtension> {
    functions = listOf("kotlin.assert", "kotlin.test.assertTrue", "kotlin.test.assertNotNull")
}
