package community.flock.wirespec.openapi.v3

import com.goncalossilva.resources.Resource
import community.flock.kotlinx.openapi.bindings.v3.OpenAPI
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class OpenApiV3EmitterTest {

    val json = Json { prettyPrint = true }

    @Test
    fun objectInRequest() {
        val petstore = Resource("src/commonTest/resources/v3/petstore.json").readText()
        val openApi = OpenAPI.decodeFromString(petstore)
        val ast = OpenApiV3Parser.parse(openApi)
        val res = OpenApiV3Emitter().emit(ast)
        assertEquals(ast, OpenApiV3Parser.parse(res))
        assertEquals(json.encodeToString(openApi), json.encodeToString(res))
    }

}
