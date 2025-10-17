package community.flock.wirespec.openapi.v2

import community.flock.kotlinx.openapi.bindings.OpenAPIV2
import community.flock.wirespec.openapi.v2.OpenAPIV2Parser.parse
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlinx.serialization.json.Json
import kotlin.test.Test

class OpenAPIV2EmitterTest {

    val json = Json { prettyPrint = true }

    @Test
    fun petstoreParseEmitParse() {
        val path = Path("src/commonTest/resources/v2/petstore.json")
        val petstoreJson = SystemFileSystem.source(path).buffered().readString()

        val petstoreOpenAPi = OpenAPIV2.decodeFromString(petstoreJson)
        val petstoreAst = petstoreOpenAPi.parse().shouldNotBeNull()

        val petstoreConvertedOpenAPI = OpenAPIV2Emitter.emitSwaggerObject(petstoreAst)
        val petstoreConvertedOpenAPiAst = petstoreConvertedOpenAPI.parse().shouldNotBeNull()

        petstoreAst.toList()
            .sortedBy { it.identifier.value }
            .joinToString("\n") { it.toString() } shouldBe petstoreConvertedOpenAPiAst
            .sortedBy { it.identifier.value }
            .joinToString("\n") { it.toString() }
    }
}
