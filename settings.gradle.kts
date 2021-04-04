pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}
rootProject.name = "the.orm.root"
include("the.orm", "the.orm.itest", "the.orm.testutil")
