package community.flock.wirespec.integration.jackson

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.BeanDescription
import com.fasterxml.jackson.databind.DeserializationConfig
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.cfg.MapperConfig
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod
import com.fasterxml.jackson.databind.introspect.AnnotatedParameter
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import community.flock.wirespec.Wirespec
import community.flock.wirespec.compiler.core.emit.JavaEmitter
import java.io.IOException


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

    override fun getModuleName(): String = "Wirespec Jackson Module"

    init {
        addSerializer(Wirespec.Refined::class.java, RefinedSerializer())
        addSerializer(Wirespec.Enum::class.java, EnumSerializer())
        setDeserializerModifier(WirespecDeserializerModifier())
        setNamingStrategy(JavaReservedKeywordNamingStrategy())
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

/**
 * Serializer Wirespec.Enum classes.
 *
 * @see Wirespec.Enum
 * @see WirespecModule
 */
private class EnumSerializer(x: Class<Wirespec.Enum>? = null) : StdSerializer<Wirespec.Enum>(x) {

    override fun serialize(value: Wirespec.Enum, gen: JsonGenerator, provider: SerializerProvider) {
        return gen.writeString(value.toString())
    }
}

/**
 * Deserializer Wirespec.Refined classes.
 *
 * @see Wirespec.Refined
 * @see WirespecModule
 */
class RefinedDeserializer(val vc: Class<*>) : StdDeserializer<Wirespec.Refined>(vc) {
    @Throws(IOException::class, JsonProcessingException::class)
    override fun deserialize(jp: JsonParser, ctxt: DeserializationContext): Wirespec.Refined {
        val node = jp.codec.readTree<JsonNode>(jp)
        return vc.declaredConstructors.first().newInstance(node.asText()) as Wirespec.Refined
    }
}

/**
 * Deserializer Wirespec.Enum classes.
 *
 * @see Wirespec.Enum
 * @see WirespecModule
 */
class EnumDeserializer(val vc: Class<*>) : StdDeserializer<Enum<*>>(vc) {
    @Throws(IOException::class, JsonProcessingException::class)
    override fun deserialize(jp: JsonParser, ctxt: DeserializationContext): Enum<*> {
        val node = jp.codec.readTree<JsonNode>(jp)
        val enum = vc.enumConstants.find {
            val toString = it.javaClass.getDeclaredMethod("toString")
            toString.invoke(it) == node.asText()
        }
        return enum as Enum<*>
    }
}

/**
 * Jackson modifier intercept the deserialization of Wirespec.Enum and Wirespec.Refined and modifies the deserializer
 *
 * @see Wirespec.Enum
 * @see WirespecModule
 */
private class WirespecDeserializerModifier : BeanDeserializerModifier() {
    override fun modifyEnumDeserializer(
        config: DeserializationConfig,
        type: JavaType,
        beanDesc: BeanDescription,
        deserializer: JsonDeserializer<*>
    ): JsonDeserializer<*> {
        if (Wirespec.Enum::class.java.isAssignableFrom(beanDesc.beanClass)) {
            return super.modifyDeserializer(config, beanDesc, EnumDeserializer(beanDesc.beanClass))
        }
        return super.modifyEnumDeserializer(config, type, beanDesc, deserializer)
    }

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

internal class JavaReservedKeywordNamingStrategy : PropertyNamingStrategy() {

    override fun nameForGetterMethod(config: MapperConfig<*>, method: AnnotatedMethod, defaultName: String): String {
        if (Record::class.java.isAssignableFrom(method.declaringClass)) {
            return translate(defaultName)
        }
        return defaultName
    }

    override fun nameForSetterMethod(config: MapperConfig<*>, method: AnnotatedMethod, defaultName: String): String {
        if (Record::class.java.isAssignableFrom(method.declaringClass)) {
            return translate(defaultName)
        }
        return defaultName
    }

    override fun nameForConstructorParameter(
        config: MapperConfig<*>,
        ctorParam: AnnotatedParameter,
        defaultName: String
    ): String {
        if (Record::class.java.isAssignableFrom(ctorParam.owner.rawType)) {
            return translate(defaultName)
        }
        return defaultName
    }

    private fun translate(property: String): String {
        val keywords = JavaEmitter.reservedKeywords.map { "_$it" }
        return if (property in keywords) {
            property.drop(1)
        } else {
            property
        }
    }
}

