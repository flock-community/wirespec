package community.flock.wirespec.openapi.v2

import com.goncalossilva.resources.Resource
import community.flock.kotlinx.openapi.bindings.v2.OpenAPI
import community.flock.wirespec.compiler.core.parse.Definition
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class OpenApiV2EmitterTest {

    val json = Json { prettyPrint = true }

    @Test
    fun objectInRequest() {
        val petstoreJson = Resource("src/commonTest/resources/v2/petstore.json").readText()

        val petstoreOpenAPi = OpenAPI.decodeFromString(petstoreJson)
        val petstoreAst = OpenApiV2Parser.parse(petstoreOpenAPi)

        val petstoreConvertedOpenAPi = OpenApiV2Emitter().emit(petstoreAst)
        val petstoreConvertedOpenAPiAst = OpenApiV2Parser.parse(petstoreConvertedOpenAPi)

        assertEquals(
            petstoreAst.filterIsInstance<Definition>().sortedBy { it.name }.joinToString("\n") { it.toString() },
            petstoreConvertedOpenAPiAst.filterIsInstance<Definition>().sortedBy { it.name }.joinToString("\n") { it.toString() }
        )

    }

}
