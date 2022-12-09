package io.the.orm.internal.classinfo

import failgood.Test
import failgood.assert.containsExactlyInAnyOrder
import failgood.describe
import io.the.orm.PK
import io.the.orm.exp.relations.BelongsTo
import io.the.orm.exp.relations.HasMany
import io.the.orm.exp.relations.belongsTo
import strikt.api.expectThat
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.isEqualTo
import kotlin.test.assertNotNull

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
        describe("mutable fields") {
            data class Entity(val name: String, var mutableField: String, val id: Long? = null)

            val classInfo = ClassInfo(Entity::class, setOf())
            it("knows if a field is mutable") {
                assert(classInfo.localFieldInfo.singleOrNull { it.property == Entity::mutableField }?.mutable == true)
            }
            it("knows if a field is immutable") {
                assert(classInfo.localFieldInfo.singleOrNull { it.property == Entity::name }?.mutable == false)
            }
            it("stores all lists as arraylist as premature optimization") {
                assert(classInfo.localFieldInfo is ArrayList<*>)
                assert(classInfo.simpleFieldInfo is ArrayList<*>)
                assert(classInfo.belongsToRelations is ArrayList<*>)
                assert(classInfo.hasManyRelations is ArrayList<*>)
            }

        }
        describe("eager belongs to relations") {

            val classInfo = ClassInfo(Eager.UserGroup::class, setOf(Eager.User::class))

            it("knows field names and types for references") {
                assert(classInfo.localFieldInfo.map { Pair(it.dbFieldName, it.type) }
                    .containsExactlyInAnyOrder(Pair("user_id", Long::class.java), Pair("id", Long::class.java)))
            }
            it("knows values for references") {
                val values = classInfo.values(Eager.UserGroup(Eager.User("name", id = 10), id = 20))
                val names = classInfo.localFieldInfo.map { it.dbFieldName }
                assert(names.zip(values.toList()).containsExactlyInAnyOrder(Pair("id", 20L), Pair("user_id", 10L)))
            }
            it("knows if entity has relations") {
                assert(classInfo.hasBelongsToRelations)
            }
            it("separates fields and relations") {
                assert(classInfo.simpleFieldInfo.map { Pair(it.dbFieldName, it.type) }
                    .containsExactlyInAnyOrder(Pair("id", Long::class.java)))
                assert(classInfo.belongsToRelations.map { Pair(it.dbFieldName, it.type) }
                    .containsExactlyInAnyOrder(Pair("user_id", Long::class.java)))
            }
            it("indicates that the belongs to field does not support lazy") {
                val rel = assertNotNull(classInfo.belongsToRelations.singleOrNull())
                assert(!rel.canBeLazy)
            }
            it("indicates that the class can not be fetched without relations") {
                assert(!classInfo.canBeFetchedWithoutRelations)
            }

        }
        describe("lazy belongs to relations") {
            val classInfo = ClassInfo(Lazy.UserGroup::class, setOf(Lazy.User::class))

            it("knows field names and types for references") {
                assert(classInfo.localFieldInfo.map { Pair(it.dbFieldName, it.type) }
                    .containsExactlyInAnyOrder(Pair("user_id", Long::class.java), Pair("id", Long::class.java)))
            }
            it("knows values for references") {
                val values = classInfo.values(Lazy.UserGroup(belongsTo(Lazy.User("name", id = 10)), id = 20))
                val names = classInfo.localFieldInfo.map { it.dbFieldName }
                assert(names.zip(values.toList()).containsExactlyInAnyOrder(Pair("id", 20L), Pair("user_id", 10L)))
            }
            it("knows if entity has relations") {
                assert(classInfo.hasBelongsToRelations)
            }
            it("separates fields and relations") {
                assert(classInfo.simpleFieldInfo.map { Pair(it.dbFieldName, it.type) }
                    .containsExactlyInAnyOrder(Pair("id", Long::class.java)))
                assert(classInfo.belongsToRelations.map { Pair(it.dbFieldName, it.type) }
                    .containsExactlyInAnyOrder(Pair("user_id", Long::class.java)))
            }
            it("indicates that the belongs to field supports lazy") {
                val rel = assertNotNull(classInfo.belongsToRelations.singleOrNull())
                assert(rel.canBeLazy)
            }
            it("indicates that the class can be fetched without relations") {
                assert(classInfo.canBeFetchedWithoutRelations)
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
                describe("the field info") {
                    val rel = assertNotNull(ClassInfo(HolderOfNestedEntity::class).hasManyRelations.singleOrNull())
                    it("knows the class of the has many relation") {
                        assert(rel.relatedClass == NestedEntity::class)
                    }
                    it("always supports lazy") {
                        assert(rel.canBeLazy)
                    }
                }

            }
        }
    }
}
object Eager {
    data class UserGroup(val user: User, val id: Long? = null)
    data class User(val name: String, val groups: HasMany<UserGroup>? = null, val id: Long? = null)
}
object Lazy {
    data class UserGroup(val user: BelongsTo<User>, val id: Long? = null)
    data class User(val name: String, val groups: HasMany<UserGroup>? = null, val id: Long? = null)
}
