@file:Suppress("ConstantConditionIf")

import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import com.jfrog.bintray.gradle.BintrayExtension
import info.solidsoft.gradle.pitest.PitestPluginExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import r2dbcfun.BuildConfig
import r2dbcfun.BuildConfig.failfastVersion

group = "r2dbcfun"
version = "0.2.2"

val coroutinesVersion = "1.4.2"
val kotlinVersion = BuildConfig.kotlinVersion
val serializationVersion = "1.1.0"
val testcontainersVersion = "1.15.2"
val log4j2Version = "2.14.0"
val vertxVersion = "4.0.2"
val byteBuddyVersion = "1.10.21"

plugins {
    java
    @Suppress("RemoveRedundantQualifierName")
    kotlin("jvm").version(r2dbcfun.BuildConfig.kotlinVersion)
    id("com.github.ben-manes.versions") version "0.36.0"
    id("info.solidsoft.pitest") version "1.5.2"
    `maven-publish`
    id("com.jfrog.bintray") version "1.8.5"
    @Suppress("RemoveRedundantQualifierName")
    kotlin("plugin.serialization").version(r2dbcfun.BuildConfig.kotlinVersion)
}


repositories {
    if (BuildConfig.eap) maven { setUrl("http://dl.bintray.com/kotlin/kotlin-eap") }
    maven { setUrl("https://oss.sonatype.org") }
    jcenter()
    mavenCentral()

}

dependencies {
    implementation(enforcedPlatform("org.jetbrains.kotlin:kotlin-bom:$kotlinVersion"))
    implementation(enforcedPlatform("io.r2dbc:r2dbc-bom:Arabba-SR8"))
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))

    api("io.r2dbc:r2dbc-spi:0.8.3.RELEASE")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:$coroutinesVersion")
    testImplementation("io.strikt:strikt-core:0.29.0")
    testImplementation("com.christophsturm.failfast:failfast:$failfastVersion")
    testImplementation("com.christophsturm.failfast:failfast-r2dbc:$failfastVersion")


    testRuntimeOnly("io.r2dbc:r2dbc-h2:0.8.4.RELEASE")
    testRuntimeOnly("com.h2database:h2:1.4.200")
    testRuntimeOnly("org.postgresql:postgresql:42.2.19")
    testRuntimeOnly("io.r2dbc:r2dbc-postgresql:0.8.6.RELEASE")
    testRuntimeOnly("io.r2dbc:r2dbc-pool:0.8.5.RELEASE")
//    testRuntimeOnly("io.projectreactor.netty:reactor-netty:0.9.14.RELEASE") // bump postgresql dependency

    testImplementation("org.testcontainers:postgresql:$testcontainersVersion")
    testImplementation("org.flywaydb:flyway-core:7.5.4")

    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-debug:$coroutinesVersion")
    testImplementation("io.projectreactor.tools:blockhound:1.0.4.RELEASE")
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$serializationVersion")

    testImplementation("io.mockk:mockk:1.10.6")
    testRuntimeOnly("net.bytebuddy:byte-buddy:$byteBuddyVersion")
    testRuntimeOnly("net.bytebuddy:byte-buddy-agent:$byteBuddyVersion")

    testImplementation("org.apache.logging.log4j:log4j-core:$log4j2Version")
    testImplementation("org.apache.logging.log4j:log4j-api:$log4j2Version")
    testImplementation("org.apache.logging.log4j:log4j-jul:$log4j2Version")
    testImplementation("org.apache.logging.log4j:log4j-slf4j-impl:$log4j2Version")
    testImplementation("io.vertx:vertx-rx-java2:$vertxVersion")

    testImplementation("io.vertx:vertx-pg-client:$vertxVersion")

    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-rx2:$coroutinesVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.7.1")

}
configure<JavaPluginConvention> { sourceCompatibility = JavaVersion.VERSION_1_8 }
kotlin { explicitApi() }
val needsRedefinition = JavaVersion.current().ordinal >= JavaVersion.VERSION_13.ordinal
tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
        kotlinOptions.useIR = true
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

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifact(tasks["sourceJar"])
            groupId = project.group as String
            artifactId = "r2dbcfun"
            version = project.version as String
        }
    }
}

// BINTRAY_API_KEY= ... ./gradlew clean build publish bintrayUpload
bintray {
    user = "christophsturm"
    key = System.getenv("BINTRAY_API_KEY")
    publish = true
    setPublications("mavenJava")
    pkg(
        delegateClosureOf<BintrayExtension.PackageConfig> {
            repo = "maven"
            name = "r2dbcfun"
            version(
                delegateClosureOf<BintrayExtension.VersionConfig> {
                    name = project.version as String
                }
            )
        }
    )
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
        targetClasses.set(setOf("r2dbcfun.*")) //by default "${project.group}.*"
        excludedClasses.set(setOf("""r2dbcfun.ResultMapper${'$'}findBy*"""))
        targetTests.set(setOf("r2dbcfun.*Test", "r2dbcfun.**.*Test"))
        pitestVersion.set("1.6.2")
        threads.set(
            System.getenv("PITEST_THREADS")?.toInt() ?: Runtime.getRuntime().availableProcessors()
        )
        outputFormats.set(setOf("XML", "HTML"))
    }
}

tasks.named<DependencyUpdatesTask>("dependencyUpdates") {
    val filtered =
        listOf("alpha", "beta", "rc", "cr", "m", "preview", "dev", "eap")
            .map { qualifier -> Regex("(?i).*[.-]$qualifier[.\\d-]*.*") }
    if (!BuildConfig.eap)
        resolutionStrategy {
            componentSelection {
                all {
                    if (filtered.any { it.matches(candidate.version) }) {
                        reject("Release candidate")
                    }
                }
            }
        }
    // optional parameters
    checkForGradleUpdate = true
    outputFormatter = "json"
    outputDir = "build/dependencyUpdates"
    reportfileName = "report"
}
tasks.wrapper { distributionType = Wrapper.DistributionType.ALL }

val testMain = task("testMain", JavaExec::class) {
    main = "r2dbcfun.test.AllTestsKt"
    classpath = sourceSets["test"].runtimeClasspath
    if (needsRedefinition)
        jvmArgs = mutableListOf("-XX:+AllowRedefinitionToAddDeleteMethods")
}

tasks.check {
    dependsOn(testMain)
}

