package community.flock.wirespec.integration.jackson

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.BeanDescription
import com.fasterxml.jackson.databind.DeserializationConfig
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import community.flock.wirespec.Wirespec
import java.io.IOException
import kotlin.reflect.full.primaryConstructor

/**
 * A Jackson module that handles deserialization of all Wirespec.Refined, to ensure
 * collapse / expanse of the wrapper class around the string value.
 *
 * Example
 * ```kt
 * data class Id(value: String): Wirespec.Refined
 * data class Task(id: Id, title: String)
 * ```
 *
 * Having an object such as
 * ```
 * Task{id: Id("123"), title: "improve API contracts"}
 * ```
 * will serialise to:
 * ```json
 * {id:"123", title: "improve API contracts"}
 * ```
 * flattening the Wirespec.Refined as a String. Conversely, such JSON will deserialize back
 * into the original `Task`, expanding the `id` field into a type safe Id data class.
 *
 * @see Wirespec.Refined
 */
class WirespecModule : SimpleModule() {
    init {
        addSerializer(Wirespec.Refined::class.java, RefinedSerializer())
        setDeserializerModifier(RefinedModifier())
    }
}

/**
 * Serializer that flattens any Wirespec.Refined wrapped String value during serialization.
 *
 * @see Wirespec.Refined
 * @see WirespecModule
 */
private class RefinedSerializer(x: Class<Wirespec.Refined>? = null) : StdSerializer<Wirespec.Refined>(x) {

    override fun serialize(value: Wirespec.Refined, gen: JsonGenerator, provider: SerializerProvider) {
        return gen.writeString(value.value)
    }
}

class RefinedDeserializer(val vc: Class<*>) : StdDeserializer<Wirespec.Refined>(vc) {
    @Throws(IOException::class, JsonProcessingException::class)
    override fun deserialize(jp: JsonParser, ctxt: DeserializationContext): Wirespec.Refined {
        val node = jp.codec.readTree<JsonNode>(jp)
        return vc.declaredConstructors.first().newInstance(node.asText()) as Wirespec.Refined
    }
}

private class RefinedModifier : BeanDeserializerModifier() {

    override fun modifyDeserializer(
        config: DeserializationConfig,
        beanDesc: BeanDescription,
        deserializer: JsonDeserializer<*>
    ): JsonDeserializer<*> {

        if (Wirespec.Refined::class.java.isAssignableFrom(beanDesc.beanClass)) {
            return super.modifyDeserializer(config, beanDesc, RefinedDeserializer(beanDesc.beanClass))
        }

        return super.modifyDeserializer(config, beanDesc, deserializer)
    }

}