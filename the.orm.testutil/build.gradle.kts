@file:Suppress("ConstantConditionIf")

import io.the.orm.BuildConfig
import io.the.orm.BuildConfig.failfastVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "io.the.orm"

val coroutinesVersion = BuildConfig.coroutinesVersion
val kotlinVersion = BuildConfig.kotlinVersion
val testcontainersVersion = BuildConfig.testContainersVersion
val vertxVersion = BuildConfig.vertxVersion
val nettyVersion = BuildConfig.nettyVersion

plugins {
    java
    kotlin("jvm")
}



dependencies {
    implementation(project(":the.orm"))
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    implementation("io.r2dbc:r2dbc-pool:0.8.7.RELEASE")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:$coroutinesVersion")

    implementation("io.vertx:vertx-rx-java2:$vertxVersion")

    implementation("io.vertx:vertx-pg-client:$vertxVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-rx2:$coroutinesVersion")
    implementation("com.christophsturm.failfast:failfast:$failfastVersion")

    implementation("org.testcontainers:postgresql:$testcontainersVersion")
}
configure<JavaPluginConvention> { sourceCompatibility = JavaVersion.VERSION_1_8 }
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
