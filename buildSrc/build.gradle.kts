plugins {
    `kotlin-dsl`
    `kotlin-dsl-precompiled-script-plugins`
    idea
}

repositories {
    gradlePluginPortal() // so that external plugins can be resolved in dependencies section
    mavenCentral()
}
dependencies {
    // hotfix to make kotlin scratch files work in idea
    implementation(kotlin("script-runtime"))
    implementation(kotlin("gradle-plugin", "1.9.22"))
    implementation("com.ncorti.ktfmt.gradle:plugin:0.17.0")
    implementation("com.adarshr:gradle-test-logger-plugin:4.0.0")
}

idea {
    module {
        generatedSourceDirs.add(File(layout.buildDirectory.get().asFile, "generated-sources/kotlin-dsl-accessors/kotlin"))
        generatedSourceDirs.add(File(layout.buildDirectory.get().asFile, "generated-sources/kotlin-dsl-plugins/kotlin"))
        generatedSourceDirs.add(File(layout.buildDirectory.get().asFile, "generated-sources/kotlin-dsl-external-plugin-spec-builders/kotlin"))
    }
}


tasks.withType<Jar> { duplicatesStrategy = DuplicatesStrategy.EXCLUDE }
