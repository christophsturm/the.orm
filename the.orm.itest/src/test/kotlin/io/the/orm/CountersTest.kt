package io.the.orm

import failgood.tests
import io.the.orm.test.Counters

// @Test no need for the counters right now
object CountersTest {
    // this test only installs the after suite callback that prints the counters
    val context = tests { afterSuite { println(Counters) } }
}
