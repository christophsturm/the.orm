@file:Suppress("ConstantConditionIf")

import com.adarshr.gradle.testlogger.TestLoggerExtension
import com.adarshr.gradle.testlogger.theme.ThemeType.STANDARD_PARALLEL
import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import com.jfrog.bintray.gradle.BintrayExtension
import info.solidsoft.gradle.pitest.PitestPluginExtension
import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


val junit5Version = "5.7.0"
val junitPlatformVersion = "1.7.0"
val coroutinesVersion = "1.3.9"
val kotlinVersion = "1.4.10"
val serializationVersion = "1.0.0"
val testcontainersVersion = "1.15.0-rc2"

plugins {
    java
    kotlin("jvm").version("1.4.10")
    id("com.github.ben-manes.versions") version "0.33.0"
    id("info.solidsoft.pitest") version "1.5.2"
    id("com.adarshr.test-logger") version "2.1.0"
    `maven-publish`
    id("com.jfrog.bintray") version "1.8.5"
    kotlin("plugin.serialization").version("1.4.10")


}

group = "r2dbcfun"
version = "0.1"


repositories {
    if (ProjectConfig.eap)
        maven { setUrl("http://dl.bintray.com/kotlin/kotlin-eap") }
    jcenter()
    mavenCentral()
}

dependencies {
    implementation(enforcedPlatform("org.jetbrains.kotlin:kotlin-bom:$kotlinVersion"))
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))

    api("io.r2dbc:r2dbc-spi:0.8.2.RELEASE")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:$coroutinesVersion")
    testImplementation("io.strikt:strikt-core:0.28.0")
    testImplementation("dev.minutest:minutest:1.11.0")

    testRuntimeOnly("io.r2dbc:r2dbc-h2:0.8.4.RELEASE")
    testRuntimeOnly("com.h2database:h2:1.4.200")
    testRuntimeOnly("org.postgresql:postgresql:42.2.17")
    testRuntimeOnly("io.r2dbc:r2dbc-postgresql:0.8.5.RELEASE")
    testImplementation("org.testcontainers:postgresql:$testcontainersVersion")
    testImplementation("org.flywaydb:flyway-core:7.0.2")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junit5Version")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:$junitPlatformVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junit5Version")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-debug:$coroutinesVersion")
    testImplementation("io.projectreactor.tools:blockhound:1.0.4.RELEASE")
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$serializationVersion")

    val log4j2Version = "2.13.3"
    testImplementation("org.apache.logging.log4j:log4j-core:$log4j2Version")
    testImplementation("org.apache.logging.log4j:log4j-api:$log4j2Version")
    testImplementation("org.apache.logging.log4j:log4j-jul:$log4j2Version")
    testImplementation("org.apache.logging.log4j:log4j-slf4j-impl:$log4j2Version")

    "pitest"("org.pitest:pitest-junit5-plugin:0.12")

}
configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}
kotlin {
    explicitApi()
}
tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }
    withType<Test> {
        // for BlockHound https://github.com/reactor/BlockHound/issues/33
        @Suppress("UnstableApiUsage")
        if (JavaVersion.current().ordinal >= JavaVersion.VERSION_13.ordinal)
            jvmArgs = mutableListOf("-XX:+AllowRedefinitionToAddDeleteMethods")
        useJUnitPlatform {
            includeEngines("junit-jupiter")
        }

        ignoreFailures = System.getenv("CI") != null
        testLogging {
            testLogging {
                exceptionFormat = FULL
            }
        }
        maxParallelForks = Runtime.getRuntime().availableProcessors() / 2
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
    pkg(delegateClosureOf<BintrayExtension.PackageConfig> {
        repo = "maven"
        name = "r2dbcfun"
        version(delegateClosureOf<BintrayExtension.VersionConfig> {
            name = project.version as String
        })
    })
}


plugins.withId("info.solidsoft.pitest") {
    configure<PitestPluginExtension> {
        //        verbose.set(true)
        jvmArgs.set(listOf("-Xmx512m"))
        testPlugin.set("junit5")
        avoidCallsTo.set(setOf("kotlin.jvm.internal", "kotlin.Result"))
        targetClasses.set(setOf("r2dbcfun.*"))  //by default "${project.group}.*"
        excludedClasses.set(setOf("""r2dbcfun.Finder${'$'}findBy*"""))
        targetTests.set(setOf("r2dbcfun.*Test", "r2dbcfun.**.*Test"))
        pitestVersion.set("1.5.2")
        threads.set(System.getenv("PITEST_THREADS")?.toInt() ?: Runtime.getRuntime().availableProcessors())
        outputFormats.set(setOf("XML", "HTML"))
    }
}

tasks.named<DependencyUpdatesTask>("dependencyUpdates") {
    val filtered = listOf("alpha", "beta", "rc", "cr", "m", "preview", "dev", "eap")
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
tasks.wrapper {
    distributionType = Wrapper.DistributionType.ALL
}

configure<TestLoggerExtension> {
    theme = STANDARD_PARALLEL
    showSimpleNames = true
}
