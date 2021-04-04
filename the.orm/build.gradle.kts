@file:Suppress("ConstantConditionIf")

import info.solidsoft.gradle.pitest.PitestPluginExtension
import io.the.orm.BuildConfig
import io.the.orm.BuildConfig.failfastVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "io.the.orm"

val coroutinesVersion = "1.4.3"
val kotlinVersion = BuildConfig.kotlinVersion
val serializationVersion = "1.1.0"
val testcontainersVersion = "1.15.2"
val log4j2Version = "2.14.1"
val vertxVersion = "4.0.3"
val nettyVersion = "4.1.63.Final"

plugins {
    java
    kotlin("jvm")
    id("info.solidsoft.pitest")
    `maven-publish`
    kotlin("plugin.serialization")
}


repositories {
    jcenter()
    mavenCentral()
//    maven("https://oss.sonatype.org/content/repositories/comchristophsturmfailfast-1002/")
}

dependencies {
    implementation(enforcedPlatform("org.jetbrains.kotlin:kotlin-bom:$kotlinVersion"))
    implementation(enforcedPlatform("io.netty:netty-bom:$nettyVersion"))
    implementation(enforcedPlatform("io.r2dbc:r2dbc-bom:Arabba-SR9"))
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))


    api("io.r2dbc:r2dbc-spi:0.8.4.RELEASE")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("io.r2dbc:r2dbc-pool:0.8.6.RELEASE")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:$coroutinesVersion")

    implementation("io.vertx:vertx-rx-java2:$vertxVersion")

    implementation("io.vertx:vertx-pg-client:$vertxVersion")
    runtimeOnly("io.netty:netty-resolver-dns-native-macos:$nettyVersion:osx-x86_64")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-rx2:$coroutinesVersion")


    testImplementation("io.strikt:strikt-core:0.30.0")
    testImplementation("com.christophsturm.failfast:failfast:$failfastVersion")
//    testImplementation("com.christophsturm.failfast:failfast-r2dbc:$failfastVersion")


    testRuntimeOnly("io.r2dbc:r2dbc-h2:0.8.4.RELEASE")
    testRuntimeOnly("com.h2database:h2:1.4.200")
    testRuntimeOnly("org.postgresql:postgresql:42.2.19")
    testRuntimeOnly("io.r2dbc:r2dbc-postgresql:0.8.7.RELEASE")
    testImplementation("com.christophsturm:randolf:0.2.0")
//    testRuntimeOnly("io.projectreactor.netty:reactor-netty:0.9.14.RELEASE") // bump postgresql dependency

    testImplementation("org.testcontainers:postgresql:$testcontainersVersion")

    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-debug:$coroutinesVersion")
    testImplementation("io.projectreactor.tools:blockhound:1.0.5.RELEASE")
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$serializationVersion")

    testImplementation("org.apache.logging.log4j:log4j-core:$log4j2Version")
    testImplementation("org.apache.logging.log4j:log4j-api:$log4j2Version")
    testImplementation("org.apache.logging.log4j:log4j-jul:$log4j2Version")
    testImplementation("org.apache.logging.log4j:log4j-slf4j-impl:$log4j2Version")

    testImplementation("org.junit.platform:junit-platform-launcher:1.7.1")

}
configure<JavaPluginConvention> { sourceCompatibility = JavaVersion.VERSION_1_8 }
//kotlin { explicitApi() }
val needsRedefinition = JavaVersion.current().ordinal >= JavaVersion.VERSION_13.ordinal
tasks {
    withType<KotlinCompile> {
        kotlinOptions {
            useIR = true
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
        testPlugin.set("failfast")
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

