package community.flock.wirespec.integration.kotest

import io.kotest.property.Gen
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * `(parent type, field name)` override registration using a property
 * reference. `KProperty1<Parent, V>` supplies the parent type (via
 * `typeOf<Parent>().toString()`) and the field name (via `property.name`)
 * in one compile-checked expression — a typo or rename becomes a compile
 * error. `V` (the property's declared type) is reflected in the call site
 * but the [factory] still returns `Gen<*>`: when the field is a Refined
 * wrapper, [JvmRefinedWrapper] auto-wraps the drawn inner primitive into
 * the wrapper class.
 *
 * Example:
 * ```
 * registerField(User::email) { Arb.email() }
 * registerField(User::age, value = 42L)
 * ```
 */
inline fun <reified Parent : Any, V> KotestWirespecGeneratorBuilder.registerField(
    property: KProperty1<Parent, V>,
    noinline factory: () -> Gen<*>,
) {
    registerFieldByTypeName(typeOf<Parent>().toString(), property.name, factory)
}

inline fun <reified Parent : Any, V> KotestWirespecGeneratorBuilder.registerField(
    property: KProperty1<Parent, V>,
    value: Any?,
) {
    registerFieldByTypeName(typeOf<Parent>().toString(), property.name, value)
}

/**
 * JVM-side `RefinedWrapper` that wraps a drawn primitive into the matching
 * single-arg Refined wrapper class. If the underlying [field] is not a
 * `KotestFieldShape<*>` with a single-ctor classifier, the drawn value is
 * passed through unchanged.
 */
internal object JvmRefinedWrapper : RefinedWrapper {

    // ConcurrentHashMap rejects null values, so non-wrappable types are
    // cached as the NotRefined sentinel instead.
    private object NotRefined

    private val cache = ConcurrentHashMap<KType, Any>()

    @Suppress("UNCHECKED_CAST")
    private fun ctorFor(type: KType): KFunction<Any>? = cache.getOrPut(type) {
        val cls = (type.classifier as? KClass<*>) ?: return@getOrPut NotRefined
        // Enums also have a single 1-arg constructor (the generated `label`), but it is
        // private/inaccessible and an enum is never a Refined wrapper — pinning an enum
        // field to a constant must pass the value through untouched, not try to re-wrap it.
        if (cls.java.isEnum) return@getOrPut NotRefined
        cls.constructors.singleOrNull()
            ?.takeIf { it.parameters.size == 1 }
            ?.let { it as? KFunction<Any> }
            ?: NotRefined
    } as? KFunction<Any>

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
