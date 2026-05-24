package community.flock.wirespec.lsp

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LspServerTest {

    private lateinit var transport: TestTransport
    private lateinit var server: LspServer

    @BeforeTest
    fun setup() {
        transport = TestTransport()
        server = LspServer(transport)
        server.start()
    }

    @Test
    fun `initialize advertises diagnostics, semantic tokens, definition and rename`() {
        val response = transport.request(
            "initialize",
            buildJsonObject { put("processId", JsonNull) },
        )

        val capabilities = response["result"]!!.jsonObject["capabilities"]!!.jsonObject
        assertNotNull(capabilities["textDocumentSync"])
        assertNotNull(capabilities["semanticTokensProvider"])
        assertEquals(JsonPrimitive(true), capabilities["definitionProvider"])
        assertEquals(JsonPrimitive(true), capabilities["renameProvider"]!!.jsonObject["prepareProvider"])
    }

    @Test
    fun `didOpen on a well-formed file publishes empty diagnostics`() {
        initialize()
        openDocument(URI, Fixtures.TODOS)

        val published = transport.notificationsOf("textDocument/publishDiagnostics")
        assertEquals(1, published.size, "expected exactly one publishDiagnostics notification")
        val params = published.single()["params"]!!.jsonObject
        assertEquals(URI, params["uri"]!!.jsonPrimitive.content)
        assertEquals(JsonArray(emptyList()), params["diagnostics"]!!.jsonArray)
    }

    @Test
    fun `didOpen on a broken file publishes at least one diagnostic with a range`() {
        initialize()
        openDocument(URI, Fixtures.BROKEN)

        val published = transport.notificationsOf("textDocument/publishDiagnostics").single()
        val diagnostics = published["params"]!!.jsonObject["diagnostics"]!!.jsonArray
        assertTrue(diagnostics.size >= 1, "expected at least one diagnostic; got $diagnostics")
        val first = diagnostics.first().jsonObject
        assertNotNull(first["range"])
        assertNotNull(first["message"])
        assertEquals(1, first["severity"]!!.jsonPrimitive.int, "expected severity = ERROR (1)")
    }

    @Test
    fun `semantic tokens are delta-encoded in groups of five`() {
        initialize()
        openDocument(URI, Fixtures.TODOS)

        val response = transport.request(
            "textDocument/semanticTokens/full",
            buildJsonObject { put("textDocument", buildJsonObject { put("uri", URI) }) },
        )

        val data = response["result"]!!.jsonObject["data"]!!.jsonArray
        assertTrue(data.isNotEmpty(), "expected at least one semantic token")
        assertEquals(0, data.size % 5, "semantic token data must come in 5-int groups (deltaLine, deltaChar, length, type, modifiers)")
        // First token: `type` keyword at line 0, char 0 — encoded as (0, 0, 4, KEYWORD, 0).
        assertEquals(0, data[0].jsonPrimitive.int, "first deltaLine")
        assertEquals(0, data[1].jsonPrimitive.int, "first deltaChar")
        assertEquals(4, data[2].jsonPrimitive.int, "first token length should be 4 (`type`)")
        assertEquals(SemanticTokenLegend.TYPE_KEYWORD, data[3].jsonPrimitive.int, "first token should be a keyword")
    }

    @Test
    fun `go-to-definition on a type reference returns the declaration and the reference`() {
        initialize()
        openDocument(URI, Fixtures.TODOS)

        // `owner: Person,` is on line 11, char 9..15. Click inside `Person`.
        val response = transport.request(
            "textDocument/definition",
            positionParams(line = 11, character = 12),
        )

        val locations = response["result"]!!.jsonArray
        assertEquals(2, locations.size, "expected the declaration and one reference; got $locations")
        val refLines = locations.map { it.jsonObject["range"]!!.jsonObject["start"]!!.jsonObject["line"]!!.jsonPrimitive.int }.toSet()
        assertEquals(setOf(2, 11), refLines, "should jump between line 2 (declaration `type Person`) and line 11 (reference)")
    }

    @Test
    fun `prepareRename accepts a user-defined type identifier`() {
        initialize()
        openDocument(URI, Fixtures.TODOS)

        // `type Person {` on line 2; the identifier `Person` starts at char 5 and is 6 chars long.
        val response = transport.request("textDocument/prepareRename", positionParams(line = 2, character = 7))

        val range = response["result"]!!.jsonObject
        assertEquals(2, range["start"]!!.jsonObject["line"]!!.jsonPrimitive.int)
        assertEquals(5, range["start"]!!.jsonObject["character"]!!.jsonPrimitive.int)
        assertEquals(2, range["end"]!!.jsonObject["line"]!!.jsonPrimitive.int)
        assertEquals(11, range["end"]!!.jsonObject["character"]!!.jsonPrimitive.int)
    }

    @Test
    fun `prepareRename refuses keywords, built-in types and field identifiers`() {
        initialize()
        openDocument(URI, Fixtures.TODOS)

        // line 0, char 1 → inside the `type` keyword
        assertNullResult(transport.request("textDocument/prepareRename", positionParams(line = 0, character = 1)))
        // line 4, char 14 → inside the built-in `Name`? no, `Name` IS user-defined. Use line 3 `Integer` instead.
        // line 3 is `  id: Integer,` (within Person) — Integer is at char 6..13.
        assertNullResult(transport.request("textDocument/prepareRename", positionParams(line = 3, character = 9)))
        // line 4, char 2 → inside the field name `firstname`
        assertNullResult(transport.request("textDocument/prepareRename", positionParams(line = 4, character = 3)))
    }

    @Test
    fun `rename produces edits for the declaration and every reference`() {
        initialize()
        openDocument(URI, Fixtures.TODOS)

        // Rename `Person` (declared on line 2) to `User`.
        val response = transport.request(
            "textDocument/rename",
            buildJsonObject {
                put("textDocument", buildJsonObject { put("uri", URI) })
                put("position", buildJsonObject {
                    put("line", 2)
                    put("character", 7)
                })
                put("newName", "User")
            },
        )

        val edits = response["result"]!!.jsonObject["changes"]!!.jsonObject[URI]!!.jsonArray
        assertEquals(2, edits.size, "expected one edit for declaration and one for the `owner: Person` reference; got $edits")
        edits.forEach { edit ->
            assertEquals("User", edit.jsonObject["newText"]!!.jsonPrimitive.content)
        }
        val editedLines = edits.map { it.jsonObject["range"]!!.jsonObject["start"]!!.jsonObject["line"]!!.jsonPrimitive.int }.toSet()
        assertEquals(setOf(2, 11), editedLines)
    }

    @Test
    fun `rename refuses an invalid PascalCase identifier`() {
        initialize()
        openDocument(URI, Fixtures.TODOS)

        val response = transport.request(
            "textDocument/rename",
            buildJsonObject {
                put("textDocument", buildJsonObject { put("uri", URI) })
                put("position", buildJsonObject {
                    put("line", 2)
                    put("character", 7)
                })
                put("newName", "lowercase")
            },
        )

        assertNullResult(response)
    }

    @Test
    fun `didChange refreshes diagnostics`() {
        initialize()
        openDocument(URI, Fixtures.TODOS)
        transport.reset()

        // Replace the document with broken content.
        transport.notify(
            "textDocument/didChange",
            buildJsonObject {
                put("textDocument", buildJsonObject {
                    put("uri", URI)
                    put("version", 2)
                })
                put("contentChanges", JsonArray(listOf(buildJsonObject { put("text", Fixtures.BROKEN) })))
            },
        )

        val published = transport.notificationsOf("textDocument/publishDiagnostics").single()
        val diagnostics = published["params"]!!.jsonObject["diagnostics"]!!.jsonArray
        assertNotEquals(0, diagnostics.size, "expected diagnostics after didChange to broken content")
    }

    private fun initialize() {
        transport.request("initialize", buildJsonObject { put("processId", JsonNull) })
        transport.notify("initialized", buildJsonObject {})
        transport.reset()
    }

    private fun openDocument(uri: String, text: String) {
        transport.notify(
            "textDocument/didOpen",
            buildJsonObject {
                put("textDocument", buildJsonObject {
                    put("uri", uri)
                    put("languageId", "wirespec")
                    put("version", 1)
                    put("text", text)
                })
            },
        )
    }

    private fun positionParams(line: Int, character: Int): JsonObject = buildJsonObject {
        put("textDocument", buildJsonObject { put("uri", URI) })
        put("position", buildJsonObject {
            put("line", line)
            put("character", character)
        })
    }

    private fun assertNullResult(response: JsonObject) {
        assertEquals(JsonNull, response["result"], "expected null result, got ${response["result"]}")
    }

    companion object {
        private const val URI = "file:///example.ws"
    }
}
