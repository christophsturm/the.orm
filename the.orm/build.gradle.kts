@file:Suppress("ConstantConditionIf")

import info.solidsoft.gradle.pitest.PitestPluginExtension
import io.the.orm.BuildConfig
import io.the.orm.BuildConfig.failgoodVersion
import io.the.orm.BuildConfig.striktVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "io.the.orm"

val coroutinesVersion = BuildConfig.coroutinesVersion
val kotlinVersion = BuildConfig.kotlinVersion
val serializationVersion = BuildConfig.serializationVersion
val testcontainersVersion = BuildConfig.testContainersVersion
val log4j2Version = "2.14.1"
val vertxVersion = BuildConfig.vertxVersion
val nettyVersion = BuildConfig.nettyVersion

plugins {
    java
    kotlin("jvm")
    id("info.solidsoft.pitest")
    `maven-publish`
    kotlin("plugin.serialization")
}



dependencies {
    implementation(enforcedPlatform("org.jetbrains.kotlin:kotlin-bom:$kotlinVersion"))
    implementation(enforcedPlatform("io.netty:netty-bom:$nettyVersion"))
//    implementation(enforcedPlatform("io.r2dbc:r2dbc-bom:Arabba-SR9"))
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))

    api("io.r2dbc:r2dbc-spi:0.8.5.RELEASE")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("io.r2dbc:r2dbc-pool:0.8.7.RELEASE")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:$coroutinesVersion")
    implementation("io.vertx:vertx-rx-java2:$vertxVersion")
    implementation("io.vertx:vertx-pg-client:$vertxVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-rx2:$coroutinesVersion")


    testImplementation("io.strikt:strikt-core:$striktVersion")
    testImplementation("dev.failgood:failgood:$failgoodVersion")


    testImplementation("com.christophsturm:randolf:0.2.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$serializationVersion")

    testImplementation("org.junit.platform:junit-platform-launcher:1.7.2")
    testRuntimeOnly("io.r2dbc:r2dbc-h2:0.8.4.RELEASE")
}
configure<JavaPluginConvention> { sourceCompatibility = JavaVersion.VERSION_1_8 }
val needsRedefinition = JavaVersion.current().ordinal >= JavaVersion.VERSION_13.ordinal
tasks {
    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "1.8"
        }
    }
    withType<Test> {
        enabled = false
    }
    create<Jar>("sourceJar") {
        from(sourceSets.main.get().allSource)
        archiveClassifier.set("sources")
    }
}
artifacts {
    add("archives", tasks["jar"])
    add("archives", tasks["sourceJar"])
}

plugins.withId("info.solidsoft.pitest") {
    configure<PitestPluginExtension> {
        //        verbose.set(true)
        if (needsRedefinition) {
            jvmArgs.set(listOf("-XX:+AllowRedefinitionToAddDeleteMethods", "-Xmx512m"))
            // need to set it on both. maybe the initial test run is done in the main process
            mainProcessJvmArgs.set(listOf("-XX:+AllowRedefinitionToAddDeleteMethods"))
        } else {
            jvmArgs.set(listOf("-Xmx512m"))
        }
        //        testPlugin.set("junit5")
        testPlugin.set("failgood")
        avoidCallsTo.set(setOf("kotlin.jvm.internal", "kotlin.Result"))
        targetClasses.set(setOf("io.the.orm.*")) //by default "${project.group}.*"
        excludedClasses.set(
            setOf(
                """io.the.orm.ResultMapperImpl${'$'}mapQueryResult*""",
                """io.the.orm.dbio.r2dbc.R2dbcStatement${'$'}executeBatch*""",
                """io.the.orm.dbio.vertx.VertxResult${'$'}map*"""
            )
        )
        targetTests.set(setOf("io.the.orm.*Test", "io.the.orm.**.*Test"))
        pitestVersion.set("1.6.2")
        threads.set(
            System.getenv("PITEST_THREADS")?.toInt() ?: Runtime.getRuntime().availableProcessors()
        )
        outputFormats.set(setOf("XML", "HTML"))
    }
}

val testMain = tasks.register("testMain", JavaExec::class) {
    main = "io.the.orm.test.AllTestsKt"
    classpath = sourceSets["test"].runtimeClasspath
    if (needsRedefinition)
        jvmArgs = mutableListOf("-XX:+AllowRedefinitionToAddDeleteMethods")
}
tasks.register("autoTest", JavaExec::class) {
    main = "io.the.orm.test.AutoTestKt"
    classpath = sourceSets["test"].runtimeClasspath
    if (needsRedefinition)
        jvmArgs = mutableListOf("-XX:+AllowRedefinitionToAddDeleteMethods")
}

tasks.check {
    dependsOn(testMain)
}

