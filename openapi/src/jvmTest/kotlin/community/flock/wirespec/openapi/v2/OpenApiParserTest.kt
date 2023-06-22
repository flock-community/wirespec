package community.flock.wirespec.openapi.v2

import community.flock.kotlinx.openapi.bindings.v2.OpenAPI
import community.flock.wirespec.openapi.IO
import kotlin.test.Test

class OpenApiParserTest {

    @Test
    fun petstore() {
        val json = IO.readOpenApi("v2/petstore.json")

        val openApi = OpenAPI.decodeFromString(json)
        val ast = OpenApiParser.parse(openApi)


        println(ast)
    }

}
