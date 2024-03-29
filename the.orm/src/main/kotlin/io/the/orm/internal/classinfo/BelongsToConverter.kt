package io.the.orm.internal.classinfo

import io.the.orm.PKType
import io.the.orm.internal.IDHandler
import io.the.orm.relations.BelongsTo

internal class BelongsToConverter<Reference : Any>(private val idHandler: IDHandler<Reference>) :
    FieldConverter {
    override fun dbValueToParameter(value: Any?): Any = throw NotImplementedError()

    @Suppress("UNCHECKED_CAST")
    override fun propertyToDBValue(value: Any?): PKType? {
        // return the id of the relationship
        if (value == null) return null

        //
        return if (value is BelongsTo<*>)
            when (value) {
                is BelongsTo.AutoGetFromHasMany<*> -> value.id
                is BelongsTo.BelongsToImpl<*> -> idHandler.readId(value.entity as Reference)
                is BelongsTo.BelongsToNotLoaded<*> -> value.pk
            }
        else idHandler.readId(value as Reference)
    }
}
