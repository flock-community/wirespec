package community.flock.wirespec.openapi.v2

import com.goncalossilva.resources.Resource
import community.flock.kotlinx.openapi.bindings.v2.OpenAPI
import community.flock.wirespec.compiler.core.parse.Definition
import community.flock.wirespec.openapi.v2.OpenApiV2Parser.parse
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class OpenApiV2EmitterTest {

    val json = Json { prettyPrint = true }

    @Test
    fun objectInRequest() {
        val petstoreJson = Resource("src/commonTest/resources/v2/petstore.json").readText()

        val petstoreOpenAPi = OpenAPI.decodeFromString(petstoreJson)
        val petstoreAst = petstoreOpenAPi.parse()

        val petstoreConvertedOpenApi = OpenApiV2Emitter.emitSwaggerObject(petstoreAst)
        val petstoreConvertedOpenAPiAst = petstoreConvertedOpenApi.parse()

        assertEquals(
            petstoreAst.filterIsInstance<Definition>().sortedBy { it.identifier.value }
                .joinToString("\n") { it.toString() },
            petstoreConvertedOpenAPiAst.filterIsInstance<Definition>().sortedBy { it.identifier.value }
                .joinToString("\n") { it.toString() },
        )
    }
}
