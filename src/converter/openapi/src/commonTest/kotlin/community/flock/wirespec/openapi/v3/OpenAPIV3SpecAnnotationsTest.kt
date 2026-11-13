package community.flock.wirespec.openapi.v3

import arrow.core.EitherNel
import arrow.core.nonEmptySetOf
import arrow.core.toNonEmptyListOrNull
import community.flock.wirespec.compiler.core.CompilationContext
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.compile
import community.flock.wirespec.compiler.core.emit.SpecInfo
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.test.parse
import community.flock.wirespec.compiler.utils.NoLogger
import community.flock.wirespec.compiler.utils.noLogger
import community.flock.wirespec.openapi.v2.OpenAPIV2Emitter
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test

class OpenAPIV3SpecAnnotationsTest {

    private fun compile(vararg files: Pair<String, String>): EitherNel<WirespecException, JsonObject> = object : CompilationContext, NoLogger {
        override val emitters = nonEmptySetOf(OpenAPIV3Emitter)
    }.compile(files.map { (name, source) -> ModuleContent(FileUri(name), source) }.toNonEmptyListOrNull()!!)
        .map { Json.parseToJsonElement(it.single().result) as JsonObject }

    private fun errors(vararg files: Pair<String, String>): List<String> = compile(*files).shouldBeLeft().map { it.message }

    private fun JsonElement.at(vararg keys: String): String = keys.fold(this) { element, key -> (element as JsonObject).getValue(key) }.toString()

    private val api =
        // language=ws
        """
        |@spec:Description("Everything about customers")
        |@spec:OpenAPIInfo(title: "Customer API", version: "2.3.0", contact: { name: "Team", email: "team@example.com" }, license: { name: "Apache 2.0" })
        |@spec:OpenAPIServer("https://api.example.com/v2", description: "Production")
        |@spec:OpenAPIServer("https://{env}.example.com/v2", variables: { env: { default: "test", enum: ["test", "acc"] } })
        |@spec:OpenAPISecurityScheme("bearerAuth", type: "http", scheme: "bearer", bearerFormat: "JWT")
        |@spec:OpenAPISecurityScheme("oauth", type: "oauth2", flows: { clientCredentials: { tokenUrl: "https://auth.example.com/token", scopes: { "customers:write": "Change customers" } } })
        |@spec:OpenAPISecurity("bearerAuth")
        """.trimMargin()

    private val customers =
        // language=ws
        """
        |@spec:OpenAPITag("Customers", description: "Manage customers")
        |
        |type Customer {
        |  id: String
        |}
        |
        |@OpenAPITag("Customers")
        |endpoint GetCustomer GET /customers/{id: String} -> {
        |  200 -> Customer
        |}
        |
        |@OpenAPITag("Customers")
        |@OpenAPITag("Admin")
        |@OpenAPISecurity("oauth", scopes: ["customers:write"])
        |@OpenAPISecurity(["bearerAuth", "oauth"])
        |@OpenAPIServer("https://files.example.com/v2")
        |endpoint CreateCustomer POST Customer /customers -> {
        |  201 -> Customer
        |}
        """.trimMargin()

    private val monitoring =
        // language=ws
        """
        |@spec:OpenAPITag("Monitoring", description: "Health checks")
        |
        |@OpenAPITag("Monitoring")
        |@OpenAPINoSecurity
        |endpoint Health GET /health -> {
        |  200 -> Unit
        |}
        """.trimMargin()

    @Test
    fun specAnnotationsDescribeTheDocument() {
        val document = compile("api.ws" to api, "customers.ws" to customers, "monitoring.ws" to monitoring).shouldBeRight { it.joinToString { e -> e.message } }

        document.at("info") shouldEqualJson """
            {
              "title": "Customer API",
              "description": "Everything about customers",
              "contact": { "name": "Team", "email": "team@example.com" },
              "license": { "name": "Apache 2.0" },
              "version": "2.3.0"
            }
        """.trimIndent()
        document.at("servers") shouldEqualJson """
            [
              { "url": "https://api.example.com/v2", "description": "Production" },
              { "url": "https://{env}.example.com/v2", "variables": { "env": { "enum": ["test", "acc"], "default": "test" } } }
            ]
        """.trimIndent()
        document.at("security") shouldEqualJson """[{ "bearerAuth": [] }]"""
        document.at("tags") shouldEqualJson """
            [
              { "name": "Customers", "description": "Manage customers" },
              { "name": "Monitoring", "description": "Health checks" }
            ]
        """.trimIndent()
        document.at("components", "securitySchemes") shouldEqualJson """
            {
              "bearerAuth": { "type": "http", "scheme": "bearer", "bearerFormat": "JWT" },
              "oauth": {
                "type": "oauth2",
                "flows": { "clientCredentials": { "tokenUrl": "https://auth.example.com/token", "scopes": { "customers:write": "Change customers" } } }
              }
            }
        """.trimIndent()
    }

    @Test
    fun endpointAnnotationsDescribeTheOperation() {
        val document = compile("api.ws" to api, "customers.ws" to customers, "monitoring.ws" to monitoring).shouldBeRight { it.joinToString { e -> e.message } }

        document.at("paths", "/customers/{id}", "get", "tags") shouldEqualJson """["Customers"]"""
        (document.at("paths", "/customers/{id}", "get").let(Json::parseToJsonElement) as JsonObject).keys.contains("security") shouldBe false
        document.at("paths", "/customers", "post", "tags") shouldEqualJson """["Customers", "Admin"]"""
        document.at("paths", "/customers", "post", "security") shouldEqualJson """[{ "oauth": ["customers:write"] }, { "bearerAuth": [], "oauth": [] }]"""
        document.at("paths", "/customers", "post", "servers") shouldEqualJson """[{ "url": "https://files.example.com/v2" }]"""
        document.at("paths", "/health", "get", "security") shouldEqualJson "[]"
    }

    @Test
    fun aRequirementWithScopesForSeveralSchemesIsADictionary() {
        val source =
            // language=ws
            """
            |@spec:OpenAPISecurityScheme("oauth", type: "oauth2", flows: { implicit: { authorizationUrl: "https://auth.example.com", scopes: { read: "Read" } } })
            |@spec:OpenAPISecurityScheme("mtls", type: "http", scheme: "mutual")
            |
            |@OpenAPISecurity({ oauth: ["read"], mtls: [] })
            |endpoint GetCustomer GET /customers -> {
            |  200 -> Unit
            |}
            """.trimMargin()

        compile("api.ws" to source).shouldBeRight { it.joinToString { e -> e.message } }
            .at("paths", "/customers", "get", "security") shouldEqualJson """[{ "oauth": ["read"], "mtls": [] }]"""
    }

    @Test
    fun withoutSpecAnnotationsTheDocumentIsAsBefore() {
        val document = compile("customers.ws" to "type Customer { id: String }").shouldBeRight { it.joinToString { e -> e.message } }

        document.at("info") shouldEqualJson """{ "title": "Wirespec", "version": "0.0.0" }"""
        document.keys shouldBe setOf("openapi", "info", "paths", "components")
    }

    @Test
    fun securityMustNameADeclaredScheme() {
        val source =
            // language=ws
            """
            |@spec:OpenAPISecurity("bearerAuth")
            |
            |@OpenAPISecurity("bearAuth")
            |endpoint GetCustomer GET /customers -> {
            |  200 -> Unit
            |}
            """.trimMargin()

        errors("api.ws" to source) shouldContainExactlyInAnyOrder listOf(
            "@OpenAPISecurity on the spec names the security scheme bearerAuth, which no @spec:OpenAPISecurityScheme declares (api.ws)",
            "@OpenAPISecurity on GetCustomer names the security scheme bearAuth, which no @spec:OpenAPISecurityScheme declares (api.ws)",
        )
    }

    @Test
    fun onlyOAuthAndOpenIdConnectRequirementsHaveScopes() {
        val source =
            // language=ws
            """
            |@spec:OpenAPISecurityScheme("bearerAuth", type: "http", scheme: "bearer")
            |
            |@OpenAPISecurity("bearerAuth", scopes: ["admin"])
            |endpoint GetCustomer GET /customers -> {
            |  200 -> Unit
            |}
            """.trimMargin()

        errors("api.ws" to source) shouldBe listOf(
            "@OpenAPISecurity on GetCustomer gives scopes for bearerAuth, but only oauth2 and openIdConnect schemes have scopes (api.ws)",
        )
    }

    @Test
    fun aSecuritySchemeMustBeComplete() {
        val source =
            // language=ws
            """
            |@spec:OpenAPISecurityScheme("noType")
            |@spec:OpenAPISecurityScheme("http", type: "http")
            |@spec:OpenAPISecurityScheme("apiKey", type: "apiKey", name: "X-Api-Key", in: "body")
            |@spec:OpenAPISecurityScheme("oauth", type: "oauth2")
            |@spec:OpenAPISecurityScheme("oidc", type: "openIdConnect")
            |@spec:OpenAPISecurityScheme("saml", type: "saml")
            |
            |type Customer { id: String }
            """.trimMargin()

        errors("api.ws" to source) shouldBe listOf(
            "@spec:OpenAPISecurityScheme(\"noType\") in api.ws has no type: use apiKey, http, oauth2 or openIdConnect",
            "@spec:OpenAPISecurityScheme(\"http\") in api.ws needs a scheme, such as bearer or basic",
            "@spec:OpenAPISecurityScheme(\"apiKey\") in api.ws needs a name and in: query, header or cookie",
            "@spec:OpenAPISecurityScheme(\"oauth\") in api.ws needs flows",
            "@spec:OpenAPISecurityScheme(\"oidc\") in api.ws needs an openIdConnectUrl",
            "@spec:OpenAPISecurityScheme(\"saml\") in api.ws has an unknown type saml: use apiKey, http, oauth2 or openIdConnect",
        )
    }

    @Test
    fun specAnnotationsFromSeveralFilesMustAgree() {
        val first =
            // language=ws
            """
            |@spec:OpenAPIInfo(title: "Customer API", version: "1.0.0")
            |@spec:OpenAPISecurityScheme("bearerAuth", type: "http", scheme: "bearer")
            |@spec:OpenAPISecurity("bearerAuth")
            |@spec:OpenAPIServer("https://api.example.com", description: "Production")
            |@spec:OpenAPITag("Customers", description: "Manage customers")
            """.trimMargin()
        val second =
            // language=ws
            """
            |@spec:OpenAPIInfo(title: "Order API", version: "1.0.0")
            |@spec:OpenAPISecurityScheme("bearerAuth", type: "http", scheme: "bearer", bearerFormat: "JWT")
            |@spec:OpenAPISecurity(["bearerAuth"])
            |@spec:OpenAPIServer("https://api.example.com", description: "Prod")
            |@spec:OpenAPITag("Customers", description: "Customers")
            |
            |type Order { id: String }
            """.trimMargin()

        errors("a.ws" to first, "b.ws" to second) shouldBe listOf(
            "@spec:OpenAPIInfo is declared 2 times, in a.ws and b.ws: a spec has one",
            "@spec:OpenAPISecurity is declared in a.ws and b.ws: the default security comes from one file, so that no file can add an alternative that weakens it",
            "The security scheme \"bearerAuth\" is declared 2 times, in a.ws and b.ws: a spec declares each security scheme once",
            "The server \"https://api.example.com\" is declared 2 times, in a.ws and b.ws: a spec declares each server once",
            "The tag \"Customers\" is declared 2 times, in a.ws and b.ws: a spec declares each tag once",
        )
    }

    @Test
    fun theSameSpecAnnotationTwiceIsAnError() {
        val first =
            // language=ws
            """
            |@spec:OpenAPISecurityScheme("bearerAuth", type: "http", scheme: "bearer")
            |@spec:OpenAPIServer("https://api.example.com")
            """.trimMargin()
        val second =
            // language=ws
            """
            |@spec:OpenAPISecurityScheme("bearerAuth", type: "http", scheme: "bearer")
            |@spec:OpenAPIServer("https://api.example.com")
            |@spec:OpenAPIServer("https://sandbox.example.com")
            |
            |type Order { id: String }
            """.trimMargin()

        errors("a.ws" to first, "b.ws" to second) shouldBe listOf(
            "@spec:OpenAPISecurityScheme(\"bearerAuth\") is declared 2 times, in a.ws and b.ws: a spec annotation is unique",
            "@spec:OpenAPIServer(\"https://api.example.com\") is declared 2 times, in a.ws and b.ws: a spec annotation is unique",
        )
    }

    @Test
    fun serversAndTagsFromSeveralFilesAreCollected() {
        val first =
            // language=ws
            """
            |@spec:OpenAPIServer("https://api.example.com")
            |@spec:OpenAPITag("Customers")
            """.trimMargin()
        val second =
            // language=ws
            """
            |@spec:OpenAPIServer("https://sandbox.example.com")
            |@spec:OpenAPITag("Orders")
            |
            |type Order { id: String }
            """.trimMargin()

        compile("a.ws" to first, "b.ws" to second).shouldBeRight { it.joinToString { e -> e.message } }.run {
            at("servers") shouldEqualJson """[{ "url": "https://api.example.com" }, { "url": "https://sandbox.example.com" }]"""
            at("tags") shouldEqualJson """[{ "name": "Customers" }, { "name": "Orders" }]"""
        }
    }

    @Test
    fun annotationsMustHaveTheRightTarget() {
        val source =
            // language=ws
            """
            |@spec:OpenAPINoSecurity
            |@spec:OpenAPILink("GetCustomer", operationId: "GetCustomer")
            |@file:OpenAPITag("Customers")
            |
            |@OpenAPIInfo(title: "Customer API")
            |@OpenAPISecurityScheme("bearerAuth", type: "http", scheme: "bearer")
            |@OpenAPISecurity("bearerAuth")
            |@OpenAPINoSecurity
            |endpoint GetCustomer GET /customers -> {
            |  200 -> Unit
            |}
            """.trimMargin()

        errors("api.ws" to source) shouldBe listOf(
            "@spec:OpenAPINoSecurity in api.ws does not apply to the spec: it goes on an endpoint",
            "@spec:OpenAPILink in api.ws does not apply to the spec: it goes on a response",
            "@file:OpenAPITag in api.ws: an OpenAPI document has no metadata per file, so write @spec:OpenAPITag",
            "@OpenAPIInfo on GetCustomer describes the document: write it in the file header as @spec:OpenAPIInfo (api.ws)",
            "@OpenAPISecurityScheme on GetCustomer describes the document: write it in the file header as @spec:OpenAPISecurityScheme (api.ws)",
            "GetCustomer has both @OpenAPISecurity and @OpenAPINoSecurity (api.ws)",
            "@OpenAPISecurity on GetCustomer names the security scheme bearerAuth, which no @spec:OpenAPISecurityScheme declares (api.ws)",
        )
    }

    @Test
    fun serversAndTagsNeedTheirValue() {
        val source =
            // language=ws
            """
            |@spec:OpenAPIServer(description: "Production")
            |@spec:OpenAPIServer("https://{env}.example.com", variables: { env: { enum: ["test"] } })
            |@spec:OpenAPITag(description: "Customers")
            |
            |type Customer { id: String }
            """.trimMargin()

        errors("api.ws" to source) shouldBe listOf(
            "@spec:OpenAPIServer has no URL in api.ws",
            "@spec:OpenAPIServer(\"https://{env}.example.com\") has a variable env without a default in api.ws",
            "@spec:OpenAPITag has no name in api.ws",
        )
    }

    @Test
    fun theBuildCanSetTheTitleAndVersion() {
        val source = "@spec:OpenAPIInfo(title: \"Customer API\", version: \"2.3.0\")\n\ntype Customer { id: String }"
        val ast = parse(source).shouldBeRight()

        listOf(OpenAPIV3Emitter, OpenAPIV2Emitter).map { it.withSpecInfo(SpecInfo(version = "2.4.0-SNAPSHOT")) }.forEach { emitter ->
            Json.parseToJsonElement(emitter.emit(ast, noLogger).single().result)
                .at("info") shouldEqualJson """{ "title": "Customer API", "version": "2.4.0-SNAPSHOT" }"""
        }
    }
}
