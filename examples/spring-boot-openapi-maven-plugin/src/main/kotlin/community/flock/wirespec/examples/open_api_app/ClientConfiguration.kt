package community.flock.wirespec.examples.open_api_app

import com.fasterxml.jackson.databind.ObjectMapper
import community.flock.wirespec.generated.ListPets
import community.flock.wirespec.generated.Mapper
import community.flock.wirespec.generated.Request
import community.flock.wirespec.generated.Response
import community.flock.wirespec.generated.Method
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.web.client.RestTemplate
import java.net.URI
import kotlin.reflect.KType
import kotlin.reflect.jvm.javaType

interface PetstoreClient : ListPets

@Configuration
class ClientConfiguration {


    @Bean
    fun restTemplate(): RestTemplate {
        return RestTemplate()
    }

    @Bean
    fun contentMapper(objectMapper: ObjectMapper): Mapper {
        return object : Mapper {
            override fun <T> read(src: ByteArray, valueType: KType): T {
                val type = objectMapper.constructType(valueType.javaType)
                return objectMapper.readValue(src, type)
            }
        }
    }

    @Bean
    fun petClient(restTemplate: RestTemplate, contentMapper: Mapper): PetstoreClient {
        return object : PetstoreClient {
            override suspend fun <Req : Request<*>, Res : Response<*>> handle(
                request: Req,
                mapper: (Mapper) -> (Int, String, Map<String, List<String>>, ByteArray) -> Res
            ): Res {
                val method = when (request.method) {
                    Method.GET -> HttpMethod.GET
                    Method.POST -> HttpMethod.POST
                    Method.PUT -> HttpMethod.PUT
                    Method.DELETE -> HttpMethod.DELETE
                    else -> error("Cannot map method")
                }
                return restTemplate.execute(
                    URI("https://6467e16be99f0ba0a819fd68.mockapi.io${request.url}"),
                    method,
                    { req -> req },
                    { res ->
                        val contentType = res.headers.contentType?.toString() ?: error("No content type")
                        mapper(contentMapper)(res.statusCode.value(), contentType, res.headers, res.body.readBytes())
                    }
                ) ?: error("No response")
            }
        }
    }
}