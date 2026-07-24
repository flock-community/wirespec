package community.flock.wirespec.integration.kotest.generator

import io.kotest.property.Gen
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/** Compile-checked `(parent type, field name)` override via a property reference. */
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

/** JVM-side `RefinedWrapper` that wraps a drawn primitive into the matching single-arg Refined wrapper class. */
internal object JvmRefinedWrapper : RefinedWrapper {

    private object NotRefined

    private val cache = ConcurrentHashMap<KType, Any>()

    @Suppress("UNCHECKED_CAST")
    private fun ctorFor(type: KType): KFunction<Any>? = cache.getOrPut(type) {
        val cls = (type.classifier as? KClass<*>) ?: return@getOrPut NotRefined
        if (cls.java.isEnum) return@getOrPut NotRefined
        cls.constructors.singleOrNull()
            ?.takeIf { it.parameters.size == 1 }
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
