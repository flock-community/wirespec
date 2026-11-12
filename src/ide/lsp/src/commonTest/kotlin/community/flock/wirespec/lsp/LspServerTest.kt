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
import kotlin.test.assertTrue

class LspServerTest {

    private lateinit var transport: TestTransport
    private lateinit var server: LspServer
    private val filesOnDisk = mutableMapOf<String, Map<String, String>>()

    @BeforeTest
    fun setup() {
        transport = TestTransport()
        server = LspServer(transport) { directoryUri -> filesOnDisk[directoryUri].orEmpty() }
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
        assertTrue(diagnostics.isNotEmpty(), "expected at least one diagnostic; got $diagnostics")
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
                put(
                    "position",
                    buildJsonObject {
                        put("line", 2)
                        put("character", 7)
                    },
                )
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
                put(
                    "position",
                    buildJsonObject {
                        put("line", 2)
                        put("character", 7)
                    },
                )
                put("newName", "lowercase")
            },
        )

        assertNullResult(response)
    }

    @Test
    fun `a part that is never spread gets a warning on its name`() {
        initialize()
        openDocument(URI, Fixtures.PARTS)

        val diagnostic = transport.notificationsOf("textDocument/publishDiagnostics").single()["params"]!!
            .jsonObject["diagnostics"]!!.jsonArray
            .single().jsonObject
        assertEquals(2, diagnostic["severity"]!!.jsonPrimitive.int, "expected severity = WARNING (2)")
        assertEquals("Part 'Unused' is never spread", diagnostic["message"]!!.jsonPrimitive.content)
        val range = diagnostic["range"]!!.jsonObject
        assertEquals(4, range["start"]!!.jsonObject["line"]!!.jsonPrimitive.int)
        assertEquals(5, range["start"]!!.jsonObject["character"]!!.jsonPrimitive.int)
        assertEquals(11, range["end"]!!.jsonObject["character"]!!.jsonPrimitive.int)
    }

    @Test
    fun `go-to-definition on a spread returns the part and the spread`() {
        initialize()
        openDocument(URI, Fixtures.PARTS)

        // `  ...Audited,` is on line 9; `Audited` starts at char 5.
        val response = transport.request("textDocument/definition", positionParams(line = 9, character = 7))

        val lines = response["result"]!!.jsonArray
            .map { it.jsonObject["range"]!!.jsonObject["start"]!!.jsonObject["line"]!!.jsonPrimitive.int }
            .toSet()
        assertEquals(setOf(0, 9), lines, "should jump between line 0 (`part Audited`) and line 9 (the spread)")
    }

    @Test
    fun `rename on a part renames its declaration and every spread`() {
        initialize()
        openDocument(URI, Fixtures.PARTS)

        val response = transport.request(
            "textDocument/rename",
            buildJsonObject {
                put("textDocument", buildJsonObject { put("uri", URI) })
                put(
                    "position",
                    buildJsonObject {
                        put("line", 0)
                        put("character", 7)
                    },
                )
                put("newName", "Audit")
            },
        )

        val edits = response["result"]!!.jsonObject["changes"]!!.jsonObject[URI]!!.jsonArray
        assertEquals(
            setOf(0 to 5, 9 to 5),
            edits.map { it.jsonObject["range"]!!.jsonObject["start"]!!.jsonObject.let { start -> start["line"]!!.jsonPrimitive.int to start["character"]!!.jsonPrimitive.int } }.toSet(),
        )
        assertTrue(edits.all { it.jsonObject["newText"]!!.jsonPrimitive.content == "Audit" })
    }

    @Test
    fun `part is a keyword and a spread is an operator`() {
        initialize()
        openDocument(URI, Fixtures.PARTS)

        val response = transport.request(
            "textDocument/semanticTokens/full",
            buildJsonObject { put("textDocument", buildJsonObject { put("uri", URI) }) },
        )

        val tokens = response["result"]!!.jsonObject["data"]!!.jsonArray.map { it.jsonPrimitive.int }.chunked(5)
        assertEquals(listOf(0, 0, 4, SemanticTokenLegend.TYPE_KEYWORD, 0), tokens.first(), "`part` at line 0, char 0")
        assertTrue(
            tokens.any { (_, _, length, type) -> length == 3 && type == SemanticTokenLegend.TYPE_OPERATOR },
            "expected `...` as an operator token; got $tokens",
        )
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
                put(
                    "textDocument",
                    buildJsonObject {
                        put("uri", URI)
                        put("version", 2)
                    },
                )
                put("contentChanges", JsonArray(listOf(buildJsonObject { put("text", Fixtures.BROKEN) })))
            },
        )

        val published = transport.notificationsOf("textDocument/publishDiagnostics").single()
        val diagnostics = published["params"]!!.jsonObject["diagnostics"]!!.jsonArray
        assertNotEquals(0, diagnostics.size, "expected diagnostics after didChange to broken content")
    }

    @Test
    fun `a type defined in a sibling file on disk resolves`() {
        filesOnDisk[DIRECTORY] = mapOf("name.ws" to Fixtures.NAME, "person.ws" to "stale content on disk {")
        initialize()
        openDocument(PERSON_URI, Fixtures.PERSON)

        assertEquals(JsonArray(emptyList()), diagnosticsOf(PERSON_URI))
    }

    @Test
    fun `a type that no file in the directory defines is still an error`() {
        initialize()
        openDocument(PERSON_URI, Fixtures.PERSON)

        assertEquals(1, diagnosticsOf(PERSON_URI).size)
    }

    @Test
    fun `an error in a sibling file is not reported on the open document`() {
        filesOnDisk[DIRECTORY] = mapOf("name.ws" to Fixtures.NAME, "broken.ws" to Fixtures.BROKEN)
        initialize()
        openDocument(PERSON_URI, Fixtures.PERSON)

        assertEquals(JsonArray(emptyList()), diagnosticsOf(PERSON_URI))
    }

    @Test
    fun `an open sibling wins over its copy on disk and refreshes the documents around it`() {
        filesOnDisk[DIRECTORY] = mapOf("name.ws" to "type Other = String")
        initialize()
        openDocument(PERSON_URI, Fixtures.PERSON)
        assertEquals(1, diagnosticsOf(PERSON_URI).size)

        openDocument(NAME_URI, Fixtures.NAME)

        assertEquals(JsonArray(emptyList()), diagnosticsOf(PERSON_URI))
        assertEquals(JsonArray(emptyList()), diagnosticsOf(NAME_URI))
    }

    @Test
    fun `a part spread in a sibling file is not unused`() {
        filesOnDisk[DIRECTORY] = mapOf("todo.ws" to "type Todo {\n  ...Audited,\n  ...Unused\n}\n")
        initialize()
        openDocument("$DIRECTORY/parts.ws", "part Audited {\n  createdAt: String\n}\n\npart Unused {\n  note: String\n}\n")

        assertEquals(JsonArray(emptyList()), diagnosticsOf("$DIRECTORY/parts.ws"))
    }

    @Test
    fun `go-to-definition and rename reach into sibling files`() {
        filesOnDisk[DIRECTORY] = mapOf("name.ws" to Fixtures.NAME)
        initialize()
        openDocument(PERSON_URI, Fixtures.PERSON)
        // `  firstname: Name` — cursor on `Name`.
        val position = buildJsonObject {
            put("textDocument", buildJsonObject { put("uri", PERSON_URI) })
            put(
                "position",
                buildJsonObject {
                    put("line", 1)
                    put("character", 14)
                },
            )
        }

        val locations = transport.request("textDocument/definition", position)["result"]!!.jsonArray
        assertEquals(
            listOf(PERSON_URI, NAME_URI),
            locations.map { it.jsonObject["uri"]!!.jsonPrimitive.content },
        )

        val changes = transport
            .request("textDocument/rename", JsonObject(position + ("newName" to JsonPrimitive("FullName"))))["result"]!!
            .jsonObject["changes"]!!.jsonObject
        assertEquals(setOf(PERSON_URI, NAME_URI), changes.keys)
    }

    @Test
    fun `a file uri maps to the path of its directory`() {
        assertEquals("/home/me/my specs", "file:///home/me/my%20specs".fileUriToPath())
        assertEquals("c:/specs", "file:///c%3A/specs".fileUriToPath())
        assertEquals("caf\u00e9 1.ws", "caf\u00e9 1.ws".encodeUriComponent().also { assertEquals("caf%C3%A9%201.ws", it) }.decodeUriComponent())
    }

    private fun diagnosticsOf(uri: String): JsonArray = transport
        .notificationsOf("textDocument/publishDiagnostics")
        .last { it["params"]!!.jsonObject["uri"]!!.jsonPrimitive.content == uri }["params"]!!
        .jsonObject["diagnostics"]!!.jsonArray

    private fun initialize() {
        transport.request("initialize", buildJsonObject { put("processId", JsonNull) })
        transport.notify("initialized", buildJsonObject {})
        transport.reset()
    }

    private fun openDocument(uri: String, text: String) {
        transport.notify(
            "textDocument/didOpen",
            buildJsonObject {
                put(
                    "textDocument",
                    buildJsonObject {
                        put("uri", uri)
                        put("languageId", "wirespec")
                        put("version", 1)
                        put("text", text)
                    },
                )
            },
        )
    }

    private fun positionParams(line: Int, character: Int): JsonObject = buildJsonObject {
        put("textDocument", buildJsonObject { put("uri", URI) })
        put(
            "position",
            buildJsonObject {
                put("line", line)
                put("character", character)
            },
        )
    }

    private fun assertNullResult(response: JsonObject) {
        assertEquals(JsonNull, response["result"], "expected null result, got ${response["result"]}")
    }

    companion object {
        private const val URI = "file:///example.ws"
        private const val DIRECTORY = "file:///specs"
        private const val PERSON_URI = "$DIRECTORY/person.ws"
        private const val NAME_URI = "$DIRECTORY/name.ws"
    }
}
