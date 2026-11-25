package community.flock.wirespec.openapi.common

import arrow.core.nonEmptyListOf
import arrow.core.nonEmptySetOf
import community.flock.wirespec.compiler.core.CompilationContext
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.compile
import community.flock.wirespec.compiler.core.emit.Emitter
import community.flock.wirespec.compiler.utils.NoLogger
import community.flock.wirespec.compiler.utils.noLogger
import community.flock.wirespec.converter.common.Parser
import community.flock.wirespec.emitters.wirespec.WirespecEmitter
import community.flock.wirespec.openapi.v2.OpenAPIV2Emitter
import community.flock.wirespec.openapi.v2.OpenAPIV2Parser
import community.flock.wirespec.openapi.v3.OpenAPIV3Emitter
import community.flock.wirespec.openapi.v3.OpenAPIV3Parser
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.string.shouldContain
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test

class OpenAPIContentTypeTest {

    private fun compile(source: String, emitter: Emitter) = object : CompilationContext, NoLogger {
        override val emitters = nonEmptySetOf(emitter)
    }.compile(nonEmptyListOf(ModuleContent(FileUri("api.ws"), source)))

    private fun document(source: String, emitter: Emitter): JsonElement = compile(source, emitter)
        .shouldBeRight { it.joinToString { e -> e.message } }
        .single().result.let(Json::parseToJsonElement)

    private fun JsonElement.at(vararg keys: String): String = keys.fold(this) { element, key -> (element as JsonObject).getValue(key) }.toString()

    private fun String.toWirespec(parser: Parser): String = WirespecEmitter()
        .emit(parser.parse(ModuleContent(FileUri("api.json"), this), true), noLogger)
        .joinToString("\n") { it.result }

    private val openAPIV3 =
        // language=json
        """
        {
          "openapi": "3.0.0",
          "info": { "title": "Pets", "version": "1.0.0" },
          "paths": {
            "/pets": {
              "post": {
                "operationId": "CreatePet",
                "requestBody": {
                  "required": true,
                  "content": {
                    "application/json": { "schema": { "${'$'}ref": "#/components/schemas/Pet" } },
                    "application/xml": { "schema": { "${'$'}ref": "#/components/schemas/Pet" } }
                  }
                },
                "responses": {
                  "200": {
                    "description": "The pet",
                    "content": {
                      "application/json": { "schema": { "${'$'}ref": "#/components/schemas/Pet" } },
                      "application/xml": { "schema": { "${'$'}ref": "#/components/schemas/Pet" } }
                    }
                  },
                  "400": {
                    "description": "The reason",
                    "content": { "text/plain": { "schema": { "type": "string" } } }
                  }
                }
              }
            }
          },
          "components": {
            "schemas": { "Pet": { "type": "object", "required": ["name"], "properties": { "name": { "type": "string" } } } }
          }
        }
        """.trimIndent()

    private val openAPIV2 =
        // language=json
        """
        {
          "swagger": "2.0",
          "info": { "title": "Pets", "version": "1.0.0" },
          "consumes": ["application/json"],
          "produces": ["application/json"],
          "paths": {
            "/pets": {
              "post": {
                "operationId": "CreatePet",
                "consumes": ["application/xml", "application/x-www-form-urlencoded"],
                "produces": ["application/xml"],
                "parameters": [{ "in": "body", "name": "RequestBody", "required": true, "schema": { "${'$'}ref": "#/definitions/Pet" } }],
                "responses": {
                  "200": { "description": "The pet", "schema": { "${'$'}ref": "#/definitions/Pet" } },
                  "204": { "description": "Nothing" }
                }
              }
            }
          },
          "definitions": { "Pet": { "type": "object", "required": ["name"], "properties": { "name": { "type": "string" } } } }
        }
        """.trimIndent()

    @Test
    fun openAPIV3ContentTypesSurviveWirespec() {
        val wirespec = openAPIV3.toWirespec(OpenAPIV3Parser)

        wirespec shouldContain """@OpenAPIContentType(["application/json", "application/xml"])"""
        wirespec shouldContain """@OpenAPIContentType("application/xml") 200 -> Pet"""
        wirespec shouldContain """@OpenAPIContentType("text/plain") 400 -> String"""

        val operation = document(wirespec, OpenAPIV3Emitter).at("paths", "/pets", "post").let(Json::parseToJsonElement)
        val pet = """{ "schema": { "${'$'}ref": "#/components/schemas/Pet" } }"""
        operation.at("requestBody", "content") shouldEqualJson """{ "application/json": $pet, "application/xml": $pet }"""
        operation.at("responses", "200", "content") shouldEqualJson """{ "application/json": $pet, "application/xml": $pet }"""
        operation.at("responses", "400", "content") shouldEqualJson """{ "text/plain": { "schema": { "type": "string" } } }"""
    }

    @Test
    fun openAPIV2ContentTypesSurviveWirespec() {
        val wirespec = openAPIV2.toWirespec(OpenAPIV2Parser)

        wirespec shouldContain """@OpenAPIContentType(["application/xml", "application/x-www-form-urlencoded"])"""
        wirespec shouldContain """@OpenAPIContentType("application/xml") 200 -> Pet"""
        wirespec shouldContain """@Description("Nothing") 204 -> Unit"""

        val operation = document(wirespec, OpenAPIV2Emitter).at("paths", "/pets", "post").let(Json::parseToJsonElement)
        operation.at("consumes") shouldEqualJson """["application/xml", "application/x-www-form-urlencoded"]"""
        operation.at("produces") shouldEqualJson """["application/xml"]"""
    }

    @Test
    fun withoutAnAnnotationABodyIsJson() {
        val source =
            // language=ws
            """
            |type Pet { name: String }
            |endpoint CreatePet POST Pet /pets -> {
            |  200 -> Pet
            |}
            """.trimMargin()

        val operation = document(source, OpenAPIV3Emitter).at("paths", "/pets", "post").let(Json::parseToJsonElement)
        (operation.at("requestBody", "content").let(Json::parseToJsonElement) as JsonObject).keys shouldContain "application/json"
        (operation.at("responses", "200", "content").let(Json::parseToJsonElement) as JsonObject).keys shouldContain "application/json"
    }

    @Test
    fun aContentTypeDoesNotApplyToTheSpec() {
        val source =
            // language=ws
            """
            |@spec:OpenAPIContentType("application/xml")
            |
            |type Pet { name: String }
            """.trimMargin()

        compile(source, OpenAPIV3Emitter).shouldBeLeft().map { it.message } shouldContain
            """@spec:OpenAPIContentType("application/xml") in api.ws does not apply to the spec: it goes on an endpoint or a response"""
    }
}
