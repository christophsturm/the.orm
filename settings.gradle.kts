@file:Suppress("UnstableApiUsage")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://oss.sonatype.org/content/repositories/comchristophsturmfailfast-1006/")
    }
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
}
rootProject.name = "the.orm.root"
include("the.orm", "the.orm.itest", "the.orm.testutil")
//includeBuild("../failfast")
