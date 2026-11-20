package community.flock.wirespec.integration.kotest.dsl

import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/** A response as it came back, without checking it against the contract. */
public data class RawResponse(
    public val status: Int,
    public val headers: Map<String, List<String>>,
    public val body: ByteArray?,
)

/**
 * The body of a value outside the contract. It starts as the bytes the typed value serializes to. [body] replaces it
 * with an input of any type: a [ByteArray] goes over the wire as it is, `null` sends none, and any other input is
 * serialized as its type with the serialization of the context it goes through.
 */
@WirespecScenarioDsl
public abstract class RawBodyScope internal constructor(body: ByteArray?) {

    public var body: Any? = body
        private set

    private var bodyType: KType = typeOf<ByteArray?>()

    public inline fun <reified B> body(input: B): Unit = replaceBody(input, typeOf<B>())

    @PublishedApi
    internal fun replaceBody(input: Any?, type: KType) {
        body = input
        bodyType = type
    }

    internal fun serializedBody(serializer: Wirespec.BodySerializer): ByteArray? = body?.let { it as? ByteArray ?: serializer.serializeBody(it, bodyType) }
}

@WirespecScenarioDsl
public class RawRequestScope internal constructor(request: Wirespec.RawRequest) : RawBodyScope(request.body) {
    public var method: String = request.method
    public var path: List<String> = request.path
    public var queries: Map<String, List<String>> = request.queries
    public var headers: Map<String, List<String>> = request.headers

    internal fun toWirespec(serializer: Wirespec.BodySerializer): Wirespec.RawRequest = Wirespec.RawRequest(method, path, queries, headers, serializedBody(serializer))
}

@WirespecScenarioDsl
public class RawResponseScope internal constructor(response: Wirespec.RawResponse) : RawBodyScope(response.body) {
    public var status: Int = response.statusCode
    public var headers: Map<String, List<String>> = response.headers

    internal fun toWirespec(serializer: Wirespec.BodySerializer): Wirespec.RawResponse = Wirespec.RawResponse(status, headers, serializedBody(serializer))
}

@WirespecScenarioDsl
public class RawMessageScope internal constructor(body: ByteArray) : RawBodyScope(body) {
    internal fun serialized(serializer: Wirespec.BodySerializer): ByteArray = serializedBody(serializer) ?: ByteArray(0)
}

internal fun Wirespec.RawResponse.toKotest(): RawResponse = RawResponse(statusCode, headers, body)
