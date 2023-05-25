package community.flock.wirespec.examples.open_api_app

import com.fasterxml.jackson.databind.ObjectMapper
import community.flock.wirespec.generated.Content
import community.flock.wirespec.generated.ContentMapper
import community.flock.wirespec.generated.CreatePets
import community.flock.wirespec.generated.ListPets
import community.flock.wirespec.generated.Pet
import community.flock.wirespec.generated.Request
import community.flock.wirespec.generated.Response
import community.flock.wirespec.generated.ShowPetById
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.web.client.RestTemplate
import java.net.URI
import kotlin.reflect.KType
import kotlin.reflect.jvm.javaType

interface PetstoreClient : ListPets, CreatePets, ShowPetById

@Configuration
class ClientConfiguration {

    @Bean
    fun restTemplate(): RestTemplate {
        return RestTemplate()
    }

    @Bean
    fun contentMapper(objectMapper: ObjectMapper) =
        object : ContentMapper<ByteArray> {
            override fun <T> read(
                content: Content<ByteArray>,
                valueType: KType,
            ): Content<T> {
                return content.let {
                    val type = objectMapper.constructType(valueType.javaType)
                    val obj: T = objectMapper.readValue(content.body, type)
                    Content(it.type, obj)
                }
            }

            override fun <T> write(
                content: Content<T>,
            ): Content<ByteArray> {
                return content.let {
                    val bytes = objectMapper.writeValueAsBytes(content.body)
                    Content(it.type, bytes)
                }
            }
        }


    @Bean
    fun petClient(restTemplate: RestTemplate, contentMapper: ContentMapper<ByteArray>): PetstoreClient =
        object : PetstoreClient {
            fun <Req : Request<*>, Res : Response<*>> handle(
                request: Req,
                responseMapper: (ContentMapper<ByteArray>) -> (Int, String, Map<String, List<String>>, ByteArray) -> Res
            ) = restTemplate.execute(
                URI("https://6467e16be99f0ba0a819fd68.mockapi.io${request.url}"),
                HttpMethod.valueOf(request.method.name),
                { req ->
                    request.content
                        ?.let { contentMapper.write(it) }
                        ?.let { req.body.write(it.body) }
                },
                { res ->
                    val contentType = res.headers.contentType?.toString() ?: error("No content type")
                    responseMapper(contentMapper)(
                        res.statusCode.value(),
                        contentType,
                        res.headers,
                        res.body.readBytes()
                    )
                }
            ) ?: error("No response")


            override suspend fun listPets(limit: Int): ListPets.ListPetsResponse<out Any> {
                return handle(ListPets.REQUEST_MAPPER(limit)(null), ListPets::RESPONSE_MAPPER)
            }

            override suspend fun createPetsApplicationJson(content: Pet): CreatePets.CreatePetsResponse<out Any> {
                return handle(CreatePets.REQUEST_MAPPER(content)("application/json"), CreatePets::RESPONSE_MAPPER)
            }

            override suspend fun showPetById(petId: String): ShowPetById.ShowPetByIdResponse<out Any> {
                return handle(ShowPetById.REQUEST_MAPPER(petId)(null), ShowPetById::RESPONSE_MAPPER)
            }
        }

}
