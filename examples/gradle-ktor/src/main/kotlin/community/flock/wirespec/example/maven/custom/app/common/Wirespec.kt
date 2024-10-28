package community.flock.wirespec.example.maven.custom.app.common

import community.flock.wirespec.kotlin.Wirespec
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

class WirespecClient(private val httpClient: HttpClient = HttpClient()) {

    fun handle(request: Wirespec.RawRequest): Wirespec.RawResponse = runBlocking {
        val response = httpClient.request("http://localhost:8080/") {
            method = HttpMethod.parse(request.method)
            url {
                path(*request.path.toTypedArray())
            }
            headers {
                request.headers.forEach { (t, u) -> appendAll(t, listOf(u)) }
            }
        }
        response.run {
            Wirespec.RawResponse(
                statusCode = status.value,
                headers = headers.entries().associate { it.key to it.value.first() },
                body = body()
            )
        }
    }
}

@Suppress("UNCHECKED_CAST")
object Serialization : Wirespec.Serialization<String> {
    override fun <T> serialize(t: T, kType: KType): String =
        Json.encodeToString(Json.serializersModule.serializer(kType), t)

    override fun <T> deserialize(raw: String, kType: KType): T = when (kType) {
        String::class.createType() -> raw as T
        else -> Json.decodeFromString(Json.serializersModule.serializer(kType), raw) as T
    }
}
