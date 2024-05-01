package community.flock.wirespec.openapi.v2

import com.goncalossilva.resources.Resource
import community.flock.kotlinx.openapi.bindings.v2.OpenAPI
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class OpenApiV2EmitterTest {

    val json = Json{prettyPrint=true}

    @Test
    fun objectInRequest() {
        val petstore = Resource("src/commonTest/resources/v2/petstore.json").readText()
//        val petstore = Resource("src/commonTest/resources/v3/petstore_converted.json").readText()
        val openApi =OpenAPI.decodeFromString(petstore)
        val ast = OpenApiV2Parser.parse(openApi)
        val res = OpenApiV2Emitter().emit(ast)
        assertEquals(ast, OpenApiV2Parser.parse(res))

    }

}
