@file:Suppress("ConstantConditionIf")

import com.adarshr.gradle.testlogger.TestLoggerExtension
import com.adarshr.gradle.testlogger.theme.ThemeType.STANDARD_PARALLEL
import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import com.jfrog.bintray.gradle.BintrayExtension
import info.solidsoft.gradle.pitest.PitestPluginExtension
import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


val junit5Version = "5.6.2"
val junitPlatformVersion = "1.6.2"
val coroutinesVersion = if (ProjectConfig.eap) "1.3.7-1.4-M2" else "1.3.7"
val kotlinVersion = if (ProjectConfig.eap) "1.4-M2" else "1.3.72"

plugins {
    java
    kotlin("jvm").version(if (ProjectConfig.eap) "1.4-M2" else "1.3.72")
    id("com.github.ben-manes.versions") version "0.28.0"
    id("info.solidsoft.pitest") version "1.5.1"
    id("com.adarshr.test-logger") version "2.0.0"
    `maven-publish`
    id("com.jfrog.bintray") version "1.8.5"

}

group = "r2dbcfun"
version = "0.1-SNAPSHOT"


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
    testImplementation("io.strikt:strikt-core:0.26.1")
    testImplementation("dev.minutest:minutest:1.11.0")

    testRuntimeOnly("io.r2dbc:r2dbc-h2:0.8.4.RELEASE")
    testRuntimeOnly("com.h2database:h2:1.4.200")
    testRuntimeOnly("org.postgresql:postgresql:42.2.14")
    testRuntimeOnly("io.r2dbc:r2dbc-postgresql:0.8.3.RELEASE")
    testImplementation("org.testcontainers:postgresql:1.14.3")
    testImplementation("org.flywaydb:flyway-core:6.4.4")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junit5Version")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:$junitPlatformVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junit5Version")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-debug:$coroutinesVersion")
    testImplementation("io.projectreactor.tools:blockhound:1.0.3.RELEASE")

    "pitest"("org.pitest:pitest-junit5-plugin:0.12")

}
if (ProjectConfig.eap) {
    // set it here to apply only to production and not test compile
    val compileKotlin: KotlinCompile by tasks
    compileKotlin.kotlinOptions.freeCompilerArgs = listOf("-Xexplicit-api=strict")
}
configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
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

// BINTRAY_API_KEY= ... ./gradlew clean check publish bintrayUpload
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
        targetTests.set(setOf("r2dbcfun.*Test", "r2dbcfun.**.*Test"))
        pitestVersion.set("1.5.2")
        threads.set(System.getenv("PITEST_THREADS")?.toInt() ?: Runtime.getRuntime().availableProcessors())
        outputFormats.set(setOf("XML", "HTML"))
    }
}

tasks.named<DependencyUpdatesTask>("dependencyUpdates") {
    val filtered = listOf("alpha", "beta", "rc", "cr", "m", "preview")
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
