package community.flock.wirespec.examples.open_api_app.java

import com.fasterxml.jackson.databind.ObjectMapper
import community.flock.wirespec.generated.java.v3.WirespecShared.Request
import community.flock.wirespec.generated.java.v3.WirespecShared.Response
import community.flock.wirespec.generated.java.v3.WirespecShared.Content
import community.flock.wirespec.generated.java.v3.WirespecShared.ContentMapper
import community.flock.wirespec.generated.java.v3.CreatePets
import community.flock.wirespec.generated.java.v3.ListPets
import community.flock.wirespec.generated.java.v3.ShowPetById
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.web.client.RestTemplate
import java.lang.reflect.Type
import java.net.URI

interface JavaPetstoreClient : ListPets, CreatePets, ShowPetById

@Configuration
class JavaPetClientConfiguration {

    @Bean
    fun javaContentMapper(objectMapper: ObjectMapper) =
        object : ContentMapper<ByteArray> {
            override fun <T> read(content: Content<ByteArray>, valueType: Type): Content<T> = content.let {
                val type = objectMapper.constructType(valueType)
                val obj: T = objectMapper.readValue(content.body, type)
                Content(it.type, obj)
            }

            override fun <T> write(content: Content<T>): Content<ByteArray> = content.let {
                val bytes = objectMapper.writeValueAsBytes(content.body)
                Content(it.type, bytes)
            }
        }


    @Bean
    fun javaPetstoreClient(restTemplate: RestTemplate, javaContentMapper: ContentMapper<ByteArray>): JavaPetstoreClient =

        object : JavaPetstoreClient {
            fun <Req : Request<*>, Res : Response<*>> handle(
                request: Req,
                responseMapper: (ContentMapper<ByteArray>, Int, Map<String, List<String>>, Content<ByteArray>) -> Res
            ) = restTemplate.execute(
                URI("https://6467e16be99f0ba0a819fd68.mockapi.io${request.path}"),
                HttpMethod.valueOf(request.method.name),
                { req ->
                    request.content
                        ?.let { javaContentMapper.write(it) }
                        ?.let { req.body.write(it.body) }
                },
                { res ->
                    val contentType = res.headers.contentType?.toString() ?: error("No content type")
                    val content = Content(contentType, res.body.readBytes())
                    responseMapper(javaContentMapper, res.statusCode.value(), res.headers, content)
                }
            ) ?: error("No response")

            override fun listPets(request: ListPets.ListPetsRequest<*>): ListPets.ListPetsResponse<*> {
             return handle(request, ListPets::RESPONSE_MAPPER)
            }

            override fun createPets(request: CreatePets.CreatePetsRequest<*>): CreatePets.CreatePetsResponse<*> {
                return handle(request, CreatePets::RESPONSE_MAPPER)
            }

            override fun showPetById(request: ShowPetById.ShowPetByIdRequest<*>): ShowPetById.ShowPetByIdResponse<*> {
                return handle(request, ShowPetById::RESPONSE_MAPPER)
            }


        }

}
