package io.the.orm.test.functional

import io.the.orm.PKType
import java.math.BigDecimal
import java.time.LocalDate

enum class Color {
    RED,
    @Suppress("unused") BLUE
}

data class User(
    val id: PKType? = null,
    val name: String,
    val email: String?,
    val isCool: Boolean? = false,
    val bio: String? = null,
    val favoriteColor: Color? = null,
    val birthday: LocalDate? = null,
    val weight: Double? = null,
    val balance: BigDecimal? = null
)

const val USERS_SCHEMA =
    """    create sequence users_id_seq no maxvalue;
    create table users
    (
        id             bigint       not null default nextval('users_id_seq') primary key,
        name           varchar(100) not null,
        email          varchar(100) unique,
        is_cool        boolean,
        bio            text,
        favorite_color varchar(10),
        birthday       date,
        weight         decimal(5, 2),
        balance        decimal(5, 2)
    );
"""
