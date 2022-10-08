package io.the.orm.internal.classinfo

import failgood.Test
import failgood.assert.containsExactlyInAnyOrder
import failgood.describe
import io.the.orm.PK
import io.the.orm.exp.relations.HasMany
import strikt.api.expectThat
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.isEqualTo

@Test
class ClassInfoTest {
    val context = describe(ClassInfo::class, isolation = false) {

        describe("for a single class") {
            data class Entity(val name: String, val id: Long? = null)

            val classInfo = ClassInfo(Entity::class, setOf())
            it("knows the class name") {
                expectThat(classInfo.name).isEqualTo("Entity")
            }
            it("knows the field names and types") {
                expectThat(classInfo.localFieldInfo.map { Pair(it.dbFieldName, it.type) })
                    .containsExactlyInAnyOrder(Pair("name", String::class.java), Pair("id", Long::class.java))
            }
            it("can get field values") {
                val names = classInfo.localFieldInfo.map { it.dbFieldName }
                expectThat(names.zip(classInfo.values(Entity("name", null)).toList()))
                    .containsExactlyInAnyOrder(Pair("name", "name"), Pair("id", null))
            }
            it("know that it has no relations") {
                assert(!classInfo.hasBelongsToRelations)
            }
        }
        describe("belongs to relations") {
            val classInfo = ClassInfo(UserGroup::class, setOf(User::class))

            it("knows field names and types for references") {
                assert(classInfo.localFieldInfo.map { Pair(it.dbFieldName, it.type) }
                    .containsExactlyInAnyOrder(Pair("user_id", Long::class.java), Pair("id", Long::class.java)))
            }
            it("knows values for references") {
                val values = classInfo.values(UserGroup(User("name", id = 10), id = 20))
                val names = classInfo.localFieldInfo.map { it.dbFieldName }
                assert(names.zip(values.toList()).containsExactlyInAnyOrder(Pair("id", 20L), Pair("user_id", 10L)))
            }
            it("knows if entity has relations") {
                assert(classInfo.hasBelongsToRelations)
            }
            it("separates fields and relations") {
                assert(classInfo.fields.map { Pair(it.dbFieldName, it.type) }
                    .containsExactlyInAnyOrder(Pair("id", Long::class.java)))
                assert(classInfo.belongsToRelations.map { Pair(it.dbFieldName, it.type) }
                    .containsExactlyInAnyOrder(Pair("user_id", Long::class.java)))
            }
        }
        describe("has many relations") {
            describe("local has many relations") {
                data class NestedEntity(val name: String)
                data class HolderOfNestedEntity(
                    val name: String,
                    val nestedEntities: HasMany<NestedEntity>,
                    val id: PK? = null
                )
                it("works without specifying the nested entity as referenced class") {
                    ClassInfo(HolderOfNestedEntity::class)
                }
                it("knows if entity has hasMany relations") {
                    assert(ClassInfo(HolderOfNestedEntity::class).hasHasManyRelations)
                }
            }
        }
    }
}

data class UserGroup(val user: User, val id: Long? = null)
data class User(val name: String, val groups: HasMany<UserGroup>? = null, val id: Long? = null)
