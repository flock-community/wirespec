package community.flock.wirespec.examples.spring.config

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.BeanDescription
import com.fasterxml.jackson.databind.DeserializationConfig
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import community.flock.wirespec.kotlin.Wirespec

/**
 * Flattens `Wirespec.Refined` to its underlying scalar in JSON and reads it back
 * via the wrapper's single-arg constructor; serializes `Wirespec.Enum` by its
 * `label`. Mirrors the published `WirespecModuleKotlin` from `jackson-jvm`,
 * inlined here so the example doesn't depend on the runtime jar.
 */
class WirespecJacksonModule : SimpleModule() {

    override fun getModuleName(): String = "WirespecJacksonModule"

    init {
        addSerializer(Wirespec.Refined::class.java, RefinedSerializer)
        addSerializer(Wirespec.Enum::class.java, EnumSerializer)
        setDeserializerModifier(WirespecDeserializerModifier)
    }

    private object RefinedSerializer : StdSerializer<Wirespec.Refined<*>>(Wirespec.Refined::class.java) {
        override fun serialize(value: Wirespec.Refined<*>, gen: JsonGenerator, provider: SerializerProvider) =
            gen.writeObject(value.value)
    }

    private object EnumSerializer : StdSerializer<Wirespec.Enum>(Wirespec.Enum::class.java) {
        override fun serialize(value: Wirespec.Enum, gen: JsonGenerator, provider: SerializerProvider) =
            gen.writeString(value.label)
    }

    private class RefinedDeserializer(private val target: Class<*>) :
        StdDeserializer<Wirespec.Refined<*>>(target) {
        override fun deserialize(parser: JsonParser, ctx: DeserializationContext): Wirespec.Refined<*> {
            val node = parser.codec.readTree<JsonNode>(parser)
            val ctor = target.declaredConstructors.first()
            val rawValue = parser.codec.treeToValue(node, ctor.parameterTypes.first())
            return ctor.newInstance(rawValue) as Wirespec.Refined<*>
        }
    }

    private class EnumDeserializer(private val target: Class<*>) : StdDeserializer<Enum<*>>(target) {
        override fun deserialize(parser: JsonParser, ctx: DeserializationContext): Enum<*> {
            val text = parser.codec.readTree<JsonNode>(parser).asText()
            return target.enumConstants
                .map { it as Enum<*> }
                .first { (it as Wirespec.Enum).label == text }
        }
    }

    private object WirespecDeserializerModifier : BeanDeserializerModifier() {
        override fun modifyDeserializer(
            config: DeserializationConfig,
            beanDesc: BeanDescription,
            deserializer: JsonDeserializer<*>,
        ): JsonDeserializer<*> = if (Wirespec.Refined::class.java.isAssignableFrom(beanDesc.beanClass)) {
            RefinedDeserializer(beanDesc.beanClass)
        } else {
            deserializer
        }

        override fun modifyEnumDeserializer(
            config: DeserializationConfig,
            type: JavaType,
            beanDesc: BeanDescription,
            deserializer: JsonDeserializer<*>,
        ): JsonDeserializer<*> = if (Wirespec.Enum::class.java.isAssignableFrom(beanDesc.beanClass)) {
            EnumDeserializer(beanDesc.beanClass)
        } else {
            deserializer
        }
    }
}
