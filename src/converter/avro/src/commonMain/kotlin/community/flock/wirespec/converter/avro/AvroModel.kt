@file:OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)

package community.flock.wirespec.converter.avro

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

object AvroModel {
    @Serializable
    data class Field(
        val name: String,
        val type: TypeList,
        val doc: String? = null,
        val default: String? = null,
    )

    @Serializable(with = TypeListSerializer::class)
    class TypeList(vararg type: Type) : AbstractList<Type>() {
        constructor(type: List<Type>) : this(*type.toTypedArray())
        val list = type.toList()
        override val size = list.size
        override fun get(index: Int) = list[index]
    }

    @Serializable(with = TypeSerializer::class)
    sealed interface Type

    @Serializable
    data class SimpleType(
        val value: String,
    ) : Type

    @Serializable
    data class RecordType(
        val type: String,
        val name: String,
        val namespace: String? = null,
        val fields: List<Field>,
        val doc: String? = null,
    ) : Type

    @Serializable
    data class ArrayType(
        val type: String,
        val items: Type,
    ) : Type

    @Serializable
    data class MapType(
        val type: String,
        val values: Type,
    ) : Type

    @Serializable
    data class EnumType(
        val type: String,
        val name: String,
        val doc: String? = null,
        val symbols: List<String>,
    ) : Type

    @Serializable
    data class UnionType(
        val name: String,
        val type: TypeList,
    ) : Type

    @Serializable
    data class LogicalType(
        val type: String,
        val logicalType: String,
        val precision: Int? = null,
        val scale: Int? = null,
    ) : Type

    object TypeListSerializer : KSerializer<TypeList> {

        override val descriptor: SerialDescriptor = buildSerialDescriptor("TypeListSerializer", PolymorphicKind.SEALED)

        override fun serialize(encoder: Encoder, value: TypeList) {
            if (value.size > 1) {
                encoder.encodeSerializableValue(ListSerializer(Type.serializer()), value)
            } else {
                encoder.encodeSerializableValue(Type.serializer(), value.first())
            }
        }

        override fun deserialize(decoder: Decoder): TypeList {
            val input = decoder as? JsonDecoder ?: throw SerializationException("This class can be loaded only by Json")
            return when (val element = input.decodeJsonElement()) {
                is JsonPrimitive -> TypeList(input.json.decodeFromJsonElement(Type.serializer(), element))
                is JsonArray -> TypeList(
                    *element.map { input.json.decodeFromJsonElement(Type.serializer(), it) }
                        .toTypedArray(),
                )

                is JsonObject -> TypeList(input.json.decodeFromJsonElement(Type.serializer(), element))
            }
        }
    }

    object TypeSerializer : KSerializer<Type> {

        override val descriptor: SerialDescriptor = buildSerialDescriptor("TypeSerializer", PolymorphicKind.SEALED)

        override fun serialize(encoder: Encoder, value: Type) {
            when (value) {
                is SimpleType -> encoder.encodeSerializableValue(String.serializer(), value.value)
                is RecordType -> encoder.encodeSerializableValue(RecordType.serializer(), value)
                is ArrayType -> encoder.encodeSerializableValue(ArrayType.serializer(), value)
                is MapType -> encoder.encodeSerializableValue(MapType.serializer(), value)
                is EnumType -> encoder.encodeSerializableValue(EnumType.serializer(), value)
                is UnionType -> encoder.encodeSerializableValue(UnionType.serializer(), value)
                is LogicalType -> TODO()
            }
        }

        override fun deserialize(decoder: Decoder): Type {
            val input = decoder as? JsonDecoder ?: throw SerializationException("This class can be loaded only by Json")
            return when (val element = input.decodeJsonElement()) {
                is JsonObject -> when {
                    element.containsKey("items") -> input.json.decodeFromJsonElement(ArrayType.serializer(), element)
                    element.containsKey("fields") -> input.json.decodeFromJsonElement(RecordType.serializer(), element)
                    element.containsKey("symbols") -> input.json.decodeFromJsonElement(EnumType.serializer(), element)
                    element.containsKey("logicalType") -> input.json.decodeFromJsonElement(LogicalType.serializer(), element)
                    else -> TODO("Unknown object type: $element")
                }
                is JsonPrimitive -> SimpleType(element.content)
                is JsonArray -> TODO("Type can never be an array")
            }
        }
    }
}
