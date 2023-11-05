package community.flock.wirespec.examples.open_api_app.java

import community.flock.wirespec.Wirespec
import community.flock.wirespec.generated.java.v3.AddPet
import community.flock.wirespec.generated.java.v3.FindPetsByStatus
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.web.client.RestTemplate
import java.net.URI
import java.util.concurrent.CompletableFuture
import java.util.function.Function

interface JavaPetstoreClient : AddPet, FindPetsByStatus

@Configuration
class JavaPetClientConfiguration {

    @Bean
    fun javaPetstoreClient(
        restTemplate: RestTemplate,
        contentMapper: Wirespec.ContentMapper<ByteArray>
    ): JavaPetstoreClient =

        object : JavaPetstoreClient {
            fun <Req : Wirespec.Request<*>, Res : Wirespec.Response<*>> handle(
                request: Req,
                responseMapper: (Wirespec.ContentMapper<ByteArray>) -> Function<Wirespec.Response<ByteArray>, Res>
            ): CompletableFuture<Res> = restTemplate.execute(
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
                    CompletableFuture.completedFuture(responseMapper(contentMapper).apply(response))
                }
            ) ?: error("No response")


            override fun addPet(request: AddPet.Request<*>): CompletableFuture<AddPet.Response<*>> {
                return handle(request, AddPet::RESPONSE_MAPPER)
            }

            override fun findPetsByStatus(request: FindPetsByStatus.Request<*>): CompletableFuture<FindPetsByStatus.Response<*>> {
                return handle(request, FindPetsByStatus::RESPONSE_MAPPER)
            }
        }
}
