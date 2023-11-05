package community.flock.wirespec.examples.open_api_app.kotlin

import community.flock.wirespec.Wirespec
import community.flock.wirespec.generated.kotlin.v3.AddPet
import community.flock.wirespec.generated.kotlin.v3.FindPetsByStatus
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.web.client.RestTemplate
import java.net.URI

interface KotlinPetstoreClient : AddPet, FindPetsByStatus

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
                        ?.let { contentMapper.write(it) }
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

            override suspend fun addPet(request: AddPet.Request<*>): AddPet.Response<*> {
                return handle(request, AddPet::RESPONSE_MAPPER)
            }

            override suspend fun findPetsByStatus(request: FindPetsByStatus.Request<*>): FindPetsByStatus.Response<*> {
                return handle(request, FindPetsByStatus::RESPONSE_MAPPER)
            }
        }
}
