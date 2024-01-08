package io.the.orm.exp

import failgood.Ignored
import failgood.Test
import failgood.tests
import io.the.orm.PKType
import io.the.orm.internal.classinfo.ClassInfo
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.reflect.KClass
import kotlin.test.assertEquals

@Test
object SimpleTableCreatorTest {
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
        """create sequence users_id_seq no maxvalue;
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

    val tests = tests {
        it(
            "can create a create table statement for a class",
            ignored =
                Ignored.Because(
                    "seems like we need some way to define the database type (for example varchar or test)"
                )
        ) {
            assertEquals(SimpleTableCreator().createTable(User::class), USERS_SCHEMA)
        }
    }
}

class SimpleTableCreator {
    fun createTable(kClass: KClass<SimpleTableCreatorTest.User>): String {
        val ci = ClassInfo(kClass)
        ci.entityInfo.localFields.map { "${it.dbFieldName} ${it}" }
        return ""
    }
}
