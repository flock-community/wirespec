package community.flock.wirespec.integration.kotest.validation

import community.flock.wirespec.kotlin.Wirespec
import java.lang.reflect.Constructor
import java.lang.reflect.Method
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

    /**
     * How to build one `Response<status>` variant: its user-facing constructor (the emitter's
     * secondary, flattening header fields + `body`, with no `status` param) plus the body's
     * shape — [bodyClass] for an object body, [bodyElementClass] for a `List<…>` body.
     */
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
        private val variantRegex = Regex("Response(\\d{3})")
        private val syntheticParamRegex = Regex("arg\\d+")

        // Pick the emitter's flattened response-variant constructor: the secondary that omits the
        // primary's `status` param and flattens response header fields + `body`. Fewest params as a
        // tiebreak; fall back to any constructor for hand-rolled variants.
        @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
        private fun introspectVariant(variantClass: Class<*>): ResponseVariantReflection {
            val ctor = variantClass.declaredConstructors
                .filter { c -> c.parameters.none { it.name == "status" } }
                .minByOrNull { it.parameterCount }
                ?: variantClass.declaredConstructors.minByOrNull { it.parameterCount }
                ?: error("${variantClass.simpleName}: no constructors found.")
            require(ctor.parameters.all { it.name != null && !it.name.matches(syntheticParamRegex) }) {
                "${variantClass.simpleName} constructor parameter names not retained. " +
                    "Ensure the generated module is compiled with `-java-parameters`."
            }
            val bodyParam = ctor.parameters.firstOrNull { it.name == "body" }
            val isListBody = bodyParam != null && java.util.List::class.java.isAssignableFrom(bodyParam.type)
            val bodyElementClass: Class<*>? = bodyParam
                ?.takeIf { isListBody }
                ?.let { (it.parameterizedType as? ParameterizedType)?.actualTypeArguments?.firstOrNull() as? Class<*> }
            return ResponseVariantReflection(
                constructor = ctor,
                hasBody = bodyParam != null,
                bodyElementClass = bodyElementClass,
                bodyClass = bodyParam?.takeIf { !isListBody }?.type,
            )
        }

        fun of(endpoint: Wirespec.Endpoint): EndpointReflection = cache.getOrPut(endpoint::class) { introspect(endpoint, endpoint::class) }

        private fun introspect(instance: Wirespec.Endpoint, cls: KClass<out Wirespec.Endpoint>): EndpointReflection {
            val jcls = cls.java
            val requestClass = jcls.declaredClasses.firstOrNull { it.simpleName == "Request" }
                ?: error("${cls.simpleName}: no nested Request type found.")

            val variants: Map<Int, Class<*>> = jcls.declaredClasses.mapNotNull { c ->
                val match = c.simpleName?.let(variantRegex::matchEntire) ?: return@mapNotNull null
                match.groupValues[1].toInt() to c
            }.toMap()
            require(variants.isNotEmpty()) { "${cls.simpleName}: no concrete ResponseNNN variants found." }

            val fromResponseMethod = jcls.declaredMethods
                .firstOrNull { it.name == "fromResponse" || it.name == "fromRawResponse" }
                ?: error("${cls.simpleName}: no fromResponse/fromRawResponse method found.")

            // Pick the user-facing secondary Request constructor: the emitter's
            // secondary flattens path/query/header fields and never declares the
            // primary's `method` parameter. Fewest-params alone is not enough — an
            // endpoint with many flattened params (e.g. 1 path + 6 queries) has a
            // secondary LARGER than the 5-arg primary. Fall back to fewest-params for
            // hand-rolled Request classes without retained parameter names.
            val requestConstructor = requestClass.declaredConstructors
                .filter { ctor -> ctor.parameters.none { it.name == "method" } }
                .minByOrNull { it.parameterCount }
                ?: requestClass.declaredConstructors.minByOrNull { it.parameterCount }
                ?: error("${cls.simpleName}.Request: no constructors found.")
            val paramNames = requestConstructor.parameters.map { it.name }
            require(paramNames.all { it != null && !it.matches(Regex("arg\\d+")) }) {
                "${cls.simpleName}.Request constructor parameter names not retained. " +
                    "Ensure the generated module is compiled with `-java-parameters`."
            }

            val hasBody = "body" in paramNames

            @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
            val bodyElementClass: Class<*>? = if (hasBody) {
                val bodyParam = requestConstructor.parameters.first { it.name == "body" }
                if (java.util.List::class.java.isAssignableFrom(bodyParam.type)) {
                    (bodyParam.parameterizedType as? ParameterizedType)?.actualTypeArguments?.firstOrNull() as? Class<*>
                } else {
                    null
                }
            } else {
                null
            }

            return EndpointReflection(
                endpointName = cls.simpleName ?: jcls.name,
                responseVariantsByStatus = variants,
                fromResponseMethod = fromResponseMethod,
                instance = instance,
                requestConstructor = requestConstructor,
                requestConstructorParamNames = paramNames.filterNotNull(),
                hasBody = hasBody,
                bodyElementClass = bodyElementClass,
            )
        }
    }
}
