package community.flock.wirespec.example.maven.custom.app.common

import community.flock.wirespec.kotlin.Wirespec
import community.flock.wirespec.kotlin.serde.DefaultParamSerialization
import community.flock.wirespec.kotlin.serde.DefaultPathSerialization
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.request
import io.ktor.http.HttpMethod
import io.ktor.http.headers
import io.ktor.http.path
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.reflect.KType
import kotlin.reflect.full.createType

class WirespecClient(
    private val httpClient: HttpClient = HttpClient(),
) {
    fun transport(request: Wirespec.RawRequest): Wirespec.RawResponse = runBlocking {
        val response =
            httpClient.request("http://localhost:8080/") {
                method = HttpMethod.parse(request.method)
                url {
                    path(*request.path.toTypedArray())
                }
                headers {
                    request.headers.forEach { (name, values) -> appendAll(name, values) }
                }
            }
        response.run {
            Wirespec.RawResponse(
                statusCode = status.value,
                headers = headers.entries().associate { it.key to it.value },
                body = body(),
            )
        }
    }
}

/**
 * Example implementation of Wirespec Serialization using DefaultParamSerialization
 * This class handles standard parameter serialization for headers and query parameters.
 * For custom serialization requirements, you can create your own implementation
 * of Wirespec.ParamSerialization instead of using DefaultParamSerialization.
 * In this case, you don't need the dependency on community.flock.wirespec.integration:wirespec
 */
@Suppress("UNCHECKED_CAST")
object Serialization :
    Wirespec.Serialization,
    Wirespec.ParamSerialization by DefaultParamSerialization(),
    Wirespec.PathSerialization by DefaultPathSerialization() {

    override fun <T> serializeBody(
        t: T,
        kType: KType,
    ): ByteArray = Json.encodeToString(Json.serializersModule.serializer(kType), t).toByteArray()

    override fun <T> deserializeBody(
        raw: ByteArray,
        kType: KType,
    ): T = when (kType) {
        String::class.createType() -> raw as T
        else -> Json.decodeFromString(Json.serializersModule.serializer(kType), raw.toString()) as T
    }
}
