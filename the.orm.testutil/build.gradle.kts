@file:Suppress("ConstantConditionIf", "GradlePackageUpdate")

import io.the.orm.versions.coroutinesVersion
import io.the.orm.versions.failgoodVersion
import io.the.orm.versions.testContainersVersion
import io.the.orm.versions.vertxVersion


plugins {
    java
    kotlin("jvm")
}



dependencies {
    implementation(project(":the.orm"))
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    implementation("io.r2dbc:r2dbc-pool:0.9.2.RELEASE")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:$coroutinesVersion")

    implementation("io.vertx:vertx-lang-kotlin-coroutines:$vertxVersion")
    implementation("io.vertx:vertx-pg-client:$vertxVersion")
    implementation("dev.failgood:failgood:$failgoodVersion")

    implementation("org.testcontainers:postgresql:$testContainersVersion")
}
