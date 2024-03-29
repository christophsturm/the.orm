import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.adarshr.gradle.testlogger.TestLoggerExtension
import com.adarshr.gradle.testlogger.theme.ThemeType.MOCHA_PARALLEL

plugins {
    kotlin("jvm")
    java
    `maven-publish`
    signing
    id("com.adarshr.test-logger")
    id("com.ncorti.ktfmt.gradle")
}
tasks.withType<Test> {
    useJUnitPlatform()
    outputs.upToDateWhen { false }
}

val pub = "mavenJava"

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "1.8"
        targetCompatibility = "1.8"
    }

    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "1.8"
            freeCompilerArgs = listOf("-opt-in=kotlin.RequiresOptIn", "-progressive")
            languageVersion = "1.9"
            apiVersion = "1.9"
        }
    }
}
java {
    withJavadocJar()
    withSourcesJar()
}


publishing {
    publications {
        create<MavenPublication>(pub) {
            from(components["java"])
            groupId = project.group as String
            artifactId = project.name
            version = project.version as String
            pom {
                description.set("rest without boilerplate")
                name.set("the.orm")
                url.set("https://github.com/christophsturm/the.orm")
                licenses {
                    license {
                        name.set("The MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        id.set("christophsturm")
                        name.set("Christoph Sturm")
                        email.set("me@christophsturm.com")
                    }
                }
                scm {
                    url.set("https://github.com/christophsturm/the.orm.git")
                }
            }
        }
    }
}
signing {
    sign(publishing.publications[pub])
}

configure<TestLoggerExtension> {
    theme = MOCHA_PARALLEL
    showSimpleNames = true
    showFullStackTraces = true
}

tasks.getByName("check").dependsOn(tasks.getByName("ktfmtCheck"))
ktfmt {
    kotlinLangStyle()
}
