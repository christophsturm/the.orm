@file:Suppress("unused")

package io.the.orm.exp

/* interface based api, to support inheritance.
 *
 * currently unsure how to best create entities with that api, we will need some kind of impl or factory method.
 */

object Entities {
    interface Loginable {
        val userName: String
        val hashedPassword: String
    }

    interface Likable {
        val likedBy: List<Liker>
    }

    interface Liker

    @Entity interface User : Loginable, Likable, Liker
}

annotation class Entity
