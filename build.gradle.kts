import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import info.solidsoft.gradle.pitest.PitestPluginExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val junit5Version = "5.6.2"
val junitPlatformVersion = "1.6.2"
val coroutinesVersion = "1.3.5"


plugins {
    java
    kotlin("jvm") version "1.3.72"
    id("com.github.ben-manes.versions") version "0.28.0"
    id("info.solidsoft.pitest") version "1.5.0"
}

group = "r2dbcfun"
version = "0.1-SNAPSHOT"


repositories {
//    maven { setUrl("http://dl.bintray.com/kotlin/kotlin-eap") }
    jcenter()
    mavenCentral()
}

dependencies {
    implementation(enforcedPlatform("org.jetbrains.kotlin:kotlin-bom:1.3.72"))
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))

    implementation("io.r2dbc:r2dbc-spi:0.8.1.RELEASE")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:1.3.5")
    testImplementation("io.strikt:strikt-core:0.25.0")
    testImplementation("dev.minutest:minutest:1.11.0")

    testRuntimeOnly("io.r2dbc:r2dbc-h2:0.8.3.RELEASE")
    testRuntimeOnly("com.h2database:h2:1.4.200")
    testRuntimeOnly("org.postgresql:postgresql:42.2.12")
    testRuntimeOnly("io.r2dbc:r2dbc-postgresql:0.8.2.RELEASE")
    testImplementation("org.testcontainers:postgresql:1.14.1")
    testImplementation("org.flywaydb:flyway-core:6.4.1")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junit5Version")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:$junitPlatformVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junit5Version")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
    "pitest"("org.pitest:pitest-junit5-plugin:0.12")

}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<Test> {
    useJUnitPlatform {
        includeEngines("junit-jupiter")
    }
    testLogging {
        events("passed", "skipped", "failed")
    }
}
plugins.withId("info.solidsoft.pitest") {
    configure<PitestPluginExtension> {
        //        verbose.set(true)
        jvmArgs.set(listOf("-Xmx512m"))
        testPlugin.set("junit5")
        avoidCallsTo.set(setOf("kotlin.jvm.internal", "kotlin.Result"))
        targetClasses.set(setOf("r2dbcfun.*"))  //by default "${project.group}.*"
        targetTests.set(setOf("r2dbcfun.*Test", "r2dbcfun.**.*Test"))
        //pitestVersion.set("1.4.10")
        threads.set(System.getenv("PITEST_THREADS")?.toInt() ?: Runtime.getRuntime().availableProcessors())
        outputFormats.set(setOf("XML", "HTML"))
    }
}

tasks.named<DependencyUpdatesTask>("dependencyUpdates") {
    val filtered = listOf("alpha", "beta", "rc", "cr", "m", "preview")
        .map { qualifier -> Regex("(?i).*[.-]$qualifier[.\\d-]*.*") }
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

