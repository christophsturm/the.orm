package io.the.orm

import failgood.describe
import io.the.orm.test.Counters

// @Test no need for the counters right now
object CountersTest {
    // this test only installs the after suite callback that prints the counters
    val context = describe("counters") { afterSuite { println(Counters) } }
}
