package community.flock.wirespec.examples.open_api_app.kotlin

import com.fasterxml.jackson.databind.ObjectMapper
import community.flock.wirespec.generated.kotlin.v3.AddPet
import community.flock.wirespec.generated.kotlin.v3.FindPetsByStatus
import community.flock.wirespec.Wirespec
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.web.client.RestTemplate
import java.net.URI
import java.lang.reflect.Type;

interface KotlinPetstoreClient : AddPet, FindPetsByStatus

@Configuration
class KotlinPetClientConfiguration {

    @Bean
    fun kotlinContentMapper(objectMapper: ObjectMapper) =
        object : Wirespec.ContentMapper<ByteArray> {
            override fun <T> read(
                content: Wirespec.Content<ByteArray>,
                valueType: Type,
            ): Wirespec.Content<T> = content.let {
                val type = objectMapper.constructType(valueType)
                val obj: T = objectMapper.readValue(content.body, type)
                Wirespec.Content(it.type, obj)
            }

            override fun <T> write(
                content: Wirespec.Content<T>,
            ): Wirespec.Content<ByteArray> = content.let {
                val bytes = objectMapper.writeValueAsBytes(content.body)
                Wirespec.Content(it.type, bytes)
            }
        }


    @Bean
    fun kotlinPetstoreClient(restTemplate: RestTemplate, kotlinContentMapper: Wirespec.ContentMapper<ByteArray>): KotlinPetstoreClient =
        object : KotlinPetstoreClient {
            fun <Req : Wirespec.Request<*>, Res : Wirespec.Response<*>> handle(
                request: Req,
                responseMapper: (Wirespec.ContentMapper<ByteArray>, Int, Map<String, List<String>>, Wirespec.Content<ByteArray>) -> Res
            ) = restTemplate.execute(
                URI("https://6467e16be99f0ba0a819fd68.mockapi.io${request.path}"),
                HttpMethod.valueOf(request.method.name),
                { req ->
                    request.content
                        ?.let { kotlinContentMapper.write(it) }
                        ?.let { req.body.write(it.body) }
                },
                { res ->
                    val contentType = res.headers.contentType?.toString() ?: error("No content type")
                    val content = Wirespec.Content(contentType, res.body.readBytes())
                    responseMapper(
                        kotlinContentMapper,
                        res.statusCode.value(),
                        res.headers,
                        content
                    )
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
