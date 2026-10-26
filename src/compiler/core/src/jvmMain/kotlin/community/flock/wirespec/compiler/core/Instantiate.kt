package community.flock.wirespec.compiler.core

/**
 * Creates an emitter or IR extension known only by its class, filling every constructor parameter with the
 * argument of that type. The constructor with the most parameters that [arguments] can fill wins, so a value
 * given for a Kotlin default parameter is always used.
 */
public inline fun <reified T : Any> Class<*>.instantiate(vararg arguments: Any): T = arguments.toList()
    .let(::instantiate)
    .let { requireNotNull(it as? T) { "$name is not a ${T::class.java.name}" } }

@PublishedApi
internal fun Class<*>.instantiate(arguments: List<Any>): Any = constructors
    .mapNotNull { constructor ->
        constructor.parameterTypes
            .map { type -> arguments.firstOrNull(type::isInstance) }
            .takeIf { values -> values.all { it != null } }
            ?.let { constructor to it }
    }
    .maxByOrNull { (constructor) -> constructor.parameterCount }
    ?.let { (constructor, values) -> constructor.newInstance(*values.toTypedArray()) }
    ?: throw IllegalArgumentException("$name has no public constructor taking only ${arguments.joinToString { it::class.java.simpleName }}")
