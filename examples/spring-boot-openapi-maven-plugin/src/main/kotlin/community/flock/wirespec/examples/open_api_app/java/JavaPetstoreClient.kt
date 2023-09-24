package community.flock.wirespec.examples.open_api_app.java

import com.fasterxml.jackson.databind.ObjectMapper
import community.flock.wirespec.Wirespec
import community.flock.wirespec.generated.java.v3.AddPet
import community.flock.wirespec.generated.java.v3.FindPetsByStatus
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.web.client.RestTemplate
import java.lang.reflect.Type
import java.net.URI

interface JavaPetstoreClient : AddPet, FindPetsByStatus

@Configuration
class JavaPetClientConfiguration {

    @Bean
    fun javaContentMapper(objectMapper: ObjectMapper) =
        object : Wirespec.ContentMapper<ByteArray> {
            override fun <T> read(content: Wirespec.Content<ByteArray>, valueType: Type): Wirespec.Content<T> = content.let {
                val type = objectMapper.constructType(valueType)
                val obj: T = objectMapper.readValue(content.body, type)
                Wirespec.Content(it.type, obj)
            }

            override fun <T> write(content: Wirespec.Content<T>): Wirespec.Content<ByteArray> = content.let {
                val bytes = objectMapper.writeValueAsBytes(content.body)
                Wirespec.Content(it.type, bytes)
            }
        }


    @Bean
    fun javaPetstoreClient(restTemplate: RestTemplate, javaContentMapper: Wirespec.ContentMapper<ByteArray>): JavaPetstoreClient =

        object : JavaPetstoreClient {
            fun <Req : Wirespec.Request<*>, Res : Wirespec.Response<*>> handle(
                request: Req,
                responseMapper: (Wirespec.ContentMapper<ByteArray>, Int, Map<String, List<String>>, Wirespec.Content<ByteArray>) -> Res
            ):Res = restTemplate.execute(
                URI("https://6467e16be99f0ba0a819fd68.mockapi.io${request.path}"),
                HttpMethod.valueOf(request.method.name),
                { req ->
                    request.content
                        ?.let { javaContentMapper.write(it) }
                        ?.let { req.body.write(it.body) }
                },
                { res ->
                    val contentType = res.headers.contentType?.toString() ?: error("No content type")
                    val content = Wirespec.Content(contentType, res.body.readBytes())
                    responseMapper(javaContentMapper, res.statusCode.value(), res.headers, content)
                }
            ) ?: error("No response")


            override fun addPet(request: AddPet.Request<*>): AddPet.Response<*> {
                return handle(request, AddPet::RESPONSE_MAPPER)
            }

            override fun findPetsByStatus(request: FindPetsByStatus.Request<*>): FindPetsByStatus.Response<*> {
                return handle(request, FindPetsByStatus::RESPONSE_MAPPER)
            }


        }

}
