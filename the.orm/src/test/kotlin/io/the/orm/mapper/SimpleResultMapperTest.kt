package io.the.orm.mapper

import failgood.Test
import failgood.tests
import io.the.orm.exp.testing.MockDBResult
import io.the.orm.exp.testing.MockDBRow
import kotlinx.coroutines.flow.toList

@Test
object SimpleResultMapperTest {
    val tests = tests {
        it("works for classes without id") {
            data class Columns(
                val columnName: String,
            )
            SimpleResultMapper.forClass(Columns::class)
                .mapQueryResult(MockDBResult(listOf(MockDBRow(mapOf("column_name" to "blah")))))
                .toList()
        }
    }
}
