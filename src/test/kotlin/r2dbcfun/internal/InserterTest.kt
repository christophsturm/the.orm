package r2dbcfun.internal

import failfast.describe

object InserterTest {
    val context = describe(Inserter::class) {
        data class Record(val name: String, val id: Long?)
//        Inserter("record", )
    }
}
