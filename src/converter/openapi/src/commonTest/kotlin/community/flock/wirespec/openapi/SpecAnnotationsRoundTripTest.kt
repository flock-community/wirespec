package community.flock.wirespec.openapi

import arrow.core.nonEmptyListOf
import arrow.core.nonEmptySetOf
import community.flock.wirespec.compiler.core.CompilationContext
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.compile
import community.flock.wirespec.compiler.core.emit.Emitter
import community.flock.wirespec.compiler.utils.NoLogger
import community.flock.wirespec.compiler.utils.noLogger
import community.flock.wirespec.emitters.wirespec.WirespecEmitter
import community.flock.wirespec.openapi.v2.OpenAPIV2Emitter
import community.flock.wirespec.openapi.v2.OpenAPIV2Parser
import community.flock.wirespec.openapi.v3.OpenAPIV3Emitter
import community.flock.wirespec.openapi.v3.OpenAPIV3Parser
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test

class SpecAnnotationsRoundTripTest {

    private fun compile(source: String, emitter: Emitter): JsonObject = object : CompilationContext, NoLogger {
        override val emitters = nonEmptySetOf(emitter)
    }.compile(nonEmptyListOf(ModuleContent(FileUri("converted.ws"), source)))
        .shouldBeRight { it.joinToString { e -> e.message } }
        .let { Json.parseToJsonElement(it.single().result) as JsonObject }

    private fun JsonElement.at(vararg keys: String): String = keys.fold(this) { element, key -> (element as JsonObject).getValue(key) }.toString()

    @Test
    fun openAPIV3MetadataSurvivesTheConversionToWirespecAndBack() {
        val original = SystemFileSystem.source(Path("src/commonTest/resources/v3/security.json")).buffered().readString()
        val wirespec = OpenAPIV3Parser.parse(ModuleContent(FileUri("security.json"), original), false)
            .let { WirespecEmitter().emit(it, noLogger).single().result }

        wirespec shouldStartWith """
            |@spec:Description("Everything about customers")
            |@spec:OpenAPIInfo(title: "Customer API", version: "2.3.0", termsOfService: "https://example.com/terms", contact: { name: "Team", url: "https://example.com", email: "team@example.com" }, license: { name: "Apache 2.0", url: "https://www.apache.org/licenses/LICENSE-2.0" })
            |@spec:OpenAPIServer("https://api.example.com/v2", description: "Production")
            |@spec:OpenAPIServer("https://{env}.example.com/v2", description: "Test", variables: { env: { default: "test", enum: ["test", "acc"], description: "The environment" } })
            |@spec:OpenAPISecurityScheme("bearerAuth", type: "http", description: "A JWT", scheme: "bearer", bearerFormat: "JWT")
            |@spec:OpenAPISecurityScheme("apiKey", type: "apiKey", name: "X-Api-Key", in: "header")
        """.trimMargin()

        val document = Json.parseToJsonElement(original)
        val roundTripped = compile(wirespec, OpenAPIV3Emitter)

        listOf(
            arrayOf("info"),
            arrayOf("servers"),
            arrayOf("security"),
            arrayOf("tags"),
            arrayOf("components", "securitySchemes"),
            arrayOf("paths", "/customers/{id}", "get", "tags"),
            arrayOf("paths", "/customers", "post", "tags"),
            arrayOf("paths", "/customers", "post", "security"),
            arrayOf("paths", "/customers", "post", "servers"),
            arrayOf("paths", "/health", "get", "security"),
        ).forEach { keys -> roundTripped.at(*keys) shouldEqualJson document.at(*keys) }
    }

    @Test
    fun swaggerMetadataSurvivesTheConversionToWirespecAndBack() {
        val original =
            // language=json
            """
            {
              "swagger": "2.0",
              "info": { "title": "Customer API", "description": "Everything about customers", "version": "2.3.0" },
              "host": "api.example.com",
              "basePath": "/v2",
              "schemes": ["https", "http"],
              "tags": [{ "name": "Customers", "description": "Manage customers" }],
              "securityDefinitions": {
                "basic": { "type": "basic" },
                "apiKey": { "type": "apiKey", "name": "X-Api-Key", "in": "header" },
                "oauth": {
                  "type": "oauth2",
                  "flow": "accessCode",
                  "authorizationUrl": "https://auth.example.com/authorize",
                  "tokenUrl": "https://auth.example.com/token",
                  "scopes": { "customers:read": "Read customers" }
                }
              },
              "security": [{ "apiKey": [] }],
              "paths": {
                "/customers": {
                  "get": {
                    "tags": ["Customers"],
                    "operationId": "GetCustomers",
                    "responses": { "200": { "description": "The customers" } },
                    "security": [{ "oauth": ["customers:read"] }, { "basic": [] }]
                  }
                }
              }
            }
            """.trimIndent()
        val wirespec = OpenAPIV2Parser.parse(ModuleContent(FileUri("swagger.json"), original), false)
            .let { WirespecEmitter().emit(it, noLogger).single().result }

        wirespec shouldStartWith """
            |@spec:Description("Everything about customers")
            |@spec:OpenAPIInfo(title: "Customer API", version: "2.3.0")
            |@spec:OpenAPIServer("https://api.example.com/v2")
            |@spec:OpenAPIServer("http://api.example.com/v2")
            |@spec:OpenAPISecurityScheme("basic", type: "http", scheme: "basic")
        """.trimMargin()

        val document = Json.parseToJsonElement(original)
        val roundTripped = compile(wirespec, OpenAPIV2Emitter)

        listOf(
            arrayOf("info"),
            arrayOf("host"),
            arrayOf("basePath"),
            arrayOf("schemes"),
            arrayOf("tags"),
            arrayOf("securityDefinitions"),
            arrayOf("security"),
            arrayOf("paths", "/customers", "get", "tags"),
            arrayOf("paths", "/customers", "get", "security"),
        ).forEach { keys -> roundTripped.at(*keys) shouldEqualJson document.at(*keys) }
    }

    @Test
    fun swaggerWritesWhatItCanOfOpenAPI3Metadata() {
        val source =
            // language=ws
            """
            |@spec:OpenAPIServer("https://{env}.example.com/v2", variables: { env: { default: "api" } })
            |@spec:OpenAPIServer("https://other.example.com")
            |@spec:OpenAPISecurityScheme("bearerAuth", type: "http", scheme: "bearer", bearerFormat: "JWT")
            |@spec:OpenAPISecurityScheme("oidc", type: "openIdConnect", openIdConnectUrl: "https://auth.example.com")
            |@spec:OpenAPISecurityScheme("oauth", type: "oauth2", flows: { clientCredentials: { tokenUrl: "https://auth.example.com/token", scopes: { read: "Read" } } })
            |@spec:OpenAPISecurity("oidc")
            |@spec:OpenAPISecurity("bearerAuth")
            |
            |@OpenAPISecurity("oauth", scopes: ["read"])
            |endpoint GetCustomers GET /customers -> {
            |  200 -> Unit
            |}
            """.trimMargin()

        val document = compile(source, OpenAPIV2Emitter)

        document.at("host") shouldEqualJson "\"api.example.com\""
        document.at("basePath") shouldEqualJson "\"/v2\""
        document.at("schemes") shouldEqualJson """["https"]"""
        document.at("securityDefinitions") shouldEqualJson """
            {
              "bearerAuth": { "type": "apiKey", "description": "A bearer token (JWT) in the Authorization header: Bearer <token>", "name": "Authorization", "in": "header" },
              "oauth": { "type": "oauth2", "flow": "application", "tokenUrl": "https://auth.example.com/token", "scopes": { "read": "Read" } }
            }
        """.trimIndent()
        document.at("security") shouldEqualJson """[{ "bearerAuth": [] }]"""
        document.at("paths", "/customers", "get", "security") shouldEqualJson """[{ "oauth": ["read"] }]"""
        (document["paths"] as JsonObject).keys shouldBe setOf("/customers")
    }
}
