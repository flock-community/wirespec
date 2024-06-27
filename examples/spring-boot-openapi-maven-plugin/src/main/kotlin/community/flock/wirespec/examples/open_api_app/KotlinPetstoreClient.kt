package community.flock.wirespec.examples.open_api_app

import community.flock.wirespec.Wirespec
import community.flock.wirespec.generated.kotlin.v3.AddPetEndpoint
import community.flock.wirespec.generated.kotlin.v3.FindPetsByStatusEndpoint
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.web.client.RestTemplate
import java.net.URI
import kotlin.reflect.typeOf

interface KotlinPetstoreClient : AddPetEndpoint, FindPetsByStatusEndpoint

@Configuration
class KotlinPetClientConfiguration {

    @Bean
    fun kotlinPetstoreClient(
        restTemplate: RestTemplate,
        contentMapper: Wirespec.ContentMapper<ByteArray>
    ): KotlinPetstoreClient =
        object : KotlinPetstoreClient {
            fun <Req : Wirespec.Request<*>, Res : Wirespec.Response<*>> handle(
                request: Req,
                responseMapper: (Wirespec.ContentMapper<ByteArray>) -> (Wirespec.Response<ByteArray>) -> Res
            ) = restTemplate.execute(
                URI("https://6467e16be99f0ba0a819fd68.mockapi.io${request.path}"),
                HttpMethod.valueOf(request.method.name),
                { req ->
                    request.content
                        ?.let { contentMapper.write(it, typeOf<Any>()) }
                        ?.let { req.body.write(it.body) }
                },
                { res ->
                    val contentType = res.headers.contentType?.toString() ?: error("No content type")
                    val content = Wirespec.Content(contentType, res.body.readBytes())
                    val response = object : Wirespec.Response<ByteArray> {
                        override val status = res.statusCode.value()
                        override val headers = res.headers
                        override val content = content
                    }
                    responseMapper(contentMapper)(response)
                }
            ) ?: error("No response")

            override suspend fun addPet(request: AddPetEndpoint.Request<*>): AddPetEndpoint.Response<*> {
                return handle(request, AddPetEndpoint::RESPONSE_MAPPER)
            }

            override suspend fun findPetsByStatus(request: FindPetsByStatusEndpoint.Request<*>): FindPetsByStatusEndpoint.Response<*> {
                return handle(request, FindPetsByStatusEndpoint::RESPONSE_MAPPER)
            }
        }
}
