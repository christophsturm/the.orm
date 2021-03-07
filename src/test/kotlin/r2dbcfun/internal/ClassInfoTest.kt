package r2dbcfun.internal

import failfast.FailFast
import failfast.describe
import strikt.api.expectThat
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.isEqualTo

fun main() {
    FailFast.runTest()
}
object ClassInfoTest {
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
        it("knows field names and types for references") {
            data class BelongsToEntity(val entity: Entity, val id: Long?)

            val classInfo = ClassInfo(BelongsToEntity::class, IDHandler(BelongsToEntity::class), setOf(Entity::class))
            expectThat(classInfo.fieldInfo.map { Pair(it.dbFieldName, it.type) })
                .containsExactlyInAnyOrder(Pair("entity_id", Long::class.java), Pair("id", Long::class.java))
        }

    }
}
