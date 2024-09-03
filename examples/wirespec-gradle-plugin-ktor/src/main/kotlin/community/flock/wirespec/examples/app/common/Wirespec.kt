package community.flock.wirespec.examples.app.common

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import community.flock.wirespec.Wirespec
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.request
import io.ktor.http.HttpMethod
import io.ktor.http.headers
import io.ktor.http.path
import kotlinx.coroutines.runBlocking
import kotlin.reflect.KType
import kotlin.reflect.javaType

class WirespecClient(private val httpClient: HttpClient = HttpClient()) {

    fun handle(request: Wirespec.RawRequest): Wirespec.RawResponse = runBlocking {
        val response = httpClient.request("http://localhost:8080/") {
            method = HttpMethod.parse(request.method)
            url {
                path(*request.path.toTypedArray())
            }
            headers {
                request.headers.forEach { (t, u) -> appendAll(t, u) }
            }
        }
        response.run {
            Wirespec.RawResponse(
                statusCode = status.value,
                headers = headers.entries().associate { it.key to it.value },
                body = body()
            )
        }
    }
}

object Serialization : Wirespec.Serialization<String> {

    private val mapper = jacksonObjectMapper()

    override fun <T> serialize(t: T, kType: KType): String =
        mapper.writeValueAsString(t)

    @OptIn(ExperimentalStdlibApi::class)
    override fun <T> deserialize(raw: String, kType: KType): T =
        mapper.constructType(kType.javaType)
            .let { mapper.readValue(raw, it) }

}
