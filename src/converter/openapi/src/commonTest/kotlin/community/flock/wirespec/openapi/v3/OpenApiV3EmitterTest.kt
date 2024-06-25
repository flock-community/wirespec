package community.flock.wirespec.openapi.v3

import com.goncalossilva.resources.Resource
import community.flock.kotlinx.openapi.bindings.v3.OpenAPI
import community.flock.wirespec.openapi.v3.OpenApiV3Parser.parse
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class OpenApiV3EmitterTest {

    val json = Json { prettyPrint = true }

    @Test
    fun objectInRequest() {
        val petstoreJson = Resource("src/commonTest/resources/v3/petstore.json").readText()

        val petstoreOpenAPi = OpenAPI.decodeFromString(petstoreJson)
        val petstoreAst = petstoreOpenAPi.parse()

        val petstoreConvertedOpenAPi = OpenApiV3Emitter.emit(petstoreAst)
        val petstoreConvertedOpenAPiAst = petstoreConvertedOpenAPi.parse()

        assertEquals(petstoreAst, petstoreConvertedOpenAPiAst)

    }

}
