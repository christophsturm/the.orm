package io.the.orm.internal

import failgood.describe

object InserterTest {
    val context = describe(Inserter::class, disabled = true) {

        data class Record(val name: String, val id: Long?)
//        Inserter("record", )
    }
}
