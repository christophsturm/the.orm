@file:Suppress("NAME_SHADOWING")

package io.the.orm.internal

import failgood.Test
import failgood.describe
import strikt.api.expectThat
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.isEqualTo

@Test
class ClassInfoTest {
    val context = describe(ClassInfo::class) {
        data class Entity(val name: String, val id: Long? = null)

        val classInfo by lazy { ClassInfo(Entity::class, IDHandler(Entity::class), setOf()) }
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
        describe("iss on its way to support belongs to relations") {
            data class BelongsToEntity(val entity: Entity, val id: Long? = null)

            val classInfo = ClassInfo(BelongsToEntity::class, IDHandler(BelongsToEntity::class), setOf(Entity::class))
            val names = classInfo.fieldInfo.map { it.dbFieldName }

            it("knows field names and types for references") {
                expectThat(classInfo.fieldInfo.map { Pair(it.dbFieldName, it.type) })
                    .containsExactlyInAnyOrder(Pair("entity_id", Long::class.java), Pair("id", Long::class.java))
            }
            pending("know values for references") {
                expectThat(names.zip(classInfo.values(BelongsToEntity(Entity("name", 10))).toList()))
                    .containsExactlyInAnyOrder(Pair("name", "name"), Pair("id", 10))
            }

        }

    }
}
