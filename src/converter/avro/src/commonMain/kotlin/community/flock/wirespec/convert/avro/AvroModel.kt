@file:OptIn(InternalSerializationApi::class)

package community.flock.wirespec.convert.avro

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.SerializationStrategy
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
        val doc: String,
        val default: String? = null
    )

    @Serializable(with = TypeListSerializer::class)
    class TypeList(vararg type: Type) : AbstractList<Type>() {
        val list = type.toList()
        override val size = list.size
        override fun get(index: Int) = list.get(index)
    }

    @Serializable(with = TypeSerializer::class)
    sealed interface Type

    @Serializable
    data class SimpleType(
        val value: String
    ) : Type

    @Serializable
    data class RecordType(
        val type: String,
        val name: String,
        val namespace: String? = null,
        val fields: List<Field>,
        val doc: String,
    ) : Type

    @Serializable
    data class ArrayType(
        val type: String,
        val items: Type,
    ) : Type
    @Serializable
    data class EnumType(
        val type: String,
        val name: String,
        val doc: String,
        val symbols: List<String>,
    ) : Type


    object TypeListSerializer : KSerializer<TypeList> {

        override val descriptor: SerialDescriptor = buildSerialDescriptor("TypeListSerializer", PolymorphicKind.SEALED)

        override fun serialize(encoder: Encoder, value: TypeList) {
            encoder.encodeSerializableValue(TypeList.serializer(), value)
        }

        override fun deserialize(decoder: Decoder): TypeList {
            val input = decoder as? JsonDecoder ?: throw SerializationException("This class can be loaded only by Json")
            return when (val element = input.decodeJsonElement()) {
                is JsonPrimitive -> TypeList(input.json.decodeFromJsonElement(Type.serializer(), element))
                is JsonArray -> TypeList(*element.map { input.json.decodeFromJsonElement(Type.serializer(), it) }
                    .toTypedArray())

                is JsonObject -> TypeList(input.json.decodeFromJsonElement(Type.serializer(), element))
            }
        }
    }

    object TypeSerializer : KSerializer<Type> {

        override val descriptor: SerialDescriptor = buildSerialDescriptor("TypeSerializer", PolymorphicKind.SEALED)

        override fun serialize(encoder: Encoder, value: Type) {
            val serializer = when (value) {
                is SimpleType -> SimpleType.serializer()
                is RecordType -> RecordType.serializer()
                is ArrayType -> ArrayType.serializer()
                is EnumType -> EnumType.serializer()
            } as SerializationStrategy<Type>
            encoder.encodeSerializableValue(serializer, value)
        }

        override fun deserialize(decoder: Decoder): Type {
            val input = decoder as? JsonDecoder ?: throw SerializationException("This class can be loaded only by Json")
            return when (val element = input.decodeJsonElement()) {
                is JsonPrimitive -> SimpleType(element.content)
                is JsonObject -> when {
                    element.containsKey("items") -> input.json.decodeFromJsonElement(ArrayType.serializer(), element)
                    element.containsKey("fields") -> input.json.decodeFromJsonElement(RecordType.serializer(), element)
                    element.containsKey("symbols") -> input.json.decodeFromJsonElement(EnumType.serializer(), element)
                    else -> TODO("Unknown object type: ${element}")
                }

                is JsonArray -> TODO("Type can never be an array")
            }
        }
    }
}
