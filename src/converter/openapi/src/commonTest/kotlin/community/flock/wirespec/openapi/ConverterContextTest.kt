package community.flock.wirespec.openapi

import community.flock.wirespec.compiler.core.emit.Emitted
import community.flock.wirespec.compiler.test.parse
import community.flock.wirespec.compiler.utils.noLogger
import community.flock.wirespec.converter.common.ConverterContext
import community.flock.wirespec.openapi.v2.OpenAPIV2Emitter
import community.flock.wirespec.openapi.v3.OpenAPIV3Emitter
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test

class ConverterContextTest {

    private val ast = parse("type Pet { name: String }").shouldBeRight()

    private val context = ConverterContext(name = "Petstore", version = "1.2.3")

    @Test
    fun openAPIV3TakesTheNameAndVersionOfTheContext() {
        OpenAPIV3Emitter(context).emit(ast, noLogger).single().run {
            file shouldBe "Petstore.json"
            info() shouldEqualJson """{ "title": "Petstore", "version": "1.2.3" }"""
        }
    }

    @Test
    fun openAPIV2TakesTheNameAndVersionOfTheContext() {
        OpenAPIV2Emitter(context).emit(ast, noLogger).single().run {
            file shouldBe "Petstore.json"
            info() shouldEqualJson """{ "title": "Petstore", "version": "1.2.3" }"""
        }
    }

    @Test
    fun withoutContextTheDefaultsStay() {
        listOf(OpenAPIV3Emitter(), OpenAPIV2Emitter()).forEach { emitter ->
            emitter.emit(ast, noLogger).single().run {
                file shouldBe "OpenAPI.json"
                info() shouldEqualJson """{ "title": "Wirespec", "version": "0.0.0" }"""
            }
        }
    }

    @Test
    fun aContextWithOnlyAVersionKeepsTheDefaultName() {
        OpenAPIV3Emitter(ConverterContext(version = "2.0.0")).emit(ast, noLogger).single().run {
            file shouldBe "OpenAPI.json"
            info() shouldEqualJson """{ "title": "Wirespec", "version": "2.0.0" }"""
        }
    }

    private fun Emitted.info(): String = Json.parseToJsonElement(result).jsonObject.getValue("info").toString()
}
