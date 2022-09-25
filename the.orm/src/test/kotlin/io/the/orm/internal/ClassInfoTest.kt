package io.the.orm.internal

import failgood.Test
import failgood.describe
import io.the.orm.BelongsTo
import io.the.orm.HasOne
import strikt.api.expectThat
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.isEqualTo

@Test
class ClassInfoTest {
    val context = describe(ClassInfo::class, isolation = false) {

        describe("for a single class") {
            data class Entity(val name: String, val id: Long? = null)

            val classInfo = ClassInfo(Entity::class, IDHandler(Entity::class), setOf())
            it("knows the class name") {
                expectThat(classInfo.name).isEqualTo("Entity")
            }
            it("knows the field names and types") {
                expectThat(classInfo.fieldInfo.map { Pair(it.dbFieldName, it.type) })
                    .containsExactlyInAnyOrder(Pair("name", String::class.java), Pair("id", Long::class.java))
            }
            it("can get field values") {
                val names = classInfo.fieldInfo.map { it.dbFieldName }
                expectThat(names.zip(classInfo.values(Entity("name", null)).toList()))
                    .containsExactlyInAnyOrder(Pair("name", "name"), Pair("id", null))
            }
        }
        describe("has one to relations") {

            val classInfo = ClassInfo(UserGroup::class, IDHandler(UserGroup::class), setOf(User::class))
            val names = classInfo.fieldInfo.map { it.dbFieldName }

            it("knows field names and types for references") {
                expectThat(classInfo.fieldInfo.map { Pair(it.dbFieldName, it.type) })
                    .containsExactlyInAnyOrder(Pair("user_id", Long::class.java), Pair("id", Long::class.java))
            }
            ignore("know values for references") {
                expectThat(names.zip(classInfo.values(UserGroup(HasOne(User("name")))).toList()))
                    .containsExactlyInAnyOrder(Pair("name", "name"), Pair("id", 10))
            }
        }
    }
}

data class UserGroup(val user: HasOne<User>, val id: Long? = null)
data class User(val name: String, val groups: BelongsTo<UserGroup>? = null, val id: Long? = null)
