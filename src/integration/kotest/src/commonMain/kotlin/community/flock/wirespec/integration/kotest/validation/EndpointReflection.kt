package community.flock.wirespec.integration.kotest.validation

import community.flock.wirespec.kotlin.Wirespec
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.lang.reflect.Parameter
import java.lang.reflect.ParameterizedType
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

@PublishedApi
internal class EndpointReflection private constructor(
    val endpointName: String,
    val responseVariantsByStatus: Map<Int, Class<*>>,
    private val fromResponseMethod: Method,
    private val instance: Any,
    val requestConstructor: Constructor<*>,
    private val requestConstructorParamNames: List<String>,
    val hasBody: Boolean,
    val bodyElementClass: Class<*>?,
) {

    private val variantCache = ConcurrentHashMap<Class<*>, ResponseVariantReflection>()

    fun responseClassForStatus(status: Int): Class<*>? = responseVariantsByStatus[status]

    /** Reflect (and cache) a response variant's flattened constructor so a random instance can be built. */
    fun responseVariant(variantClass: Class<*>): ResponseVariantReflection = variantCache.getOrPut(variantClass) { introspectVariant(variantClass) }

    /** How to build one `Response<status>` variant: its flattened constructor plus the body's shape. */
    class ResponseVariantReflection(
        val constructor: Constructor<*>,
        val hasBody: Boolean,
        val bodyElementClass: Class<*>?,
        val bodyClass: Class<*>?,
    )

    fun fromRawResponse(serialization: Wirespec.Serialization, response: Wirespec.RawResponse): Any = fromResponseMethod.invoke(instance, serialization, response)

    fun buildRequest(args: Map<String, Any?>): Any {
        val ordered = requestConstructorParamNames.map { name ->
            if (!args.containsKey(name)) {
                error(
                    "$endpointName.Request constructor parameter `$name` was not supplied. " +
                        "Set it on the EndpointCallBuilder via .path/.body/.query/.header.",
                )
            }
            args[name]
        }
        return try {
            requestConstructor.newInstance(*ordered.toTypedArray())
        } catch (t: Throwable) {
            error("Failed to build $endpointName.Request with args=$args: ${t.cause?.message ?: t.message}")
        }
    }

    companion object {
        private val cache = ConcurrentHashMap<KClass<out Wirespec.Endpoint>, EndpointReflection>()
        private val syntheticParamRegex = Regex("arg\\d+")

        /** Matches a Wirespec response-variant simple name (`Response<NNN>`, e.g. `Response200`). */
        private val responseVariantRegex = Regex("Response(\\d{3})")

        /** Pick the emitter's flattened secondary constructor of [cls], omitting the [excludeParam] param. */
        private fun pickEmitterConstructor(cls: Class<*>, excludeParam: String, label: String): Constructor<*> {
            val ctor = cls.declaredConstructors
                .filter { c -> c.parameters.none { it.name == excludeParam } }
                .minByOrNull { it.parameterCount }
                ?: cls.declaredConstructors.minByOrNull { it.parameterCount }
                ?: error("$label: no constructors found.")
            require(ctor.parameters.all { it.name != null && !it.name.matches(syntheticParamRegex) }) {
                "$label constructor parameter names not retained. " +
                    "Ensure the generated module is compiled with `-java-parameters`."
            }
            return ctor
        }

        @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
        private fun Parameter.isListType(): Boolean = java.util.List::class.java.isAssignableFrom(type)

        /** The element `Class<*>` of a `List<…>` body/param, or `null` when it isn't a list. */
        private fun Parameter.listElementClassOrNull(): Class<*>? = takeIf { it.isListType() }
            ?.let { (parameterizedType as? ParameterizedType)?.actualTypeArguments?.firstOrNull() as? Class<*> }

        private fun introspectVariant(variantClass: Class<*>): ResponseVariantReflection {
            val ctor = pickEmitterConstructor(variantClass, excludeParam = "status", label = variantClass.simpleName ?: variantClass.name)
            val bodyParam = ctor.parameters.firstOrNull { it.name == "body" }
            return ResponseVariantReflection(
                constructor = ctor,
                hasBody = bodyParam != null,
                bodyElementClass = bodyParam?.listElementClassOrNull(),
                bodyClass = bodyParam?.takeIf { !it.isListType() }?.type,
            )
        }

        fun of(endpoint: Wirespec.Endpoint): EndpointReflection = cache.getOrPut(endpoint::class) { introspect(endpoint, endpoint::class) }

        private fun introspect(instance: Wirespec.Endpoint, cls: KClass<out Wirespec.Endpoint>): EndpointReflection {
            val jcls = cls.java
            val requestClass = jcls.declaredClasses.firstOrNull { it.simpleName == "Request" }
                ?: error("${cls.simpleName}: no nested Request type found.")

            val variants: Map<Int, Class<*>> = jcls.declaredClasses.mapNotNull { c ->
                val status = responseVariantRegex.matchEntire(c.simpleName)?.groupValues?.get(1)?.toInt() ?: return@mapNotNull null
                status to c
            }.toMap()
            require(variants.isNotEmpty()) { "${cls.simpleName}: no concrete ResponseNNN variants found." }

            val fromResponseMethod = jcls.declaredMethods
                .firstOrNull { it.name == "fromResponse" || it.name == "fromRawResponse" }
                ?: error("${cls.simpleName}: no fromResponse/fromRawResponse method found.")

            val requestConstructor = pickEmitterConstructor(requestClass, excludeParam = "method", label = "${cls.simpleName}.Request")
            val paramNames = requestConstructor.parameters.map { it.name!! }
            val bodyParam = requestConstructor.parameters.firstOrNull { it.name == "body" }

            return EndpointReflection(
                endpointName = cls.simpleName ?: jcls.name,
                responseVariantsByStatus = variants,
                fromResponseMethod = fromResponseMethod,
                instance = instance,
                requestConstructor = requestConstructor,
                requestConstructorParamNames = paramNames,
                hasBody = bodyParam != null,
                bodyElementClass = bodyParam?.listElementClassOrNull(),
            )
        }
    }
}
