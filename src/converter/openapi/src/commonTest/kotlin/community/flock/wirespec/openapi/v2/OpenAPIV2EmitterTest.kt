package community.flock.wirespec.openapi.v2

import arrow.core.toNonEmptyListOrNull
import community.flock.kotlinx.openapi.bindings.v2.OpenAPI
import community.flock.wirespec.compiler.core.parse.Module
import community.flock.wirespec.openapi.v2.OpenAPIV2Parser.parse
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class OpenAPIV2EmitterTest {

    val json = Json { prettyPrint = true }

    @Test
    fun petstoreParseEmitParse() {
        val path = Path("src/commonTest/resources/v2/petstore.json")
        val petstoreJson = SystemFileSystem.source(path).buffered().readString()

        val petstoreOpenAPi = OpenAPI.decodeFromString(petstoreJson)
        val petstoreAst = petstoreOpenAPi.parse().toNonEmptyListOrNull() ?: error("AST should not be empty")

        val petstoreConvertedOpenAPI = OpenAPIV2Emitter.emitSwaggerObject(Module("", petstoreAst))
        val petstoreConvertedOpenAPiAst = petstoreConvertedOpenAPI.parse()

        assertEquals(
            petstoreAst.toList().sortedBy { it.identifier.value }
                .joinToString("\n") { it.toString() },
            petstoreConvertedOpenAPiAst.sortedBy { it.identifier.value }
                .joinToString("\n") { it.toString() },
        )
    }
}
