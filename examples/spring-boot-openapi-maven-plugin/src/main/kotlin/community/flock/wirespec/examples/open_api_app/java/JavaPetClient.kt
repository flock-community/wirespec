package community.flock.wirespec.examples.open_api_app.java

import com.fasterxml.jackson.databind.ObjectMapper
import community.flock.wirespec.generated.java.WirespecShared.Content
import community.flock.wirespec.generated.java.WirespecShared.ContentMapper
import community.flock.wirespec.generated.java.CreatePets
import community.flock.wirespec.generated.java.ListPets
import community.flock.wirespec.generated.java.ShowPetById
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate
import java.lang.reflect.Type
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
    fun petClient(restTemplate: RestTemplate, contentMapper: ContentMapper<ByteArray>): PetstoreClient =
        object : PetstoreClient {
            override fun listPets(request: ListPets.ListPetsRequest<*>?): ListPets.ListPetsResponse<*> {
                TODO("Not yet implemented")
            }

            override fun createPets(request: CreatePets.CreatePetsRequest<*>?): CreatePets.CreatePetsResponse<*> {
                TODO("Not yet implemented")
            }

            override fun showPetById(request: ShowPetById.ShowPetByIdRequest<*>?): ShowPetById.ShowPetByIdResponse<*> {
                TODO("Not yet implemented")
            }


        }

}
