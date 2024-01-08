package io.the.orm.internal

import failgood.Test
import failgood.mock.mock
import failgood.tests
import io.the.orm.PKType
import io.the.orm.internal.classinfo.ClassInfo
import io.the.orm.internal.classinfo.Table

@Test
object SimpleInserterTest {
    val tests = tests {
        it("can insert") {
            data class Entity(val id: PKType, val name: String)

            val classInfo = ClassInfo(Entity::class)
            SimpleInserter(
                    classInfo.idHandler!!,
                    ExceptionInspector(Table("blah"), Entity::class),
                    classInfo
                )
                .create(mock(), EntityWrapper.fromClass(Entity(1, "blah")))
        }
    }
}
