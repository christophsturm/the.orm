package io.the.orm

import failgood.Test
import failgood.describe
import io.the.orm.test.Counters

@Test
object CountersTest {
    // this test only installs the after suite callback that prints the counters
    val context = describe("counters") {
        afterSuite {
            println(Counters)
        }
    }
}
