package io.the.orm.internal.classinfo

import io.the.orm.PK
import io.the.orm.exp.relations.BelongsTo
import io.the.orm.internal.IDHandler

internal class BelongsToConverter<Reference : Any>(private val idHandler: IDHandler<Reference>) : FieldConverter {
    override fun dbValueToParameter(value: Any?): Any? = null

    @Suppress("UNCHECKED_CAST")
    override fun propertyToDBValue(value: Any?): PK? {
        if (value == null) return null

        return if (value is BelongsTo<*>)
            when (value) {
                is BelongsTo.Auto<*> -> value.id
                is BelongsTo.BelongsToImpl<*> -> idHandler.readId(value.entity as Reference)
                is BelongsTo.BelongsToNotLoaded<*> -> value.pk
            }
        else idHandler.readId(value as Reference)
    }
}
