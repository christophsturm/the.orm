@file:Suppress("NAME_SHADOWING")

package io.the.orm.internal.classinfo

import failgood.Test
import failgood.assert.containsExactlyInAnyOrder
import failgood.tests
import io.the.orm.PKType
import io.the.orm.internal.EntityWrapper
import io.the.orm.relations.BelongsTo
import io.the.orm.relations.HasMany
import io.the.orm.relations.belongsTo
import kotlin.test.assertNotNull
import strikt.api.expectThat
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.isEqualTo

@Test
object ClassInfoTest {
    val context = tests {
        data class Entity(val name: String, var mutableField: String, val id: Long?)

        val classInfo = ClassInfo(Entity::class, setOf())
        val instance = EntityWrapper(Entity("nameValue", "mutableFieldValue", 42))
        describe("for a simple class without relations") {
            it("knows the class name") { expectThat(classInfo.name).isEqualTo("Entity") }
            it("knows the field names and types") {
                expectThat(classInfo.localFields.map { Pair(it.dbFieldName, it.type) })
                    .containsExactlyInAnyOrder(
                        Pair("name", String::class.java),
                        Pair("id", Long::class.java),
                        Pair("mutable_field", String::class.java)
                    )
            }
            it("can get field values") {
                val names = classInfo.localFields.map { it.dbFieldName }
                expectThat(names.zip(classInfo.values(instance).toList()))
                    .containsExactlyInAnyOrder(
                        Pair("name", "nameValue"),
                        Pair("id", 42L),
                        Pair("mutable_field", "mutableFieldValue")
                    )
            }
            it("has an id field") {
                val idField = assertNotNull(classInfo.idField)
                assert(idField.property.call(instance.entity) == 42L)
                assert(idField == classInfo.idFieldOrThrow())
            }
            it("know that it has no relations") { assert(!classInfo.hasBelongsToRelations) }
            describe("mutable fields") {
                it("knows if a field is mutable") {
                    assert(
                        classInfo.localFields
                            .singleOrNull { it.field.property == Entity::mutableField }
                            ?.mutable == true
                    )
                }
                it("knows if a field is immutable") {
                    assert(
                        classInfo.localFields
                            .singleOrNull { it.field.property == Entity::name }
                            ?.mutable == false
                    )
                }
            }
            it("stores all lists as arraylist as premature optimization") {
                assert(classInfo.localFields is ArrayList<*>)
                assert(classInfo.simpleFields is ArrayList<*>)
                assert(classInfo.belongsToRelations is ArrayList<*>)
                assert(classInfo.hasManyRelations is ArrayList<*>)
            }
        }
        describe("for a class with eager belongs to relations") {
            val classInfo = ClassInfo(Eager.UserGroup::class, setOf(Eager.User::class))

            it("knows field names and types for references") {
                assert(
                    classInfo.localFields
                        .map { Pair(it.dbFieldName, it.type) }
                        .containsExactlyInAnyOrder(
                            Pair("user_id", Long::class.java),
                            Pair("id", Long::class.java)
                        )
                )
            }
            it("knows values for references") {
                val values =
                    classInfo.values(
                        EntityWrapper(Eager.UserGroup(Eager.User("name", id = 10), id = 20))
                    )
                val names = classInfo.localFields.map { it.dbFieldName }
                assert(
                    names
                        .zip(values.toList())
                        .containsExactlyInAnyOrder(Pair("id", 20L), Pair("user_id", 10L))
                )
            }
            it("knows if entity has relations") { assert(classInfo.hasBelongsToRelations) }
            it("separates fields and relations") {
                assert(
                    classInfo.simpleFields
                        .map { Pair(it.dbFieldName, it.type) }
                        .containsExactlyInAnyOrder(Pair("id", Long::class.java))
                )
                assert(
                    classInfo.belongsToRelations
                        .map { Pair(it.dbFieldName, it.type) }
                        .containsExactlyInAnyOrder(Pair("user_id", Long::class.java))
                )
            }
            it("indicates that the belongs to field does not support lazy") {
                val rel = assertNotNull(classInfo.belongsToRelations.singleOrNull())
                assert(!rel.canBeLazy)
            }
            it("indicates that the class can not be fetched without relations") {
                assert(!classInfo.canBeFetchedWithoutRelations)
            }
        }
        describe("for a class with lazy belongs to relations") {
            val classInfo = ClassInfo(Lazy.UserGroup::class, setOf(Lazy.User::class))

            it("knows field names and types for references") {
                assert(
                    classInfo.localFields
                        .map { Pair(it.dbFieldName, it.type) }
                        .containsExactlyInAnyOrder(
                            Pair("user_id", Long::class.java),
                            Pair("id", Long::class.java)
                        )
                )
            }
            it("knows values for references") {
                val values =
                    classInfo.values(
                        EntityWrapper(
                            Lazy.UserGroup(belongsTo(Lazy.User("name", id = 10)), id = 20)
                        )
                    )
                val names = classInfo.localFields.map { it.dbFieldName }
                assert(
                    names
                        .zip(values.toList())
                        .containsExactlyInAnyOrder(Pair("id", 20L), Pair("user_id", 10L))
                )
            }
            it("knows if entity has relations") { assert(classInfo.hasBelongsToRelations) }
            it("separates fields and relations") {
                assert(
                    classInfo.simpleFields
                        .map { Pair(it.dbFieldName, it.type) }
                        .containsExactlyInAnyOrder(Pair("id", Long::class.java))
                )
                assert(
                    classInfo.belongsToRelations
                        .map { Pair(it.dbFieldName, it.type) }
                        .containsExactlyInAnyOrder(Pair("user_id", Long::class.java))
                )
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
                    val id: PKType? = null
                )
                it("works without specifying the nested entity as referenced class") {
                    ClassInfo(HolderOfNestedEntity::class)
                }
                it("knows if entity has hasMany relations") {
                    assert(ClassInfo(HolderOfNestedEntity::class).hasHasManyRelations)
                }
                describe("the field info") {
                    val rel =
                        assertNotNull(
                            ClassInfo(HolderOfNestedEntity::class).hasManyRelations.singleOrNull()
                        )
                    it("knows the class of the has many relation") {
                        assert(rel.relatedClass == NestedEntity::class)
                    }
                    it("always supports lazy") { assert(rel.canBeLazy) }
                }
            }
        }
    }

    // these data classes cannot be declared inside the context where they are used because they
    // have a circular dependency
    object Eager {
        data class UserGroup(val user: User, val id: Long? = null)

        data class User(
            val name: String,
            val groups: HasMany<UserGroup>? = null,
            val id: Long? = null
        )
    }

    object Lazy {
        data class UserGroup(val user: BelongsTo<User>, val id: Long? = null)

        data class User(
            val name: String,
            val groups: HasMany<UserGroup>? = null,
            val id: Long? = null
        )
    }
}
