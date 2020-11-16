@file:Suppress("ConstantConditionIf")

import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import com.jfrog.bintray.gradle.BintrayExtension
import info.solidsoft.gradle.pitest.PitestPluginExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import r2dbcfun.ProjectConfig


val coroutinesVersion = "1.4.0"
val kotlinVersion = ProjectConfig.kotlinVersion
val serializationVersion = "1.0.1"
val testcontainersVersion = "1.15.0"
val log4j2Version = "2.13.3"
val kotestVersion = "4.3.1"

plugins {
    java
    kotlin("jvm").version(r2dbcfun.ProjectConfig.kotlinVersion)
    id("com.github.ben-manes.versions") version "0.34.0"
    id("info.solidsoft.pitest") version "1.5.2"
    `maven-publish`
    id("com.jfrog.bintray") version "1.8.5"
    kotlin("plugin.serialization").version(r2dbcfun.ProjectConfig.kotlinVersion)
    id("tech.formatter-kt.formatter") version "0.6.7"
    id("io.kotest") version "0.2.6"
}

group = "r2dbcfun"
version = "0.2"

repositories {
    if (ProjectConfig.eap) maven { setUrl("http://dl.bintray.com/kotlin/kotlin-eap") }
    if (ProjectConfig.useKotestSnapshot)
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
    jcenter()
    mavenCentral()
}

dependencies {
    implementation(enforcedPlatform("org.jetbrains.kotlin:kotlin-bom:$kotlinVersion"))
    implementation(enforcedPlatform("io.r2dbc:r2dbc-bom:Arabba-SR8"))
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))

    api("io.r2dbc:r2dbc-spi:0.8.3.RELEASE")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:$coroutinesVersion")
    testImplementation("io.strikt:strikt-core:0.28.0")

    testRuntimeOnly("io.r2dbc:r2dbc-h2:0.8.4.RELEASE")
    testRuntimeOnly("com.h2database:h2:1.4.200")
    testRuntimeOnly("org.postgresql:postgresql:42.2.18")
    testRuntimeOnly("io.r2dbc:r2dbc-postgresql:0.8.6.RELEASE")
    testImplementation("org.testcontainers:postgresql:$testcontainersVersion")
    testImplementation("org.flywaydb:flyway-core:7.1.1")

    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-debug:$coroutinesVersion")
    testImplementation("io.projectreactor.tools:blockhound:1.0.4.RELEASE")
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$serializationVersion")

    testImplementation("io.kotest:kotest-framework-engine-jvm:$kotestVersion")
    testImplementation("io.kotest:kotest-plugins-pitest:$kotestVersion")
    testImplementation("io.mockk:mockk:1.10.2")

    testImplementation("org.apache.logging.log4j:log4j-core:$log4j2Version")
    testImplementation("org.apache.logging.log4j:log4j-api:$log4j2Version")
    testImplementation("org.apache.logging.log4j:log4j-jul:$log4j2Version")
    testImplementation("org.apache.logging.log4j:log4j-slf4j-impl:$log4j2Version")
    //    "pitest"("org.pitest:pitest-junit5-plugin:0.12")
}
configure<JavaPluginConvention> { sourceCompatibility = JavaVersion.VERSION_1_8 }
kotlin { explicitApi() }
tasks {
    withType<KotlinCompile> { kotlinOptions.jvmTarget = "1.8" }
    withType<Test> {
        // for BlockHound https://github.com/reactor/BlockHound/issues/33
        @Suppress("UnstableApiUsage")
        if (JavaVersion.current().ordinal >= JavaVersion.VERSION_13.ordinal)
            jvmArgs = mutableListOf("-XX:+AllowRedefinitionToAddDeleteMethods")
//        ignoreFailures = System.getenv("CI") != null
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
        jvmArgs.set(listOf("-Xmx512m"))
        //        testPlugin.set("junit5")
        testPlugin.set("Kotest")
        avoidCallsTo.set(setOf("kotlin.jvm.internal", "kotlin.Result"))
        targetClasses.set(setOf("r2dbcfun.*")) //by default "${project.group}.*"
        excludedClasses.set(setOf("""r2dbcfun.ResultMapper${'$'}findBy*"""))
        targetTests.set(setOf("r2dbcfun.*Test", "r2dbcfun.**.*Test"))
        pitestVersion.set("1.5.2")
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
    if (!ProjectConfig.eap)
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
