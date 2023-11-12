@file:Suppress("UNUSED_VARIABLE", "unused")

package io.the.orm.exp.dynamic

import failgood.Test
import failgood.describe
import io.the.orm.exp.dynamic.Field.*

@Test
class DynamicApiExperiment {
    val tests = describe("dynamic api") {
        val product = Entity("Product", setOf(Text("name")))
        val shop = Entity("Shop", setOf(Text("name")))
        val productPrice = Entity("Product Price", setOf(Timestamp("")))
    }
}
sealed interface Field {
    val name: String
    data class Text(override val name: String) : Field
    data class Decimal(override val name: String) : Field
    data class Boolean(override val name: String) : Field
    data class Timestamp(override val name: String) : Field
}
data class Entity(val name: String, val fields: Set<Field>)
