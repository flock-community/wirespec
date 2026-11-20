package community.flock.wirespec.integration.kotest.dsl

import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * A request outside the contract. A [ByteArray] body goes over the wire as it is, a `null` body sends none, and any
 * other body is serialized as [bodyType] with the serialization of the endpoint context.
 */
public data class RawRequest<out B>(
    public val method: String,
    public val path: List<String>,
    public val queries: Map<String, List<String>>,
    public val headers: Map<String, List<String>>,
    public val body: B,
    public val bodyType: KType,
)

public inline fun <reified B> RawRequest(
    method: String,
    path: List<String>,
    queries: Map<String, List<String>>,
    headers: Map<String, List<String>>,
    body: B,
): RawRequest<B> = RawRequest(method, path, queries, headers, body, typeOf<B>())

public inline fun <reified C> RawRequest<*>.withBody(body: C): RawRequest<C> = RawRequest(method, path, queries, headers, body, typeOf<C>())

/**
 * A response outside the contract. A [ByteArray] body goes over the wire as it is, a `null` body sends none, and any
 * other body is serialized as [bodyType] with the serialization of the mock context.
 */
public data class RawResponse<out B>(
    public val status: Int,
    public val headers: Map<String, List<String>>,
    public val body: B,
    public val bodyType: KType,
)

public inline fun <reified B> RawResponse(
    status: Int,
    headers: Map<String, List<String>>,
    body: B,
): RawResponse<B> = RawResponse(status, headers, body, typeOf<B>())

public inline fun <reified C> RawResponse<*>.withBody(body: C): RawResponse<C> = RawResponse(status, headers, body, typeOf<C>())

/**
 * A channel message outside the contract. A [ByteArray] body is published as it is, and any other body is serialized
 * as [bodyType] with the serialization of the channel context.
 */
public data class RawMessage<out B : Any>(
    public val topic: String,
    public val key: String?,
    public val body: B,
    public val bodyType: KType,
)

public inline fun <reified B : Any> RawMessage(
    topic: String,
    key: String?,
    body: B,
): RawMessage<B> = RawMessage(topic, key, body, typeOf<B>())

public inline fun <reified C : Any> RawMessage<*>.withBody(body: C): RawMessage<C> = RawMessage(topic, key, body, typeOf<C>())

internal fun Wirespec.BodySerializer.serializeRaw(body: Any, type: KType): ByteArray = body as? ByteArray ?: serializeBody(body, type)

internal fun RawRequest<*>.toWirespec(serializer: Wirespec.BodySerializer): Wirespec.RawRequest = Wirespec.RawRequest(
    method = method,
    path = path,
    queries = queries,
    headers = headers,
    body = body?.let { serializer.serializeRaw(it, bodyType) },
)

internal fun RawResponse<*>.toWirespec(serializer: Wirespec.BodySerializer): Wirespec.RawResponse = Wirespec.RawResponse(
    statusCode = status,
    headers = headers,
    body = body?.let { serializer.serializeRaw(it, bodyType) },
)

internal fun Wirespec.RawRequest.toKotest(): RawRequest<ByteArray?> = RawRequest(method, path, queries, headers, body)

internal fun Wirespec.RawResponse.toKotest(): RawResponse<ByteArray?> = RawResponse(statusCode, headers, body)
