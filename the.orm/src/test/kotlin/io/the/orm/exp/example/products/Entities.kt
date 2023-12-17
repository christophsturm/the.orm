package io.the.orm.exp.example.products

import failgood.Test
import failgood.describe
import io.the.orm.RepoRegistry
import java.time.LocalDate

data class Product(val name: String, val gtin: String? = null, val id: Long?)
data class Shop(val name: String, val id: Long?)
data class ShopProduct(val shop: Shop, val product: Product, val localId: String, val id: Long?)
data class ProductOffer(val shopProduct: ShopProduct, val timestamp: LocalDate, val id: Long?)

@Test
object ShopTest {
    val tests = describe {
        it("works") {
            RepoRegistry(setOf(Product::class, Shop::class, ShopProduct::class, ProductOffer::class))
        }
    }
}
