package community.flock.wirespec.examples.open_api_app

import com.fasterxml.jackson.databind.ObjectMapper
import community.flock.wirespec.generated.ListPets
import community.flock.wirespec.generated.ContentMapper
import community.flock.wirespec.generated.Request
import community.flock.wirespec.generated.Response
import community.flock.wirespec.generated.Content
import community.flock.wirespec.generated.CreatePets
import community.flock.wirespec.generated.ShowPetById
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.web.client.RestTemplate
import java.io.ByteArrayOutputStream
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
    fun contentMapper(objectMapper: ObjectMapper): ContentMapper {
        return object : ContentMapper {
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
    }

    @Bean
    fun petClient(restTemplate: RestTemplate, contentMapper: ContentMapper): PetstoreClient {
        return object : PetstoreClient {
            override suspend fun <Req : Request<*>, Res : Response<*>> handle(
                request: Req,
                mapper: (ContentMapper) -> (Int, String, Map<String, List<String>>, ByteArray) -> Res
            ): Res {
                val method = HttpMethod.valueOf(request.method.name)
                return restTemplate.execute(
                    URI("https://6467e16be99f0ba0a819fd68.mockapi.io${request.url}"),
                    method,
                    { req ->

                        request.content
                            ?.let { contentMapper.write(it) }
                            ?.let { req.body.write(it.body) }
                    },
                    { res ->
                        val contentType = res.headers.contentType?.toString() ?: error("No content type")
                        mapper(contentMapper)(res.statusCode.value(), contentType, res.headers, res.body.readBytes())
                    }
                ) ?: error("No response")
            }
        }
    }
}