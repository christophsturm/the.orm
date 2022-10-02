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
    id("com.adarshr.test-logger") version "3.2.0"
    java
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("org.jmailen.kotlinter") version "3.12.0"
    id("com.bnorm.power.kotlin-power-assert")
}




dependencies {
    implementation(project(":the.orm"))
    implementation(project(":the.orm.testutil"))
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))


    implementation("io.r2dbc:r2dbc-pool:0.9.2.RELEASE")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:$coroutinesVersion")


    implementation("io.vertx:vertx-lang-kotlin-coroutines:$vertxVersion")
    implementation("io.vertx:vertx-pg-client:$vertxVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-rx2:$coroutinesVersion")


    testImplementation("io.strikt:strikt-core:$striktVersion")
    testImplementation("dev.failgood:failgood:$failgoodVersion")


    testRuntimeOnly("io.r2dbc:r2dbc-h2:0.9.1.RELEASE")
    testRuntimeOnly("com.h2database:h2:2.1.214")
    testRuntimeOnly("org.postgresql:postgresql:42.5.0")
    testRuntimeOnly("org.postgresql:r2dbc-postgresql:0.9.2.RELEASE")
    testImplementation("com.christophsturm:randolf:0.2.2")


    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-debug:$coroutinesVersion")
    testImplementation("io.projectreactor.tools:blockhound:$blockHoundVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$serializationVersion")

    testImplementation("org.apache.logging.log4j:log4j-core:$log4j2Version")
    testImplementation("org.apache.logging.log4j:log4j-api:$log4j2Version")
    testImplementation("org.apache.logging.log4j:log4j-jul:$log4j2Version")
    testImplementation("org.apache.logging.log4j:log4j-slf4j-impl:$log4j2Version")
}
val needsRedefinition = JavaVersion.current().ordinal >= JavaVersion.VERSION_13.ordinal
val testMain = tasks.register("testMain", JavaExec::class) {
    mainClass.set("io.the.orm.test.functional.AllTestsKt")
    classpath = sourceSets["test"].runtimeClasspath
    if (needsRedefinition)
        jvmArgs = mutableListOf("-XX:+AllowRedefinitionToAddDeleteMethods")
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
