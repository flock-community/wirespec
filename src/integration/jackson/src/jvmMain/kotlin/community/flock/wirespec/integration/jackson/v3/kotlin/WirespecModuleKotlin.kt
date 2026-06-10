package community.flock.wirespec.integration.jackson.v3.kotlin

import community.flock.wirespec.compiler.core.emit.Keywords
import community.flock.wirespec.emitters.kotlin.KotlinIdentifierEmitter
import community.flock.wirespec.kotlin.Wirespec
import tools.jackson.core.JsonGenerator
import tools.jackson.core.JsonParser
import tools.jackson.databind.BeanDescription
import tools.jackson.databind.DeserializationConfig
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.JavaType
import tools.jackson.databind.JsonNode
import tools.jackson.databind.PropertyNamingStrategy
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.ValueDeserializer
import tools.jackson.databind.cfg.MapperConfig
import tools.jackson.databind.deser.ValueDeserializerModifier
import tools.jackson.databind.deser.std.StdDeserializer
import tools.jackson.databind.introspect.AnnotatedMethod
import tools.jackson.databind.introspect.AnnotatedParameter
import tools.jackson.databind.module.SimpleModule
import tools.jackson.databind.ser.std.StdSerializer
import kotlin.reflect.KClass

/**
 * Jackson 3 variant of the Wirespec Kotlin module. It collapses / expands the wrapper
 * class around `Wirespec.Refined` string values and serializes `Wirespec.Enum` via
 * `toString()`, mirroring the Jackson 2 module.
 *
 * Unlike the Jackson 2 module, this module does **not** configure field visibility:
 * Jackson 3 mappers are immutable and a module cannot mutate visibility from
 * `setupModule`. Field visibility is applied on the mapper builder instead — see
 * [WirespecSerialization], which configures it for you. When registering this module
 * manually, configure visibility yourself, e.g.:
 *
 * ```kotlin
 * jsonMapper {
 *     addModule(kotlinModule())
 *     addModule(WirespecModuleKotlin())
 *     changeDefaultVisibility {
 *         it.withVisibility(PropertyAccessor.ALL, Visibility.NONE).withFieldVisibility(Visibility.ANY)
 *     }
 * }
 * ```
 *
 * @see Wirespec.Refined
 * @see WirespecSerialization
 */
class WirespecModuleKotlin : SimpleModule() {

    override fun getModuleName(): String = "Wirespec Jackson 3 Module for Kotlin"

    init {
        addSerializer(Wirespec.Refined::class.java, RefinedSerializer())
        addSerializer(Wirespec.Enum::class.java, EnumSerializer())
        setDeserializerModifier(WirespecDeserializerModifier())
        // Jackson 3 modules cannot register a PropertyNamingStrategy (no SetupContext
        // hook); apply KotlinReservedKeywordNamingStrategy on the mapper builder instead.
        // WirespecSerialization does this for you.
    }
}

/**
 * Serializer that flattens any Wirespec.Refined wrapped value during serialization.
 *
 * @see Wirespec.Refined
 * @see WirespecModuleKotlin
 */
private class RefinedSerializer(x: Class<Wirespec.Refined<*>>? = null) : StdSerializer<Wirespec.Refined<*>>(x) {
    override fun serialize(value: Wirespec.Refined<*>, gen: JsonGenerator, ctxt: SerializationContext) {
        gen.writePOJO(value.value)
    }
}

/**
 * Serializer for Wirespec.Enum classes.
 *
 * @see Wirespec.Enum
 * @see WirespecModuleKotlin
 */
private class EnumSerializer(x: Class<Wirespec.Enum>? = null) : StdSerializer<Wirespec.Enum>(x) {
    override fun serialize(value: Wirespec.Enum, gen: JsonGenerator, ctxt: SerializationContext) {
        gen.writeString(value.toString())
    }
}

/**
 * Deserializer for Wirespec.Refined classes.
 *
 * @see Wirespec.Refined
 * @see WirespecModuleKotlin
 */
private class RefinedDeserializer(private val vc: Class<*>) : StdDeserializer<Wirespec.Refined<*>>(vc) {
    override fun deserialize(jp: JsonParser, ctxt: DeserializationContext): Wirespec.Refined<*> {
        val node: JsonNode = ctxt.readTree(jp)
        val constructor = vc.declaredConstructors.first()
        val value = ctxt.readTreeAsValue(node, constructor.parameterTypes.first())
        return constructor.newInstance(value) as Wirespec.Refined<*>
    }
}

/**
 * Deserializer for Wirespec.Enum classes.
 *
 * @see Wirespec.Enum
 * @see WirespecModuleKotlin
 */
private class EnumDeserializer(private val vc: Class<*>) : StdDeserializer<Enum<*>>(vc) {
    override fun deserialize(jp: JsonParser, ctxt: DeserializationContext): Enum<*> {
        val node: JsonNode = ctxt.readTree(jp)
        return vc.enumConstants.find { it.javaClass.getDeclaredMethod("toString").invoke(it) == node.asString() } as Enum<*>
    }
}

/**
 * Jackson modifier that intercepts the deserialization of Wirespec.Enum and Wirespec.Refined
 * and swaps in the custom deserializers above.
 *
 * @see Wirespec.Enum
 * @see WirespecModuleKotlin
 */
private class WirespecDeserializerModifier : ValueDeserializerModifier() {
    override fun modifyEnumDeserializer(
        config: DeserializationConfig,
        type: JavaType,
        beanDescRef: BeanDescription.Supplier,
        deserializer: ValueDeserializer<*>,
    ): ValueDeserializer<*> = when (Wirespec.Enum::class.java.isAssignableFrom(beanDescRef.beanClass)) {
        true -> EnumDeserializer(beanDescRef.beanClass)
        false -> super.modifyEnumDeserializer(config, type, beanDescRef, deserializer)
    }

    override fun modifyDeserializer(
        config: DeserializationConfig,
        beanDescRef: BeanDescription.Supplier,
        deserializer: ValueDeserializer<*>,
    ): ValueDeserializer<*> = when (Wirespec.Refined::class.java.isAssignableFrom(beanDescRef.beanClass)) {
        true -> RefinedDeserializer(beanDescRef.beanClass)
        false -> super.modifyDeserializer(config, beanDescRef, deserializer)
    }
}

class KotlinReservedKeywordNamingStrategy : PropertyNamingStrategy() {

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
