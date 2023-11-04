package io.the.orm

import failgood.Test
import failgood.describe
import io.the.orm.test.Counters

@Test
object CountersTest {
    val contest = describe("counters") {
        afterSuite {
            println(Counters)
        }
    }
}
