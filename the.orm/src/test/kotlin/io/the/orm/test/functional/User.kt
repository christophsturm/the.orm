package io.the.orm.test.functional

import java.math.BigDecimal
import java.time.LocalDate

data class UserPK(override val id: Long) : io.the.orm.PK
enum class Color {
    RED,

    @Suppress("unused")
    BLUE
}

data class User(
    val id: UserPK? = null,
    val name: String,
    val email: String?,
    val isCool: Boolean? = false,
    val bio: String? = null,
    val favoriteColor: Color? = null,
    val birthday: LocalDate? = null,
    val weight: Double? = null,
    val balance: BigDecimal? = null
)
