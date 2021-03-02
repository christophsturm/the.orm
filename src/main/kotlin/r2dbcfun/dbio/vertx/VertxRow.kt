package r2dbcfun.dbio.vertx

import io.vertx.reactivex.sqlclient.Row
import r2dbcfun.dbio.DBRow
import r2dbcfun.dbio.LazyResult

class VertxRow(val row: Row) : DBRow {
    override fun getLazy(key: String): LazyResult<Any?> {
        val result = get(key, Object::class.java)
        return LazyResult { result }
    }

    override fun <T> get(key: String, type: Class<T>): T? {
        return row.get(type, key)
    }

}
