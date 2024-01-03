@file:Suppress("unused")

package io.the.orm.exp.dynamic

import failgood.Test
import failgood.tests
import io.the.orm.exp.dynamic.Field.BelongsTo
import io.the.orm.exp.dynamic.Field.HasMany
import io.the.orm.exp.dynamic.Field.Text
import io.the.orm.exp.dynamic.Field.Timestamp

/**
 * A dynamic api that could be used by a webserver that offers CRUD for rest resources, for example
 * like remult
 */
@Test
class DynamicApiExperiment {
    val tests = tests {
        it("works") {
            // to handle the circular dependency, we first specify only one side of the relation,
            // in this example
            // the HasMany side, and when all the entities are created we add the other side
            val productPrice = Entity("Product Price", setOf(Timestamp("")))
            val product = Entity("Product", setOf(Text("name"), HasMany("price", productPrice)))
            val shop = Entity("Shop", setOf(Text("name"), HasMany("products", product)))
            productPrice.addField(BelongsTo("product", product))
            product.addField(BelongsTo("shop", shop))
        }
    }
}

sealed interface Field {
    val name: String

    data class Text(override val name: String) : Field

    data class Decimal(override val name: String) : Field

    data class Boolean(override val name: String) : Field

    data class Timestamp(override val name: String) : Field

    data class HasMany(override val name: String, val entity: Entity) : Field

    data class BelongsTo(override val name: String, val entity: Entity) : Field
}

class Entity(val name: String, fields: Set<Field>) {
    fun addField(field: Field) {
        realFields.add(field)
    }

    private val realFields = fields.toMutableSet()
}
