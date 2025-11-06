package community.flock.wirespec.integration.jackson.kotlin

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.BeanDescription
import com.fasterxml.jackson.databind.DeserializationConfig
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.cfg.MapperConfig
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod
import com.fasterxml.jackson.databind.introspect.AnnotatedParameter
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import community.flock.wirespec.compiler.core.emit.Keywords
import community.flock.wirespec.emitters.kotlin.KotlinIdentifierEmitter
import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.KClass

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
 * will serialize to:
 * ```json
 * {id:"123", title: "improve API contracts"}
 * ```
 * flattening the Wirespec.Refined as a String. Conversely, such JSON will deserialize back
 * into the original `Task`, expanding the `id` field into a type safe `Id` data class.
 *
 * @see Wirespec.Refined
 */
class WirespecModuleKotlin : SimpleModule() {

    override fun getModuleName(): String = "Wirespec Jackson Module for Kotlin"

    init {
        addSerializer(Wirespec.Refined::class.java, RefinedSerializer())
        addSerializer(Wirespec.Enum::class.java, EnumSerializer())
        setDeserializerModifier(WirespecDeserializerModifier())
        setNamingStrategy(KotlinReservedKeywordNamingStrategy())
    }

    override fun setupModule(context: SetupContext?) {
        super.setupModule(context)
        val owner: ObjectMapper? = context!!.getOwner()
        if (owner != null) {
            owner.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
            owner.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
        }
    }
}

/**
 * Serializer that flattens any Wirespec.Refined wrapped String value during serialization.
 *
 * @see Wirespec.Refined
 * @see WirespecModuleKotlin
 */
private class RefinedSerializer(x: Class<Wirespec.Refined>? = null) : StdSerializer<Wirespec.Refined>(x) {
    override fun serialize(value: Wirespec.Refined, gen: JsonGenerator, provider: SerializerProvider) = gen.writeString(value.value)
}

/**
 * Serializer Wirespec.Enum classes.
 *
 * @see Wirespec.Enum
 * @see WirespecModuleKotlin
 */
private class EnumSerializer(x: Class<Wirespec.Enum>? = null) : StdSerializer<Wirespec.Enum>(x) {
    override fun serialize(value: Wirespec.Enum, gen: JsonGenerator, provider: SerializerProvider) = gen.writeString(value.toString())
}

/**
 * Deserializer Wirespec.Refined classes.
 *
 * @see Wirespec.Refined
 * @see WirespecModuleKotlin
 */
private class RefinedDeserializer(private val vc: Class<*>) : StdDeserializer<Wirespec.Refined>(vc) {
    override fun deserialize(jp: JsonParser, ctxt: DeserializationContext): Wirespec.Refined = jp
        .codec
        .readTree<JsonNode>(jp)
        .run { vc.declaredConstructors.first().newInstance(asText()) as Wirespec.Refined }
}

/**
 * Deserializer Wirespec.Enum classes.
 *
 * @see Wirespec.Enum
 * @see WirespecModuleKotlin
 */
private class EnumDeserializer(private val vc: Class<*>) : StdDeserializer<Enum<*>>(vc) {
    override fun deserialize(jp: JsonParser, ctxt: DeserializationContext): Enum<*> = jp
        .codec
        .readTree<JsonNode>(jp)
        .run { vc.enumConstants.find { it.javaClass.getDeclaredMethod("toString").invoke(it) == asText() } as Enum<*> }
}

/**
 * Jackson modifier intercepts the deserialization of Wirespec.Enum and Wirespec.Refined and modifies the deserializer
 *
 * @see Wirespec.Enum
 * @see WirespecModuleKotlin
 */
private class WirespecDeserializerModifier : BeanDeserializerModifier() {
    override fun modifyEnumDeserializer(
        config: DeserializationConfig,
        type: JavaType,
        beanDesc: BeanDescription,
        deserializer: JsonDeserializer<*>,
    ): JsonDeserializer<*> = when (Wirespec.Enum::class.java.isAssignableFrom(beanDesc.beanClass)) {
        true -> super.modifyDeserializer(config, beanDesc, EnumDeserializer(beanDesc.beanClass))
        false -> super.modifyEnumDeserializer(config, type, beanDesc, deserializer)
    }

    override fun modifyDeserializer(
        config: DeserializationConfig,
        beanDesc: BeanDescription,
        deserializer: JsonDeserializer<*>,
    ): JsonDeserializer<*> = when (Wirespec.Refined::class.java.isAssignableFrom(beanDesc.beanClass)) {
        true -> super.modifyDeserializer(config, beanDesc, RefinedDeserializer(beanDesc.beanClass))
        false -> super.modifyDeserializer(config, beanDesc, deserializer)
    }
}

private class KotlinReservedKeywordNamingStrategy : PropertyNamingStrategy() {

    private fun translator(reserved: Keywords): String.() -> String = {
        val keywords = reserved.reservedKeywords.map { "_$it" }
        if (this in keywords) drop(1) else this
    }

    private val translate = translator(KotlinIdentifierEmitter)

    override fun nameForGetterMethod(config: MapperConfig<*>, method: AnnotatedMethod, defaultName: String): String = defaultName.translateIfDataClass(method.declaringClass.kotlin)

    override fun nameForSetterMethod(config: MapperConfig<*>, method: AnnotatedMethod, defaultName: String): String = defaultName.translateIfDataClass(method.declaringClass.kotlin)

    override fun nameForConstructorParameter(
        config: MapperConfig<*>,
        ctorParam: AnnotatedParameter,
        defaultName: String,
    ) = defaultName.translateIfDataClass(ctorParam.owner.rawType.kotlin)

    private fun String.translateIfDataClass(clazz: KClass<*>) = when (clazz.isData) {
        true -> translate()
        false -> this
    }
}
