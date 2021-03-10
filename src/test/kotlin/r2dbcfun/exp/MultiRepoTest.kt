package r2dbcfun.exp

import failfast.FailFast
import failfast.describe

fun main() {
    FailFast.runTest()
}

object MultiRepoTest {
    val context = describe(MultiRepo::class) {
        it("can be created with classes that reference each other") {
            MultiRepo(listOf(Page::class, Recipe::class))
        }
        it("can return a query factory for a class") {
            MultiRepo(listOf(Page::class, Recipe::class)).queryFactory<Page>()
        }
    }
}
