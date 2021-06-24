@file:Suppress("ConstantConditionIf")

import io.the.orm.BuildConfig
import io.the.orm.BuildConfig.blockHoundVersion
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
    kotlin("plugin.serialization")
}



dependencies {
    implementation(project(":the.orm"))
    implementation(project(":the.orm.testutil"))
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))


    implementation("io.r2dbc:r2dbc-pool:0.8.7.RELEASE")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:$coroutinesVersion")

    implementation("io.vertx:vertx-rx-java2:$vertxVersion")

    implementation("io.vertx:vertx-pg-client:$vertxVersion")
    runtimeOnly("io.netty:netty-resolver-dns-native-macos:$nettyVersion:osx-x86_64")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-rx2:$coroutinesVersion")


    testImplementation("io.strikt:strikt-core:$striktVersion")
    testImplementation("dev.failgood:failgood:$failgoodVersion")
//    testImplementation("com.christophsturm.failgood:failgood-r2dbc:$failgoodVersion")


    testRuntimeOnly("io.r2dbc:r2dbc-h2:0.8.4.RELEASE")
    testRuntimeOnly("com.h2database:h2:1.4.200")
    testRuntimeOnly("org.postgresql:postgresql:42.2.22")
    testRuntimeOnly("io.r2dbc:r2dbc-postgresql:0.8.8.RELEASE")
    testImplementation("com.christophsturm:randolf:0.2.1")
//    testRuntimeOnly("io.projectreactor.netty:reactor-netty:0.9.14.RELEASE") // bump postgresql dependency


    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-debug:$coroutinesVersion")
    testImplementation("io.projectreactor.tools:$blockHoundVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$serializationVersion")

    testImplementation("org.apache.logging.log4j:log4j-core:$log4j2Version")
    testImplementation("org.apache.logging.log4j:log4j-api:$log4j2Version")
    testImplementation("org.apache.logging.log4j:log4j-jul:$log4j2Version")
    testImplementation("org.apache.logging.log4j:log4j-slf4j-impl:$log4j2Version")

    testImplementation("org.junit.platform:junit-platform-launcher:1.7.2")
}
configure<JavaPluginConvention> { sourceCompatibility = JavaVersion.VERSION_1_8 }
//kotlin { explicitApi() }
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

val testMain = tasks.register("testMain", JavaExec::class) {
    main = "io.the.orm.test.functional.AllTestsKt"
    classpath = sourceSets["test"].runtimeClasspath
    if (needsRedefinition)
        jvmArgs = mutableListOf("-XX:+AllowRedefinitionToAddDeleteMethods")
}

tasks.check {
    dependsOn(testMain)
}

