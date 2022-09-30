package io.the.orm.internal.classinfo

import io.the.orm.exp.relations.BelongsTo
import io.the.orm.internal.IDHandler

internal class BelongsToConverter<Reference : Any>(private val idHandler: IDHandler<Reference>) : FieldConverter {
    override fun dbValueToParameter(value: Any?): Any? = null

    @Suppress("UNCHECKED_CAST")
    override fun propertyToDBValue(value: Any?): Any? {
        return value?.let {
            val entity = if (it is BelongsTo<*>) (it as BelongsTo<Reference>).entity
            else
                it as Reference
            idHandler.readId(entity)
        }
    }
}
