package community.flock.wirespec.examples.open_api_app

import community.flock.wirespec.Wirespec
import community.flock.wirespec.generated.kotlin.v3.AddPet
import community.flock.wirespec.generated.kotlin.v3.FindPetsByStatus
import java.net.URI
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.web.client.RestTemplate
import kotlin.reflect.typeOf

interface KotlinPetstoreClient : AddPet.Endpoint.Handler, FindPetsByStatus.Endpoint.Handler

@Configuration
class KotlinPetClientConfiguration {

    @Bean
    fun kotlinPetstoreClient(
        restTemplate: RestTemplate,
        contentMapper: Wirespec.Mapper<ByteArray>
    ): KotlinPetstoreClient =
        object : KotlinPetstoreClient {
            fun <Req : Wirespec.Request<*>, Res : Wirespec.Response<*>> handle(
                request: Req,
                responseMapper: (Wirespec.Mapper<ByteArray>) -> (Wirespec.Response<ByteArray>) -> Res
            ) = restTemplate.execute(
                URI("https://6467e16be99f0ba0a819fd68.mockapi.io${request.path}"),
                HttpMethod.valueOf(request.method.name),
                { req ->
                    request
                        .let { contentMapper.write(it, typeOf<Any>()) }
                        .let { req.body.write(it) }
                },
                { res ->
                    val content = res.body.readBytes()
                    val response = object : Wirespec.Response<ByteArray> {
                        override val status = res.statusCode.value()
                        override val headers = object : Wirespec.Response.Headers {}
                        override val body = content
                    }
                    responseMapper(contentMapper)(response)
                }
            ) ?: error("No response")

            override suspend fun addPet(request: AddPet.Endpoint.Request<*>): AddPet.Endpoint.Response<*> {
                when (request) {
                    is AddPet.Endpoint.RequestApplicationJson -> TODO()
                    is AddPet.Endpoint.RequestApplicationXWwwFormUrlencoded -> TODO()
                    is AddPet.Endpoint.RequestApplicationXml -> TODO()
                }
            }

            override suspend fun findPetsByStatus(request: FindPetsByStatus.Endpoint.Request<*>): FindPetsByStatus.Endpoint.Response<*> {
                when (request) {
                    is FindPetsByStatus.Endpoint.RequestUnit -> TODO()
                }
            }

        }
}
