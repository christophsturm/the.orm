package io.the.orm.dbio.vertx

import io.the.orm.dbio.DBRow
import io.the.orm.dbio.LazyResult
import io.vertx.sqlclient.Row

class VertxRow(val row: Row) : DBRow {
    override fun getLazy(key: String): LazyResult<Any?> {
        val result = get(key, Object::class.java)
        return LazyResult { result }
    }

    override fun <T> get(key: String, type: Class<T>): T? {
        return row.get(type, key)
    }

}
