package community.flock.wirespec.integration.kotest

import io.kotest.property.Arb
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * Reified `(parent type, field name)` override registration. The parent's
 * Kotlin type is captured as `typeOf<Parent>().toString()` and matched
 * against the equivalent string the generator computes from
 * `KotestFieldShape.type` at lookup time. Both sides use the same Kotlin
 * stdlib `KType.toString()` representation, so the strings match.
 */
inline fun <reified Parent : Any> KotestWirespecGeneratorBuilder.registerField(
    name: String,
    noinline factory: () -> Arb<*>,
) {
    registerFieldByTypeName(typeOf<Parent>().toString(), name, factory)
}

inline fun <reified Parent : Any> KotestWirespecGeneratorBuilder.registerField(
    name: String,
    value: Any?,
) {
    registerFieldByTypeName(typeOf<Parent>().toString(), name, value)
}

/**
 * JVM-side `RefinedWrapper` that wraps a drawn primitive into the matching
 * single-arg Refined wrapper class. If the underlying [field] is not a
 * `KotestFieldShape<*>` with a single-ctor classifier, the drawn value is
 * passed through unchanged.
 */
internal object JvmRefinedWrapper : RefinedWrapper {

    private val cache = ConcurrentHashMap<KType, KFunction<Any>?>()

    @Suppress("UNCHECKED_CAST")
    private fun ctorFor(type: KType): KFunction<Any>? = cache.getOrPut(type) {
        val cls = (type.classifier as? KClass<*>) ?: return@getOrPut null
        cls.constructors.singleOrNull()
            ?.takeIf { it.parameters.size == 1 }
            ?.let { it as? KFunction<Any> }
    }

    override fun wrap(drawn: Any?, field: KotestField<*>, path: List<String>): Any? {
        val shape = field as? KotestFieldShape<*> ?: return drawn
        val ctor = ctorFor(shape.type) ?: return drawn
        return try {
            ctor.call(drawn)
        } catch (e: IllegalArgumentException) {
            error(
                "Override at ${path.joinToString("/")}: expected " +
                    "Arb<${ctor.parameters[0].type}> for refined " +
                    "${(shape.type.classifier as KClass<*>).qualifiedName}, " +
                    "got value of type ${drawn?.let { it::class.qualifiedName }}",
            )
        }
    }
}
