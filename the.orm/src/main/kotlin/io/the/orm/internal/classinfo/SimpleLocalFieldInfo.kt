package io.the.orm.internal.classinfo

import io.r2dbc.spi.Blob
import io.r2dbc.spi.Clob
import io.the.orm.OrmException
import io.vertx.sqlclient.data.Numeric
import java.math.BigDecimal
import java.nio.ByteBuffer
import java.time.LocalDate
import kotlin.reflect.KClass

internal val passThroughFieldConverter = PassThroughConverter

interface FieldConverter {
    fun dbValueToParameter(value: Any?): Any? = value

    fun propertyToDBValue(value: Any?): Any? = value
}

object PassThroughConverter : FieldConverter

// from the r2dbc spec: https://r2dbc.io/spec/0.8.4.RELEASE/spec/html/#datatypes
private val fieldConverters =
    mapOf<KClass<*>, FieldConverter>(
        String::class to passThroughFieldConverter,
        Clob::class to passThroughFieldConverter,
        Boolean::class to passThroughFieldConverter,
        ByteBuffer::class to passThroughFieldConverter,
        Blob::class to passThroughFieldConverter,
        Int::class to IntConverter,
        Byte::class to passThroughFieldConverter,
        Short::class to passThroughFieldConverter,
        Long::class to LongConverter,
        Double::class to DoubleConverter,
        BigDecimal::class to BigDecimalConverter,
        LocalDate::class to passThroughFieldConverter
    )

object IntConverter : FieldConverter {
    override fun dbValueToParameter(value: Any?): Int? {
        return (value as Number?)?.toInt()
    }
}

object LongConverter : FieldConverter {
    override fun dbValueToParameter(value: Any?): Long? {
        return (value as Number?)?.toLong()
    }
}

object BigDecimalConverter : FieldConverter {
    override fun dbValueToParameter(value: Any?): Any? {
        return if (value is Numeric) value.bigDecimalValue() else value
    }
}

object DoubleConverter : FieldConverter {
    override fun dbValueToParameter(value: Any?): Any? {
        return (value as Number?)?.toDouble()
    }
}

data class SimpleLocalFieldInfo(
    override val field: Field,
    override val dbFieldName: String,
    override val fieldConverter: FieldConverter,
    override val type: Class<*>,
    override val mutable: Boolean,
    override val name: String
) : ClassInfo.LocalFieldInfo {
    companion object {
        operator fun invoke(
            writer: Field,
            dbFieldName: String,
            kotlinClass: KClass<*>,
            javaClass: Class<*>,
            mutable: Boolean,
            name: String,
            otherClasses: Set<KClass<*>>
        ): SimpleLocalFieldInfo {
            val fieldConverter =
                fieldConverters[kotlinClass]
                    ?: throw OrmException(
                        "type ${kotlinClass.simpleName} not supported." +
                            " class: ${kotlinClass.simpleName}," +
                            " otherClasses: ${otherClasses.map { it.simpleName }}"
                    )
            return SimpleLocalFieldInfo(
                writer,
                dbFieldName,
                fieldConverter,
                javaClass,
                mutable,
                name
            )
        }
    }

    override fun valueForDb(instance: Any): Any? =
        fieldConverter.propertyToDBValue(field.property.call(instance))
}
