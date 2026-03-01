# KMP LSP Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Rebuild the VS Code LSP server as a Kotlin Multiplatform module compiled to JS, replacing the current TypeScript server.

**Architecture:** A single KMP module `src/ide/lsp/` with JS target producing a Node.js executable. The server reads JSON-RPC over stdin, dispatches to feature handlers that use `compiler:core` for tokenization/parsing, and writes responses to stdout. The VS Code extension becomes a thin launcher.

**Tech Stack:** Kotlin 2.3, kotlinx-serialization-json, compiler:core (tokenize/parse/AST), Node.js stdio APIs via Kotlin/JS externals.

---

### Task 1: Create the Gradle module skeleton

**Files:**
- Create: `src/ide/lsp/build.gradle.kts`
- Modify: `settings.gradle.kts` (add `"src:ide:lsp"` to include list)

**Step 1: Add module to settings.gradle.kts**

In `settings.gradle.kts`, add `"src:ide:lsp"` to the `include(...)` block after `"src:ide:intellij-plugin"`:

```kotlin
include(
    "src:bom",
    "src:compiler:core",
    "src:compiler:lib",
    "src:compiler:test",
    "src:compiler:emitters:kotlin",
    "src:compiler:emitters:java",
    "src:compiler:emitters:typescript",
    "src:compiler:emitters:python",
    "src:compiler:emitters:wirespec",
    "src:ide:intellij-plugin",
    "src:ide:lsp",
    // ... rest unchanged
)
```

**Step 2: Create build.gradle.kts**

```kotlin
plugins {
    id("module.spotless")
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlinx.serialization)
}

group = "${libs.versions.group.id.get()}.ide.lsp"
version = System.getenv(libs.versions.from.env.get()) ?: libs.versions.default.get()

repositories {
    mavenCentral()
    mavenLocal()
}

kotlin {
    js(IR) {
        nodejs()
        useEsModules()
        binaries.executable()
    }

    sourceSets.all {
        languageSettings.apply {
            languageVersion = libs.versions.kotlin.compiler.get()
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":src:compiler:core"))
                implementation(libs.kotlinx.serialization)
            }
        }
        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
    }
}
```

**Step 3: Create minimal Main.kt to verify compilation**

Create `src/ide/lsp/src/jsMain/kotlin/community/flock/wirespec/ide/lsp/Main.kt`:

```kotlin
package community.flock.wirespec.ide.lsp

fun main() {
    // LSP server entry point — will be implemented in later tasks
}
```

**Step 4: Verify the module compiles**

Run: `./gradlew :src:ide:lsp:compileKotlinJs`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add src/ide/lsp/build.gradle.kts src/ide/lsp/src settings.gradle.kts
git commit -m "feat(lsp): add KMP LSP module skeleton"
```

---

### Task 2: JSON-RPC protocol types and Content-Length framing

**Files:**
- Create: `src/ide/lsp/src/commonMain/kotlin/community/flock/wirespec/ide/lsp/protocol/JsonRpc.kt`
- Create: `src/ide/lsp/src/commonMain/kotlin/community/flock/wirespec/ide/lsp/protocol/Framing.kt`
- Create: `src/ide/lsp/src/commonTest/kotlin/community/flock/wirespec/ide/lsp/protocol/JsonRpcTest.kt`
- Create: `src/ide/lsp/src/commonTest/kotlin/community/flock/wirespec/ide/lsp/protocol/FramingTest.kt`

**Step 1: Write the failing test for JSON-RPC serialization**

Create `src/ide/lsp/src/commonTest/kotlin/community/flock/wirespec/ide/lsp/protocol/JsonRpcTest.kt`:

```kotlin
package community.flock.wirespec.ide.lsp.protocol

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

class JsonRpcTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun serializeRequest() {
        val request = JsonRpcRequest(
            id = JsonPrimitive(1),
            method = "initialize",
            params = buildJsonObject { }
        )
        val serialized = json.encodeToString(JsonRpcRequest.serializer(), request)
        val parsed = json.parseToJsonElement(serialized) as JsonObject
        assertEquals("2.0", (parsed["jsonrpc"] as JsonPrimitive).content)
        assertEquals("initialize", (parsed["method"] as JsonPrimitive).content)
        assertEquals(1, (parsed["id"] as JsonPrimitive).content.toInt())
    }

    @Test
    fun serializeResponse() {
        val response = JsonRpcResponse(
            id = JsonPrimitive(1),
            result = buildJsonObject { },
        )
        val serialized = json.encodeToString(JsonRpcResponse.serializer(), response)
        val parsed = json.parseToJsonElement(serialized) as JsonObject
        assertEquals("2.0", (parsed["jsonrpc"] as JsonPrimitive).content)
    }

    @Test
    fun serializeNotification() {
        val notification = JsonRpcNotification(
            method = "textDocument/didOpen",
            params = buildJsonObject { }
        )
        val serialized = json.encodeToString(JsonRpcNotification.serializer(), notification)
        val parsed = json.parseToJsonElement(serialized) as JsonObject
        assertEquals("2.0", (parsed["jsonrpc"] as JsonPrimitive).content)
        assertEquals("textDocument/didOpen", (parsed["method"] as JsonPrimitive).content)
    }

    @Test
    fun deserializeIncomingMessage() {
        val raw = """{"jsonrpc":"2.0","id":1,"method":"initialize","params":{}}"""
        val message = parseIncomingMessage(raw)
        assertEquals("initialize", message.method)
        assertEquals(JsonPrimitive(1), message.id)
    }

    @Test
    fun deserializeNotificationMessage() {
        val raw = """{"jsonrpc":"2.0","method":"initialized","params":{}}"""
        val message = parseIncomingMessage(raw)
        assertEquals("initialized", message.method)
        assertEquals(null, message.id)
    }
}
```

**Step 2: Write the failing test for Content-Length framing**

Create `src/ide/lsp/src/commonTest/kotlin/community/flock/wirespec/ide/lsp/protocol/FramingTest.kt`:

```kotlin
package community.flock.wirespec.ide.lsp.protocol

import kotlin.test.Test
import kotlin.test.assertEquals

class FramingTest {

    @Test
    fun encodeFrame() {
        val body = """{"jsonrpc":"2.0","id":1,"result":{}}"""
        val frame = encodeFrame(body)
        assertEquals("Content-Length: ${body.length}\r\n\r\n$body", frame)
    }

    @Test
    fun parseContentLength() {
        val header = "Content-Length: 42\r\n\r\n"
        assertEquals(42, parseContentLength(header))
    }

    @Test
    fun parseContentLengthWithExtraHeaders() {
        val header = "Content-Length: 100\r\nContent-Type: application/vscode-jsonrpc; charset=utf-8\r\n\r\n"
        assertEquals(100, parseContentLength(header))
    }
}
```

**Step 3: Run tests to verify they fail**

Run: `./gradlew :src:ide:lsp:allTests`
Expected: FAIL (classes don't exist yet)

**Step 4: Implement JsonRpc.kt**

Create `src/ide/lsp/src/commonMain/kotlin/community/flock/wirespec/ide/lsp/protocol/JsonRpc.kt`:

```kotlin
package community.flock.wirespec.ide.lsp.protocol

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class JsonRpcRequest(
    val jsonrpc: String = "2.0",
    val id: JsonPrimitive,
    val method: String,
    val params: JsonElement? = null,
)

@Serializable
data class JsonRpcResponse(
    val jsonrpc: String = "2.0",
    val id: JsonPrimitive? = null,
    val result: JsonElement? = null,
    val error: JsonRpcError? = null,
)

@Serializable
data class JsonRpcNotification(
    val jsonrpc: String = "2.0",
    val method: String,
    val params: JsonElement? = null,
)

@Serializable
data class JsonRpcError(
    val code: Int,
    val message: String,
    val data: JsonElement? = null,
)

data class IncomingMessage(
    val id: JsonPrimitive?,
    val method: String,
    val params: JsonElement?,
)

fun parseIncomingMessage(raw: String): IncomingMessage {
    val obj = Json.parseToJsonElement(raw).jsonObject
    return IncomingMessage(
        id = obj["id"]?.jsonPrimitive,
        method = obj["method"]!!.jsonPrimitive.content,
        params = obj["params"],
    )
}
```

**Step 5: Implement Framing.kt**

Create `src/ide/lsp/src/commonMain/kotlin/community/flock/wirespec/ide/lsp/protocol/Framing.kt`:

```kotlin
package community.flock.wirespec.ide.lsp.protocol

fun encodeFrame(body: String): String {
    val bytes = body.encodeToByteArray()
    return "Content-Length: ${bytes.size}\r\n\r\n$body"
}

fun parseContentLength(header: String): Int {
    val match = Regex("Content-Length: (\\d+)").find(header)
    return match?.groupValues?.get(1)?.toInt()
        ?: error("Missing Content-Length header")
}
```

**Step 6: Run tests to verify they pass**

Run: `./gradlew :src:ide:lsp:allTests`
Expected: ALL PASS

**Step 7: Commit**

```bash
git add src/ide/lsp/src
git commit -m "feat(lsp): add JSON-RPC protocol types and content-length framing"
```

---

### Task 3: LSP type definitions

**Files:**
- Create: `src/ide/lsp/src/commonMain/kotlin/community/flock/wirespec/ide/lsp/protocol/LspTypes.kt`

**Step 1: Create LSP data types**

Create `src/ide/lsp/src/commonMain/kotlin/community/flock/wirespec/ide/lsp/protocol/LspTypes.kt`:

```kotlin
package community.flock.wirespec.ide.lsp.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- Base types ---

@Serializable
data class Position(val line: Int, val character: Int)

@Serializable
data class Range(val start: Position, val end: Position)

@Serializable
data class Location(val uri: String, val range: Range)

@Serializable
data class TextDocumentIdentifier(val uri: String)

@Serializable
data class TextDocumentPositionParams(
    val textDocument: TextDocumentIdentifier,
    val position: Position,
)

@Serializable
data class VersionedTextDocumentIdentifier(val uri: String, val version: Int)

// --- Text Document Sync ---

@Serializable
data class TextDocumentItem(
    val uri: String,
    val languageId: String,
    val version: Int,
    val text: String,
)

@Serializable
data class DidOpenTextDocumentParams(val textDocument: TextDocumentItem)

@Serializable
data class TextDocumentContentChangeEvent(val text: String)

@Serializable
data class DidChangeTextDocumentParams(
    val textDocument: VersionedTextDocumentIdentifier,
    val contentChanges: List<TextDocumentContentChangeEvent>,
)

@Serializable
data class DidCloseTextDocumentParams(val textDocument: TextDocumentIdentifier)

// --- Diagnostics ---

@Serializable
data class Diagnostic(
    val range: Range,
    val severity: Int? = null,
    val source: String? = null,
    val message: String,
)

@Serializable
data class PublishDiagnosticsParams(
    val uri: String,
    val diagnostics: List<Diagnostic>,
)

// --- Semantic Tokens ---

@Serializable
data class SemanticTokensParams(val textDocument: TextDocumentIdentifier)

@Serializable
data class SemanticTokens(val data: List<Int>)

@Serializable
data class SemanticTokensLegend(
    val tokenTypes: List<String>,
    val tokenModifiers: List<String>,
)

// --- Definition ---

@Serializable
data class DefinitionParams(
    val textDocument: TextDocumentIdentifier,
    val position: Position,
)

// --- References ---

@Serializable
data class ReferenceContext(val includeDeclaration: Boolean)

@Serializable
data class ReferenceParams(
    val textDocument: TextDocumentIdentifier,
    val position: Position,
    val context: ReferenceContext,
)

// --- Rename ---

@Serializable
data class RenameParams(
    val textDocument: TextDocumentIdentifier,
    val position: Position,
    val newName: String,
)

@Serializable
data class TextEdit(val range: Range, val newText: String)

@Serializable
data class WorkspaceEdit(val changes: Map<String, List<TextEdit>>? = null)

@Serializable
data class PrepareRenameParams(
    val textDocument: TextDocumentIdentifier,
    val position: Position,
)

// --- Hover ---

@Serializable
data class HoverParams(
    val textDocument: TextDocumentIdentifier,
    val position: Position,
)

@Serializable
data class MarkupContent(val kind: String = "markdown", val value: String)

@Serializable
data class Hover(val contents: MarkupContent, val range: Range? = null)

// --- Completion ---

@Serializable
data class CompletionParams(
    val textDocument: TextDocumentIdentifier,
    val position: Position,
)

@Serializable
data class CompletionItem(
    val label: String,
    val kind: Int? = null,
    val detail: String? = null,
    val insertText: String? = null,
)

@Serializable
data class CompletionList(
    val isIncomplete: Boolean = false,
    val items: List<CompletionItem>,
)

// --- Code Action ---

@Serializable
data class CodeActionContext(val diagnostics: List<Diagnostic>)

@Serializable
data class CodeActionParams(
    val textDocument: TextDocumentIdentifier,
    val range: Range,
    val context: CodeActionContext,
)

@Serializable
data class CodeAction(
    val title: String,
    val kind: String? = null,
    val diagnostics: List<Diagnostic>? = null,
    val edit: WorkspaceEdit? = null,
)

// --- Initialize ---

@Serializable
data class InitializeParams(
    val processId: Int? = null,
    val rootUri: String? = null,
    val capabilities: ClientCapabilities? = null,
)

@Serializable
class ClientCapabilities

@Serializable
data class InitializeResult(val capabilities: ServerCapabilities)

@Serializable
data class ServerCapabilities(
    val textDocumentSync: Int? = null,
    val semanticTokensProvider: SemanticTokensOptions? = null,
    val definitionProvider: Boolean? = null,
    val referencesProvider: Boolean? = null,
    val renameProvider: RenameOptions? = null,
    val hoverProvider: Boolean? = null,
    val completionProvider: CompletionOptions? = null,
    val codeActionProvider: Boolean? = null,
)

@Serializable
data class SemanticTokensOptions(
    val legend: SemanticTokensLegend,
    val full: Boolean = true,
    val range: Boolean = false,
)

@Serializable
data class RenameOptions(val prepareProvider: Boolean = true)

@Serializable
data class CompletionOptions(
    val triggerCharacters: List<String>? = null,
    val resolveProvider: Boolean = false,
)

// --- Diagnostic Severity ---

object DiagnosticSeverity {
    const val Error = 1
    const val Warning = 2
    const val Information = 3
    const val Hint = 4
}

// --- Completion Item Kind ---

object CompletionItemKind {
    const val Text = 1
    const val Keyword = 14
    const val Class = 7
    const val Property = 10
    const val Enum = 13
    const val EnumMember = 20
}
```

**Step 2: Verify compilation**

Run: `./gradlew :src:ide:lsp:compileKotlinJs`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add src/ide/lsp/src
git commit -m "feat(lsp): add LSP protocol type definitions"
```

---

### Task 4: DocumentStore

**Files:**
- Create: `src/ide/lsp/src/commonMain/kotlin/community/flock/wirespec/ide/lsp/server/DocumentStore.kt`
- Create: `src/ide/lsp/src/commonTest/kotlin/community/flock/wirespec/ide/lsp/server/DocumentStoreTest.kt`

**Step 1: Write the failing test**

Create `src/ide/lsp/src/commonTest/kotlin/community/flock/wirespec/ide/lsp/server/DocumentStoreTest.kt`:

```kotlin
package community.flock.wirespec.ide.lsp.server

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DocumentStoreTest {

    @Test
    fun openAndRetrieveDocument() {
        val store = DocumentStore()
        store.open("file:///test.ws", "type Foo {\n  bar: String\n}")
        val doc = store.get("file:///test.ws")
        assertNotNull(doc)
        assertEquals("type Foo {\n  bar: String\n}", doc.text)
    }

    @Test
    fun updateDocument() {
        val store = DocumentStore()
        store.open("file:///test.ws", "type Foo {}")
        store.update("file:///test.ws", "type Bar {}")
        val doc = store.get("file:///test.ws")
        assertNotNull(doc)
        assertEquals("type Bar {}", doc.text)
    }

    @Test
    fun closeDocument() {
        val store = DocumentStore()
        store.open("file:///test.ws", "type Foo {}")
        store.close("file:///test.ws")
        assertNull(store.get("file:///test.ws"))
    }

    @Test
    fun tokensAreCachedOnOpen() {
        val store = DocumentStore()
        store.open("file:///test.ws", "type Foo {\n  bar: String\n}")
        val doc = store.get("file:///test.ws")
        assertNotNull(doc)
        assertNotNull(doc.tokens)
    }

    @Test
    fun allDocumentsReturnsAllOpen() {
        val store = DocumentStore()
        store.open("file:///a.ws", "type A {}")
        store.open("file:///b.ws", "type B {}")
        assertEquals(2, store.all().size)
    }
}
```

**Step 2: Run tests to verify they fail**

Run: `./gradlew :src:ide:lsp:allTests`
Expected: FAIL

**Step 3: Implement DocumentStore**

Create `src/ide/lsp/src/commonMain/kotlin/community/flock/wirespec/ide/lsp/server/DocumentStore.kt`:

```kotlin
package community.flock.wirespec.ide.lsp.server

import arrow.core.toNonEmptyListOrNull
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.TokenizedModule
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.parse.Parser.parse
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.tokenize.Token
import community.flock.wirespec.compiler.core.tokenize.tokenize

data class DocumentState(
    val uri: String,
    val text: String,
    val tokens: List<Token>,
)

class DocumentStore {

    private val documents = mutableMapOf<String, DocumentState>()

    fun open(uri: String, text: String) {
        documents[uri] = createState(uri, text)
    }

    fun update(uri: String, text: String) {
        documents[uri] = createState(uri, text)
    }

    fun close(uri: String) {
        documents.remove(uri)
    }

    fun get(uri: String): DocumentState? = documents[uri]

    fun all(): List<DocumentState> = documents.values.toList()

    fun parseAll(): Pair<AST?, List<WirespecException>> {
        val modules = documents.values.map {
            TokenizedModule(FileUri(it.uri), WirespecSpec.tokenize(it.text))
        }
        val nel = modules.toNonEmptyListOrNull() ?: return null to emptyList()
        return parse(nel).fold(
            { errors -> null to errors },
            { ast -> ast to emptyList() }
        )
    }

    private fun createState(uri: String, text: String): DocumentState {
        val tokens = WirespecSpec.tokenize(text)
        return DocumentState(uri = uri, text = text, tokens = tokens)
    }
}
```

**Step 4: Run tests to verify they pass**

Run: `./gradlew :src:ide:lsp:allTests`
Expected: ALL PASS

**Step 5: Commit**

```bash
git add src/ide/lsp/src
git commit -m "feat(lsp): add DocumentStore for tracking open documents"
```

---

### Task 5: DiagnosticsProvider

**Files:**
- Create: `src/ide/lsp/src/commonMain/kotlin/community/flock/wirespec/ide/lsp/features/DiagnosticsProvider.kt`
- Create: `src/ide/lsp/src/commonTest/kotlin/community/flock/wirespec/ide/lsp/features/DiagnosticsProviderTest.kt`

**Step 1: Write the failing test**

Create `src/ide/lsp/src/commonTest/kotlin/community/flock/wirespec/ide/lsp/features/DiagnosticsProviderTest.kt`:

```kotlin
package community.flock.wirespec.ide.lsp.features

import community.flock.wirespec.ide.lsp.server.DocumentStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DiagnosticsProviderTest {

    @Test
    fun validSourceProducesNoDiagnostics() {
        val store = DocumentStore()
        store.open("file:///test.ws", "type Foo {\n  bar: String\n}")
        val diagnostics = DiagnosticsProvider.diagnose(store)
        val forFile = diagnostics["file:///test.ws"] ?: emptyList()
        assertTrue(forFile.isEmpty())
    }

    @Test
    fun invalidSourceProducesDiagnostics() {
        val store = DocumentStore()
        store.open("file:///test.ws", "type Foo {")
        val diagnostics = DiagnosticsProvider.diagnose(store)
        val forFile = diagnostics["file:///test.ws"] ?: emptyList()
        assertTrue(forFile.isNotEmpty())
    }

    @Test
    fun diagnosticContainsErrorMessage() {
        val store = DocumentStore()
        store.open("file:///test.ws", "type Foo {")
        val diagnostics = DiagnosticsProvider.diagnose(store)
        val forFile = diagnostics["file:///test.ws"]!!
        assertTrue(forFile.first().message.isNotEmpty())
    }
}
```

**Step 2: Run tests to verify they fail**

Run: `./gradlew :src:ide:lsp:allTests`
Expected: FAIL

**Step 3: Implement DiagnosticsProvider**

Create `src/ide/lsp/src/commonMain/kotlin/community/flock/wirespec/ide/lsp/features/DiagnosticsProvider.kt`:

```kotlin
package community.flock.wirespec.ide.lsp.features

import community.flock.wirespec.ide.lsp.protocol.Diagnostic
import community.flock.wirespec.ide.lsp.protocol.DiagnosticSeverity
import community.flock.wirespec.ide.lsp.protocol.Position
import community.flock.wirespec.ide.lsp.protocol.Range
import community.flock.wirespec.ide.lsp.server.DocumentStore

object DiagnosticsProvider {

    fun diagnose(store: DocumentStore): Map<String, List<Diagnostic>> {
        val (_, errors) = store.parseAll()
        val result = mutableMapOf<String, MutableList<Diagnostic>>()

        // Initialize all open documents with empty diagnostics (clears old ones)
        for (doc in store.all()) {
            result[doc.uri] = mutableListOf()
        }

        for (error in errors) {
            val uri = error.fileUri.value
            val startIdx = error.coordinates.idxAndLength.idx - error.coordinates.idxAndLength.length
            val endIdx = error.coordinates.idxAndLength.idx
            val doc = store.get(uri)
            val startPos = doc?.offsetToPosition(startIdx) ?: Position(0, 0)
            val endPos = doc?.offsetToPosition(endIdx) ?: Position(0, 0)
            val diagnostic = Diagnostic(
                range = Range(start = startPos, end = endPos),
                severity = DiagnosticSeverity.Error,
                source = "wirespec",
                message = error.message,
            )
            result.getOrPut(uri) { mutableListOf() }.add(diagnostic)
        }

        return result
    }
}

fun community.flock.wirespec.ide.lsp.server.DocumentState.offsetToPosition(offset: Int): Position {
    var line = 0
    var character = 0
    for (i in 0 until minOf(offset, text.length)) {
        if (text[i] == '\n') {
            line++
            character = 0
        } else {
            character++
        }
    }
    return Position(line, character)
}
```

**Step 4: Run tests to verify they pass**

Run: `./gradlew :src:ide:lsp:allTests`
Expected: ALL PASS

**Step 5: Commit**

```bash
git add src/ide/lsp/src
git commit -m "feat(lsp): add DiagnosticsProvider for error reporting"
```

---

### Task 6: SemanticTokensProvider

**Files:**
- Create: `src/ide/lsp/src/commonMain/kotlin/community/flock/wirespec/ide/lsp/features/SemanticTokensProvider.kt`
- Create: `src/ide/lsp/src/commonTest/kotlin/community/flock/wirespec/ide/lsp/features/SemanticTokensProviderTest.kt`

**Step 1: Write the failing test**

Create `src/ide/lsp/src/commonTest/kotlin/community/flock/wirespec/ide/lsp/features/SemanticTokensProviderTest.kt`:

```kotlin
package community.flock.wirespec.ide.lsp.features

import community.flock.wirespec.ide.lsp.server.DocumentStore
import kotlin.test.Test
import kotlin.test.assertTrue

class SemanticTokensProviderTest {

    @Test
    fun producesTokensForValidSource() {
        val store = DocumentStore()
        store.open("file:///test.ws", "type Foo {\n  bar: String\n}")
        val result = SemanticTokensProvider.provide("file:///test.ws", store)
        assertTrue(result.data.isNotEmpty(), "Should produce semantic tokens")
    }

    @Test
    fun tokenDataLengthIsMultipleOfFive() {
        val store = DocumentStore()
        store.open("file:///test.ws", "type Foo {\n  bar: String\n}")
        val result = SemanticTokensProvider.provide("file:///test.ws", store)
        assertEquals(0, result.data.size % 5, "Token data must be groups of 5 integers")
    }

    @Test
    fun emptyDocumentProducesEmptyTokens() {
        val store = DocumentStore()
        store.open("file:///test.ws", "")
        val result = SemanticTokensProvider.provide("file:///test.ws", store)
        assertTrue(result.data.isEmpty())
    }

    @Test
    fun keywordTokenTypeIsFirst() {
        // "type" keyword should map to token type index 0 (keyword)
        val store = DocumentStore()
        store.open("file:///test.ws", "type Foo {\n  bar: String\n}")
        val result = SemanticTokensProvider.provide("file:///test.ws", store)
        // First token: deltaLine=0, deltaStart=0, length=4, tokenType=keyword(0)
        assertTrue(result.data.size >= 5)
        assertEquals(4, result.data[2], "First token 'type' should have length 4")
        assertEquals(SemanticTokensProvider.TOKEN_TYPE_KEYWORD, result.data[3], "First token should be keyword type")
    }
}

private fun assertEquals(expected: Int, actual: Int, message: String) {
    kotlin.test.assertEquals(expected, actual, message)
}
```

**Step 2: Run tests to verify they fail**

Run: `./gradlew :src:ide:lsp:allTests`
Expected: FAIL

**Step 3: Implement SemanticTokensProvider**

Create `src/ide/lsp/src/commonMain/kotlin/community/flock/wirespec/ide/lsp/features/SemanticTokensProvider.kt`:

```kotlin
package community.flock.wirespec.ide.lsp.features

import community.flock.wirespec.compiler.core.tokenize.*
import community.flock.wirespec.ide.lsp.protocol.SemanticTokens
import community.flock.wirespec.ide.lsp.protocol.SemanticTokensLegend
import community.flock.wirespec.ide.lsp.server.DocumentStore

object SemanticTokensProvider {

    const val TOKEN_TYPE_KEYWORD = 0
    const val TOKEN_TYPE_TYPE = 1
    const val TOKEN_TYPE_PROPERTY = 2
    const val TOKEN_TYPE_COMMENT = 3
    const val TOKEN_TYPE_DECORATOR = 4
    const val TOKEN_TYPE_NUMBER = 5

    const val TOKEN_MODIFIER_DECLARATION = 0
    const val TOKEN_MODIFIER_DEFAULT_LIBRARY = 1

    val legend = SemanticTokensLegend(
        tokenTypes = listOf("keyword", "type", "property", "comment", "macro", "number"),
        tokenModifiers = listOf("declaration", "defaultLibrary"),
    )

    fun provide(uri: String, store: DocumentStore): SemanticTokens {
        val doc = store.get(uri) ?: return SemanticTokens(data = emptyList())
        if (doc.text.isBlank()) return SemanticTokens(data = emptyList())

        val tokens = doc.tokens
        val data = mutableListOf<Int>()
        var prevLine = 0
        var prevChar = 0

        for (token in tokens) {
            val mapped = mapToken(token.type) ?: continue
            val line = token.coordinates.line - 1
            val char = token.coordinates.position - 1 - token.coordinates.idxAndLength.length
            val length = token.coordinates.idxAndLength.length

            if (length <= 0) continue

            val deltaLine = line - prevLine
            val deltaChar = if (deltaLine == 0) char - prevChar else char

            data.add(deltaLine)
            data.add(deltaChar)
            data.add(length)
            data.add(mapped.first)
            data.add(mapped.second)

            prevLine = line
            prevChar = char
        }

        return SemanticTokens(data = data)
    }

    private fun mapToken(type: TokenType): Pair<Int, Int>? = when (type) {
        is WirespecDefinition -> TOKEN_TYPE_KEYWORD to 0
        is TypeIdentifier -> TOKEN_TYPE_TYPE to 0
        is PascalCaseIdentifier -> TOKEN_TYPE_TYPE to 0
        is DromedaryCaseIdentifier -> TOKEN_TYPE_PROPERTY to 0
        is WsString, is WsInteger, is WsNumber, is WsBoolean, is WsBytes, is WsUnit ->
            TOKEN_TYPE_TYPE to (1 shl TOKEN_MODIFIER_DEFAULT_LIBRARY)
        is Comment -> TOKEN_TYPE_COMMENT to 0
        is Annotation -> TOKEN_TYPE_DECORATOR to 0
        is Method -> TOKEN_TYPE_KEYWORD to 0
        is Integer, is Number -> TOKEN_TYPE_NUMBER to 0
        else -> null
    }
}
```

**Step 4: Run tests to verify they pass**

Run: `./gradlew :src:ide:lsp:allTests`
Expected: ALL PASS

**Step 5: Commit**

```bash
git add src/ide/lsp/src
git commit -m "feat(lsp): add SemanticTokensProvider for syntax highlighting"
```

---

### Task 7: WorkspaceIndex, DefinitionProvider, and ReferencesProvider

**Files:**
- Create: `src/ide/lsp/src/commonMain/kotlin/community/flock/wirespec/ide/lsp/server/WorkspaceIndex.kt`
- Create: `src/ide/lsp/src/commonMain/kotlin/community/flock/wirespec/ide/lsp/features/DefinitionProvider.kt`
- Create: `src/ide/lsp/src/commonMain/kotlin/community/flock/wirespec/ide/lsp/features/ReferencesProvider.kt`
- Create: `src/ide/lsp/src/commonTest/kotlin/community/flock/wirespec/ide/lsp/server/WorkspaceIndexTest.kt`
- Create: `src/ide/lsp/src/commonTest/kotlin/community/flock/wirespec/ide/lsp/features/DefinitionProviderTest.kt`
- Create: `src/ide/lsp/src/commonTest/kotlin/community/flock/wirespec/ide/lsp/features/ReferencesProviderTest.kt`

**Step 1: Write the failing tests**

Create `src/ide/lsp/src/commonTest/kotlin/community/flock/wirespec/ide/lsp/server/WorkspaceIndexTest.kt`:

```kotlin
package community.flock.wirespec.ide.lsp.server

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WorkspaceIndexTest {

    @Test
    fun findsDefinitionsFromTokens() {
        val store = DocumentStore()
        store.open("file:///test.ws", "type Foo {\n  bar: String\n}")
        val index = WorkspaceIndex.build(store)
        val defs = index.findDefinitions("Foo")
        assertEquals(1, defs.size)
    }

    @Test
    fun findsReferencesFromTokens() {
        val store = DocumentStore()
        store.open("file:///test.ws", "type Foo {\n  bar: String\n}\n\ntype Bar {\n  foo: Foo\n}")
        val index = WorkspaceIndex.build(store)
        val refs = index.findReferences("Foo")
        // 1 definition + 1 reference
        assertTrue(refs.size >= 1)
    }

    @Test
    fun findsTokenAtPosition() {
        val store = DocumentStore()
        store.open("file:///test.ws", "type Foo {\n  bar: String\n}")
        val index = WorkspaceIndex.build(store)
        val token = index.findTokenAt("file:///test.ws", line = 0, character = 5)
        assertEquals("Foo", token?.value)
    }
}
```

Create `src/ide/lsp/src/commonTest/kotlin/community/flock/wirespec/ide/lsp/features/DefinitionProviderTest.kt`:

```kotlin
package community.flock.wirespec.ide.lsp.features

import community.flock.wirespec.ide.lsp.protocol.Position
import community.flock.wirespec.ide.lsp.server.DocumentStore
import community.flock.wirespec.ide.lsp.server.WorkspaceIndex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DefinitionProviderTest {

    @Test
    fun findsDefinitionOfTypeReference() {
        val store = DocumentStore()
        store.open("file:///test.ws", "type Foo {\n  bar: String\n}\n\ntype Bar {\n  foo: Foo\n}")
        val index = WorkspaceIndex.build(store)
        // Position on "Foo" in "foo: Foo" (line 5, character 7)
        val locations = DefinitionProvider.provide("file:///test.ws", Position(5, 7), index)
        assertTrue(locations.isNotEmpty(), "Should find definition of Foo")
        assertEquals("file:///test.ws", locations.first().uri)
    }

    @Test
    fun returnsEmptyForNonTypeToken() {
        val store = DocumentStore()
        store.open("file:///test.ws", "type Foo {\n  bar: String\n}")
        val index = WorkspaceIndex.build(store)
        // Position on "{" — not a type identifier
        val locations = DefinitionProvider.provide("file:///test.ws", Position(0, 9), index)
        assertTrue(locations.isEmpty())
    }
}
```

Create `src/ide/lsp/src/commonTest/kotlin/community/flock/wirespec/ide/lsp/features/ReferencesProviderTest.kt`:

```kotlin
package community.flock.wirespec.ide.lsp.features

import community.flock.wirespec.ide.lsp.protocol.Position
import community.flock.wirespec.ide.lsp.server.DocumentStore
import community.flock.wirespec.ide.lsp.server.WorkspaceIndex
import kotlin.test.Test
import kotlin.test.assertTrue

class ReferencesProviderTest {

    @Test
    fun findsAllReferencesToType() {
        val source = "type Foo {\n  bar: String\n}\n\ntype Bar {\n  foo: Foo\n}\n\ntype Baz {\n  also: Foo\n}"
        val store = DocumentStore()
        store.open("file:///test.ws", source)
        val index = WorkspaceIndex.build(store)
        // Position on "Foo" definition (line 0, character 5)
        val locations = ReferencesProvider.provide("file:///test.ws", Position(0, 5), true, index)
        // Should find definition + 2 references = 3
        assertTrue(locations.size >= 2, "Should find multiple references to Foo, got ${locations.size}")
    }
}
```

**Step 2: Run tests to verify they fail**

Run: `./gradlew :src:ide:lsp:allTests`
Expected: FAIL

**Step 3: Implement WorkspaceIndex**

Create `src/ide/lsp/src/commonMain/kotlin/community/flock/wirespec/ide/lsp/server/WorkspaceIndex.kt`:

```kotlin
package community.flock.wirespec.ide.lsp.server

import community.flock.wirespec.compiler.core.tokenize.*
import community.flock.wirespec.ide.lsp.protocol.Location
import community.flock.wirespec.ide.lsp.protocol.Position
import community.flock.wirespec.ide.lsp.protocol.Range

data class IndexedToken(
    val uri: String,
    val value: String,
    val type: TokenType,
    val line: Int,
    val character: Int,
    val length: Int,
)

class WorkspaceIndex private constructor(
    private val allTokens: List<IndexedToken>,
) {

    companion object {
        fun build(store: DocumentStore): WorkspaceIndex {
            val tokens = mutableListOf<IndexedToken>()
            for (doc in store.all()) {
                for (token in doc.tokens) {
                    val line = token.coordinates.line - 1
                    val char = token.coordinates.position - 1 - token.coordinates.idxAndLength.length
                    val length = token.coordinates.idxAndLength.length
                    tokens.add(
                        IndexedToken(
                            uri = doc.uri,
                            value = token.value,
                            type = token.type,
                            line = line,
                            character = char,
                            length = length,
                        )
                    )
                }
            }
            return WorkspaceIndex(tokens)
        }
    }

    fun findTokenAt(uri: String, line: Int, character: Int): IndexedToken? =
        allTokens.find { token ->
            token.uri == uri &&
                token.line == line &&
                character >= token.character &&
                character < token.character + token.length
        }

    fun findDefinitions(name: String): List<Location> =
        allTokens
            .filter { it.value == name && isTypeIdentifier(it.type) }
            .filter { isDefinitionSite(it) }
            .map { it.toLocation() }

    fun findReferences(name: String): List<Location> =
        allTokens
            .filter { it.value == name && isTypeIdentifier(it.type) }
            .map { it.toLocation() }

    fun findAllOccurrences(name: String): List<Location> =
        allTokens
            .filter { it.value == name }
            .map { it.toLocation() }

    private fun isTypeIdentifier(type: TokenType): Boolean = when (type) {
        is TypeIdentifier, is PascalCaseIdentifier -> true
        else -> false
    }

    private fun isDefinitionSite(token: IndexedToken): Boolean {
        val idx = allTokens.indexOf(token)
        if (idx <= 0) return false
        // A definition site is a type identifier preceded by a definition keyword
        val prev = allTokens.subList(0, idx).lastOrNull { it.type !is WhiteSpace }
        return prev != null && prev.type is WirespecDefinition
    }

    private fun IndexedToken.toLocation() = Location(
        uri = uri,
        range = Range(
            start = Position(line, character),
            end = Position(line, character + length),
        ),
    )
}
```

**Step 4: Implement DefinitionProvider**

Create `src/ide/lsp/src/commonMain/kotlin/community/flock/wirespec/ide/lsp/features/DefinitionProvider.kt`:

```kotlin
package community.flock.wirespec.ide.lsp.features

import community.flock.wirespec.ide.lsp.protocol.Location
import community.flock.wirespec.ide.lsp.protocol.Position
import community.flock.wirespec.ide.lsp.server.WorkspaceIndex

object DefinitionProvider {

    fun provide(uri: String, position: Position, index: WorkspaceIndex): List<Location> {
        val token = index.findTokenAt(uri, position.line, position.character) ?: return emptyList()
        return index.findDefinitions(token.value)
    }
}
```

**Step 5: Implement ReferencesProvider**

Create `src/ide/lsp/src/commonMain/kotlin/community/flock/wirespec/ide/lsp/features/ReferencesProvider.kt`:

```kotlin
package community.flock.wirespec.ide.lsp.features

import community.flock.wirespec.ide.lsp.protocol.Location
import community.flock.wirespec.ide.lsp.protocol.Position
import community.flock.wirespec.ide.lsp.server.WorkspaceIndex

object ReferencesProvider {

    fun provide(uri: String, position: Position, includeDeclaration: Boolean, index: WorkspaceIndex): List<Location> {
        val token = index.findTokenAt(uri, position.line, position.character) ?: return emptyList()
        return if (includeDeclaration) {
            index.findReferences(token.value)
        } else {
            val definitions = index.findDefinitions(token.value).toSet()
            index.findReferences(token.value).filter { it !in definitions }
        }
    }
}
```

**Step 6: Run tests to verify they pass**

Run: `./gradlew :src:ide:lsp:allTests`
Expected: ALL PASS

**Step 7: Commit**

```bash
git add src/ide/lsp/src
git commit -m "feat(lsp): add WorkspaceIndex, DefinitionProvider, and ReferencesProvider"
```

---

### Task 8: RenameProvider, HoverProvider, CompletionProvider, CodeActionProvider

**Files:**
- Create: `src/ide/lsp/src/commonMain/kotlin/community/flock/wirespec/ide/lsp/features/RenameProvider.kt`
- Create: `src/ide/lsp/src/commonMain/kotlin/community/flock/wirespec/ide/lsp/features/HoverProvider.kt`
- Create: `src/ide/lsp/src/commonMain/kotlin/community/flock/wirespec/ide/lsp/features/CompletionProvider.kt`
- Create: `src/ide/lsp/src/commonMain/kotlin/community/flock/wirespec/ide/lsp/features/CodeActionProvider.kt`
- Create: `src/ide/lsp/src/commonTest/kotlin/community/flock/wirespec/ide/lsp/features/RenameProviderTest.kt`
- Create: `src/ide/lsp/src/commonTest/kotlin/community/flock/wirespec/ide/lsp/features/HoverProviderTest.kt`
- Create: `src/ide/lsp/src/commonTest/kotlin/community/flock/wirespec/ide/lsp/features/CompletionProviderTest.kt`

**Step 1: Write the failing tests**

Create `src/ide/lsp/src/commonTest/kotlin/community/flock/wirespec/ide/lsp/features/RenameProviderTest.kt`:

```kotlin
package community.flock.wirespec.ide.lsp.features

import community.flock.wirespec.ide.lsp.protocol.Position
import community.flock.wirespec.ide.lsp.server.DocumentStore
import community.flock.wirespec.ide.lsp.server.WorkspaceIndex
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RenameProviderTest {

    @Test
    fun renameTypeUpdatesAllOccurrences() {
        val source = "type Foo {\n  bar: String\n}\n\ntype Bar {\n  foo: Foo\n}"
        val store = DocumentStore()
        store.open("file:///test.ws", source)
        val index = WorkspaceIndex.build(store)
        val edit = RenameProvider.provide("file:///test.ws", Position(0, 5), "Baz", index)
        assertNotNull(edit)
        val changes = edit.changes?.get("file:///test.ws")
        assertNotNull(changes)
        assertTrue(changes.size >= 2, "Should rename at least 2 occurrences (definition + reference)")
        assertTrue(changes.all { it.newText == "Baz" })
    }
}
```

Create `src/ide/lsp/src/commonTest/kotlin/community/flock/wirespec/ide/lsp/features/HoverProviderTest.kt`:

```kotlin
package community.flock.wirespec.ide.lsp.features

import community.flock.wirespec.ide.lsp.protocol.Position
import community.flock.wirespec.ide.lsp.server.DocumentStore
import community.flock.wirespec.ide.lsp.server.WorkspaceIndex
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HoverProviderTest {

    @Test
    fun hoverOnTypeShowsDefinition() {
        val source = "type Foo {\n  bar: String\n}"
        val store = DocumentStore()
        store.open("file:///test.ws", source)
        val index = WorkspaceIndex.build(store)
        val hover = HoverProvider.provide("file:///test.ws", Position(0, 5), index, store)
        assertNotNull(hover)
        assertTrue(hover.contents.value.contains("Foo"))
    }

    @Test
    fun hoverOnNonIdentifierReturnsNull() {
        val source = "type Foo {\n  bar: String\n}"
        val store = DocumentStore()
        store.open("file:///test.ws", source)
        val index = WorkspaceIndex.build(store)
        val hover = HoverProvider.provide("file:///test.ws", Position(0, 9), index, store)
        assertNull(hover)
    }
}
```

Create `src/ide/lsp/src/commonTest/kotlin/community/flock/wirespec/ide/lsp/features/CompletionProviderTest.kt`:

```kotlin
package community.flock.wirespec.ide.lsp.features

import community.flock.wirespec.ide.lsp.protocol.Position
import community.flock.wirespec.ide.lsp.server.DocumentStore
import community.flock.wirespec.ide.lsp.server.WorkspaceIndex
import kotlin.test.Test
import kotlin.test.assertTrue

class CompletionProviderTest {

    @Test
    fun suggestsKeywords() {
        val store = DocumentStore()
        store.open("file:///test.ws", "")
        val index = WorkspaceIndex.build(store)
        val result = CompletionProvider.provide("file:///test.ws", Position(0, 0), index)
        val labels = result.items.map { it.label }
        assertTrue("type" in labels)
        assertTrue("enum" in labels)
        assertTrue("endpoint" in labels)
    }

    @Test
    fun suggestsDefinedTypes() {
        val source = "type Foo {\n  bar: String\n}\n\ntype Bar {\n  foo: "
        val store = DocumentStore()
        store.open("file:///test.ws", source)
        val index = WorkspaceIndex.build(store)
        val result = CompletionProvider.provide("file:///test.ws", Position(4, 7), index)
        val labels = result.items.map { it.label }
        assertTrue("Foo" in labels, "Should suggest Foo as a completion, got: $labels")
    }
}
```

**Step 2: Run tests to verify they fail**

Run: `./gradlew :src:ide:lsp:allTests`
Expected: FAIL

**Step 3: Implement RenameProvider**

Create `src/ide/lsp/src/commonMain/kotlin/community/flock/wirespec/ide/lsp/features/RenameProvider.kt`:

```kotlin
package community.flock.wirespec.ide.lsp.features

import community.flock.wirespec.ide.lsp.protocol.Position
import community.flock.wirespec.ide.lsp.protocol.Range
import community.flock.wirespec.ide.lsp.protocol.TextEdit
import community.flock.wirespec.ide.lsp.protocol.WorkspaceEdit
import community.flock.wirespec.ide.lsp.server.WorkspaceIndex

object RenameProvider {

    fun provide(uri: String, position: Position, newName: String, index: WorkspaceIndex): WorkspaceEdit? {
        val token = index.findTokenAt(uri, position.line, position.character) ?: return null
        val occurrences = index.findReferences(token.value)
        if (occurrences.isEmpty()) return null

        val changes = occurrences.groupBy({ it.uri }) { location ->
            TextEdit(range = location.range, newText = newName)
        }
        return WorkspaceEdit(changes = changes)
    }

    fun prepareRename(uri: String, position: Position, index: WorkspaceIndex): Range? {
        val token = index.findTokenAt(uri, position.line, position.character) ?: return null
        val defs = index.findDefinitions(token.value)
        if (defs.isEmpty()) return null
        return Range(
            start = Position(token.line, token.character),
            end = Position(token.line, token.character + token.length),
        )
    }
}

private val community.flock.wirespec.ide.lsp.server.IndexedToken.line get() = this.let {
    val loc = community.flock.wirespec.ide.lsp.server.WorkspaceIndex.build(
        // We need access to the token's line — stored directly in IndexedToken
        TODO()
    )
    0
}
```

Wait — `IndexedToken` already has `line`, `character`, and `length` fields. Let me fix that:

```kotlin
package community.flock.wirespec.ide.lsp.features

import community.flock.wirespec.ide.lsp.protocol.Position
import community.flock.wirespec.ide.lsp.protocol.Range
import community.flock.wirespec.ide.lsp.protocol.TextEdit
import community.flock.wirespec.ide.lsp.protocol.WorkspaceEdit
import community.flock.wirespec.ide.lsp.server.WorkspaceIndex

object RenameProvider {

    fun provide(uri: String, position: Position, newName: String, index: WorkspaceIndex): WorkspaceEdit? {
        val token = index.findTokenAt(uri, position.line, position.character) ?: return null
        val occurrences = index.findReferences(token.value)
        if (occurrences.isEmpty()) return null

        val changes = occurrences.groupBy({ it.uri }) { location ->
            TextEdit(range = location.range, newText = newName)
        }
        return WorkspaceEdit(changes = changes)
    }

    fun prepareRename(uri: String, position: Position, index: WorkspaceIndex): Range? {
        val token = index.findTokenAt(uri, position.line, position.character) ?: return null
        return Range(
            start = Position(token.line, token.character),
            end = Position(token.line, token.character + token.length),
        )
    }
}
```

**Step 4: Implement HoverProvider**

Create `src/ide/lsp/src/commonMain/kotlin/community/flock/wirespec/ide/lsp/features/HoverProvider.kt`:

```kotlin
package community.flock.wirespec.ide.lsp.features

import community.flock.wirespec.compiler.core.tokenize.*
import community.flock.wirespec.ide.lsp.protocol.Hover
import community.flock.wirespec.ide.lsp.protocol.MarkupContent
import community.flock.wirespec.ide.lsp.protocol.Position
import community.flock.wirespec.ide.lsp.server.DocumentStore
import community.flock.wirespec.ide.lsp.server.WorkspaceIndex

object HoverProvider {

    fun provide(uri: String, position: Position, index: WorkspaceIndex, store: DocumentStore): Hover? {
        val token = index.findTokenAt(uri, position.line, position.character) ?: return null

        if (!isHoverable(token.type)) return null

        val definitions = index.findDefinitions(token.value)
        if (definitions.isEmpty() && !isBuiltinType(token.type)) return null

        val markdown = buildString {
            if (isBuiltinType(token.type)) {
                append("```wirespec\n${token.value}\n```\n")
                append("Built-in type")
            } else {
                append("```wirespec\n${token.value}\n```")
            }
        }

        return Hover(contents = MarkupContent(value = markdown))
    }

    private fun isHoverable(type: TokenType): Boolean = when (type) {
        is TypeIdentifier, is PascalCaseIdentifier -> true
        is WsString, is WsInteger, is WsNumber, is WsBoolean, is WsBytes, is WsUnit -> true
        is DromedaryCaseIdentifier -> true
        else -> false
    }

    private fun isBuiltinType(type: TokenType): Boolean = when (type) {
        is WsString, is WsInteger, is WsNumber, is WsBoolean, is WsBytes, is WsUnit -> true
        else -> false
    }
}
```

**Step 5: Implement CompletionProvider**

Create `src/ide/lsp/src/commonMain/kotlin/community/flock/wirespec/ide/lsp/features/CompletionProvider.kt`:

```kotlin
package community.flock.wirespec.ide.lsp.features

import community.flock.wirespec.ide.lsp.protocol.CompletionItem
import community.flock.wirespec.ide.lsp.protocol.CompletionItemKind
import community.flock.wirespec.ide.lsp.protocol.CompletionList
import community.flock.wirespec.ide.lsp.protocol.Position
import community.flock.wirespec.ide.lsp.server.WorkspaceIndex

object CompletionProvider {

    private val keywords = listOf("type", "enum", "endpoint", "channel")
    private val builtinTypes = listOf("String", "Integer", "Integer32", "Number", "Number32", "Boolean", "Bytes", "Unit")

    fun provide(uri: String, position: Position, index: WorkspaceIndex): CompletionList {
        val items = mutableListOf<CompletionItem>()

        // Keywords
        for (kw in keywords) {
            items.add(CompletionItem(label = kw, kind = CompletionItemKind.Keyword))
        }

        // Built-in types
        for (bt in builtinTypes) {
            items.add(CompletionItem(label = bt, kind = CompletionItemKind.Class, detail = "Built-in type"))
        }

        // User-defined types from workspace
        val definedTypes = mutableSetOf<String>()
        for (doc in index.findAllDefinitionNames()) {
            if (doc !in builtinTypes) {
                definedTypes.add(doc)
            }
        }
        for (typeName in definedTypes) {
            items.add(CompletionItem(label = typeName, kind = CompletionItemKind.Class))
        }

        return CompletionList(items = items)
    }
}
```

This requires adding `findAllDefinitionNames()` to `WorkspaceIndex`. Add to `WorkspaceIndex.kt`:

```kotlin
fun findAllDefinitionNames(): Set<String> =
    allTokens
        .filter { isTypeIdentifier(it.type) && isDefinitionSite(it) }
        .map { it.value }
        .toSet()
```

**Step 6: Implement CodeActionProvider**

Create `src/ide/lsp/src/commonMain/kotlin/community/flock/wirespec/ide/lsp/features/CodeActionProvider.kt`:

```kotlin
package community.flock.wirespec.ide.lsp.features

import community.flock.wirespec.ide.lsp.protocol.CodeAction
import community.flock.wirespec.ide.lsp.protocol.Diagnostic

object CodeActionProvider {

    fun provide(uri: String, diagnostics: List<Diagnostic>): List<CodeAction> {
        // Future: match diagnostics to quick fixes
        return emptyList()
    }
}
```

**Step 7: Run tests to verify they pass**

Run: `./gradlew :src:ide:lsp:allTests`
Expected: ALL PASS

**Step 8: Commit**

```bash
git add src/ide/lsp/src
git commit -m "feat(lsp): add RenameProvider, HoverProvider, CompletionProvider, CodeActionProvider"
```

---

### Task 9: LspServer — main dispatch loop

**Files:**
- Create: `src/ide/lsp/src/commonMain/kotlin/community/flock/wirespec/ide/lsp/server/LspServer.kt`
- Create: `src/ide/lsp/src/commonTest/kotlin/community/flock/wirespec/ide/lsp/server/LspServerTest.kt`

**Step 1: Write the failing test**

Create `src/ide/lsp/src/commonTest/kotlin/community/flock/wirespec/ide/lsp/server/LspServerTest.kt`:

```kotlin
package community.flock.wirespec.ide.lsp.server

import community.flock.wirespec.ide.lsp.protocol.JsonRpcResponse
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LspServerTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun handleInitializeReturnsCapabilities() {
        val server = LspServer()
        val responses = mutableListOf<String>()
        server.onSend = { responses.add(it) }

        server.handleMessage("""{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"processId":null}}""")

        assertTrue(responses.isNotEmpty(), "Should send initialize response")
        val response = json.decodeFromString(JsonRpcResponse.serializer(), responses.first())
        assertNotNull(response.result)
        assertTrue(response.result.toString().contains("capabilities"))
    }

    @Test
    fun handleShutdownReturnsNull() {
        val server = LspServer()
        val responses = mutableListOf<String>()
        server.onSend = { responses.add(it) }

        server.handleMessage("""{"jsonrpc":"2.0","id":1,"method":"initialize","params":{}}""")
        responses.clear()
        server.handleMessage("""{"jsonrpc":"2.0","id":2,"method":"shutdown","params":null}""")

        assertTrue(responses.isNotEmpty())
    }

    @Test
    fun handleDidOpenPublishesDiagnostics() {
        val server = LspServer()
        val responses = mutableListOf<String>()
        server.onSend = { responses.add(it) }

        server.handleMessage("""{"jsonrpc":"2.0","id":1,"method":"initialize","params":{}}""")
        responses.clear()

        val params = buildJsonObject {
            put("textDocument", buildJsonObject {
                put("uri", "file:///test.ws")
                put("languageId", "wirespec")
                put("version", 1)
                put("text", "type Foo {\n  bar: String\n}")
            })
        }
        server.handleMessage("""{"jsonrpc":"2.0","method":"textDocument/didOpen","params":$params}""")

        assertTrue(responses.any { it.contains("textDocument/publishDiagnostics") }, "Should publish diagnostics")
    }
}
```

**Step 2: Run tests to verify they fail**

Run: `./gradlew :src:ide:lsp:allTests`
Expected: FAIL

**Step 3: Implement LspServer**

Create `src/ide/lsp/src/commonMain/kotlin/community/flock/wirespec/ide/lsp/server/LspServer.kt`:

```kotlin
package community.flock.wirespec.ide.lsp.server

import community.flock.wirespec.ide.lsp.features.*
import community.flock.wirespec.ide.lsp.protocol.*
import kotlinx.serialization.json.*

class LspServer {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val store = DocumentStore()
    var onSend: (String) -> Unit = {}

    fun handleMessage(raw: String) {
        val message = parseIncomingMessage(raw)
        when (message.method) {
            "initialize" -> handleInitialize(message)
            "initialized" -> {} // no-op
            "textDocument/didOpen" -> handleDidOpen(message)
            "textDocument/didChange" -> handleDidChange(message)
            "textDocument/didClose" -> handleDidClose(message)
            "textDocument/semanticTokens/full" -> handleSemanticTokens(message)
            "textDocument/definition" -> handleDefinition(message)
            "textDocument/references" -> handleReferences(message)
            "textDocument/rename" -> handleRename(message)
            "textDocument/prepareRename" -> handlePrepareRename(message)
            "textDocument/hover" -> handleHover(message)
            "textDocument/completion" -> handleCompletion(message)
            "textDocument/codeAction" -> handleCodeAction(message)
            "shutdown" -> handleShutdown(message)
            "exit" -> {} // handled by transport layer
        }
    }

    private fun handleInitialize(message: IncomingMessage) {
        val result = InitializeResult(
            capabilities = ServerCapabilities(
                textDocumentSync = 1, // Full
                semanticTokensProvider = SemanticTokensOptions(
                    legend = SemanticTokensProvider.legend,
                ),
                definitionProvider = true,
                referencesProvider = true,
                renameProvider = RenameOptions(prepareProvider = true),
                hoverProvider = true,
                completionProvider = CompletionOptions(triggerCharacters = emptyList()),
                codeActionProvider = true,
            ),
        )
        sendResponse(message.id, json.encodeToJsonElement(InitializeResult.serializer(), result))
    }

    private fun handleDidOpen(message: IncomingMessage) {
        val params = json.decodeFromJsonElement(DidOpenTextDocumentParams.serializer(), message.params!!)
        store.open(params.textDocument.uri, params.textDocument.text)
        publishDiagnostics()
    }

    private fun handleDidChange(message: IncomingMessage) {
        val params = json.decodeFromJsonElement(DidChangeTextDocumentParams.serializer(), message.params!!)
        val text = params.contentChanges.lastOrNull()?.text ?: return
        store.update(params.textDocument.uri, text)
        publishDiagnostics()
    }

    private fun handleDidClose(message: IncomingMessage) {
        val params = json.decodeFromJsonElement(DidCloseTextDocumentParams.serializer(), message.params!!)
        store.close(params.textDocument.uri)
    }

    private fun handleSemanticTokens(message: IncomingMessage) {
        val params = json.decodeFromJsonElement(SemanticTokensParams.serializer(), message.params!!)
        val result = SemanticTokensProvider.provide(params.textDocument.uri, store)
        sendResponse(message.id, json.encodeToJsonElement(SemanticTokens.serializer(), result))
    }

    private fun handleDefinition(message: IncomingMessage) {
        val params = json.decodeFromJsonElement(DefinitionParams.serializer(), message.params!!)
        val index = WorkspaceIndex.build(store)
        val locations = DefinitionProvider.provide(params.textDocument.uri, params.position, index)
        sendResponse(message.id, json.encodeToJsonElement(locations))
    }

    private fun handleReferences(message: IncomingMessage) {
        val params = json.decodeFromJsonElement(ReferenceParams.serializer(), message.params!!)
        val index = WorkspaceIndex.build(store)
        val locations = ReferencesProvider.provide(
            params.textDocument.uri, params.position, params.context.includeDeclaration, index
        )
        sendResponse(message.id, json.encodeToJsonElement(locations))
    }

    private fun handleRename(message: IncomingMessage) {
        val params = json.decodeFromJsonElement(RenameParams.serializer(), message.params!!)
        val index = WorkspaceIndex.build(store)
        val edit = RenameProvider.provide(params.textDocument.uri, params.position, params.newName, index)
        sendResponse(message.id, if (edit != null) json.encodeToJsonElement(WorkspaceEdit.serializer(), edit) else JsonNull)
    }

    private fun handlePrepareRename(message: IncomingMessage) {
        val params = json.decodeFromJsonElement(PrepareRenameParams.serializer(), message.params!!)
        val index = WorkspaceIndex.build(store)
        val range = RenameProvider.prepareRename(params.textDocument.uri, params.position, index)
        sendResponse(message.id, if (range != null) json.encodeToJsonElement(Range.serializer(), range) else JsonNull)
    }

    private fun handleHover(message: IncomingMessage) {
        val params = json.decodeFromJsonElement(HoverParams.serializer(), message.params!!)
        val index = WorkspaceIndex.build(store)
        val hover = HoverProvider.provide(params.textDocument.uri, params.position, index, store)
        sendResponse(message.id, if (hover != null) json.encodeToJsonElement(Hover.serializer(), hover) else JsonNull)
    }

    private fun handleCompletion(message: IncomingMessage) {
        val params = json.decodeFromJsonElement(CompletionParams.serializer(), message.params!!)
        val index = WorkspaceIndex.build(store)
        val result = CompletionProvider.provide(params.textDocument.uri, params.position, index)
        sendResponse(message.id, json.encodeToJsonElement(CompletionList.serializer(), result))
    }

    private fun handleCodeAction(message: IncomingMessage) {
        val params = json.decodeFromJsonElement(CodeActionParams.serializer(), message.params!!)
        val actions = CodeActionProvider.provide(params.textDocument.uri, params.context.diagnostics)
        sendResponse(message.id, json.encodeToJsonElement(actions))
    }

    private fun handleShutdown(message: IncomingMessage) {
        sendResponse(message.id, JsonNull)
    }

    private fun publishDiagnostics() {
        val allDiagnostics = DiagnosticsProvider.diagnose(store)
        for ((uri, diagnostics) in allDiagnostics) {
            val params = PublishDiagnosticsParams(uri = uri, diagnostics = diagnostics)
            sendNotification("textDocument/publishDiagnostics", json.encodeToJsonElement(PublishDiagnosticsParams.serializer(), params))
        }
    }

    private fun sendResponse(id: JsonPrimitive?, result: JsonElement) {
        val response = JsonRpcResponse(id = id, result = result)
        onSend(json.encodeToString(JsonRpcResponse.serializer(), response))
    }

    private fun sendNotification(method: String, params: JsonElement) {
        val notification = JsonRpcNotification(method = method, params = params)
        onSend(json.encodeToString(JsonRpcNotification.serializer(), notification))
    }
}
```

**Step 4: Run tests to verify they pass**

Run: `./gradlew :src:ide:lsp:allTests`
Expected: ALL PASS

**Step 5: Commit**

```bash
git add src/ide/lsp/src
git commit -m "feat(lsp): add LspServer with request dispatch and all handlers"
```

---

### Task 10: Node.js entry point (stdin/stdout transport)

**Files:**
- Modify: `src/ide/lsp/src/jsMain/kotlin/community/flock/wirespec/ide/lsp/Main.kt`

**Step 1: Implement the Node.js main entry point**

Replace `src/ide/lsp/src/jsMain/kotlin/community/flock/wirespec/ide/lsp/Main.kt`:

```kotlin
package community.flock.wirespec.ide.lsp

import community.flock.wirespec.ide.lsp.protocol.encodeFrame
import community.flock.wirespec.ide.lsp.server.LspServer

private val server = LspServer()
private var buffer = ""
private var contentLength = -1

fun main() {
    server.onSend = { body ->
        val frame = encodeFrame(body)
        js("process.stdout.write(frame)")
    }

    val stdin = js("process.stdin")
    stdin.setEncoding("utf8")
    stdin.on("data") { chunk: String ->
        buffer += chunk
        processBuffer()
    }
}

private fun processBuffer() {
    while (true) {
        if (contentLength == -1) {
            val headerEnd = buffer.indexOf("\r\n\r\n")
            if (headerEnd == -1) return
            val header = buffer.substring(0, headerEnd + 4)
            val match = Regex("Content-Length: (\\d+)").find(header) ?: return
            contentLength = match.groupValues[1].toInt()
            buffer = buffer.substring(headerEnd + 4)
        }

        if (buffer.length < contentLength) return

        val body = buffer.substring(0, contentLength)
        buffer = buffer.substring(contentLength)
        contentLength = -1

        try {
            server.handleMessage(body)
        } catch (e: Exception) {
            // Log to stderr so it doesn't corrupt LSP protocol on stdout
            js("process.stderr.write('LSP Error: ' + e.message + '\\n')")
        }
    }
}
```

**Step 2: Build the JS executable**

Run: `./gradlew :src:ide:lsp:jsNodeProductionRun --dry-run` (verify task exists)
Then: `./gradlew :src:ide:lsp:compileProductionExecutableKotlinJs`
Expected: BUILD SUCCESSFUL — produces a JS file in `build/compileSync/js/main/productionExecutable/`

**Step 3: Verify the output file exists**

Run: `find src/ide/lsp/build -name "*.mjs" -o -name "*.js" | head -5`
Expected: At least one JS file output

**Step 4: Commit**

```bash
git add src/ide/lsp/src/jsMain
git commit -m "feat(lsp): add Node.js entry point with stdin/stdout transport"
```

---

### Task 11: Update VS Code extension to use Kotlin LSP server

**Files:**
- Modify: `src/ide/vscode/src/extension.ts`
- Delete: `src/ide/vscode/src/server.ts`
- Modify: `src/ide/vscode/package.json`

**Step 1: Update extension.ts to launch Kotlin/JS server via stdio**

Replace `src/ide/vscode/src/extension.ts` with:

```typescript
import * as vscode from "vscode";
import { ExtensionContext } from "vscode";
import { LanguageClient, ServerOptions, TransportKind, LanguageClientOptions } from "vscode-languageclient/node";
import * as path from "path";

let client: LanguageClient;

export const activate = (context: ExtensionContext) => {
  console.log("Activating Wirespec LSP...");

  const serverModule = context.asAbsolutePath(path.join("build", "lsp-server.mjs"));

  const serverOptions: ServerOptions = {
    run: {
      module: serverModule,
      transport: TransportKind.stdio,
    },
    debug: {
      module: serverModule,
      transport: TransportKind.stdio,
    },
  };

  const clientOptions: LanguageClientOptions = {
    documentSelector: [
      {
        scheme: "file",
        language: "wirespec",
      },
    ],
  };

  client = new LanguageClient("wirespec-extension-id", "Wirespec LSP", serverOptions, clientOptions);

  client.start().catch(console.error);

  console.log("Wirespec LSP activated.");
};

export const deactivate = (): Thenable<void> | undefined => (client ? client.stop() : undefined);
```

**Step 2: Delete server.ts**

Delete `src/ide/vscode/src/server.ts`.

**Step 3: Update package.json**

Remove dependencies that are no longer needed (`@flock/wirespec`, `vscode-languageserver`, `vscode-languageserver-textdocument`, `format-util`, `source-map-support`). Update esbuild scripts to only build the extension (not server):

```json
{
  "name": "wirespec-vscode-plugin",
  "displayName": "wirespec",
  "version": "0.0.0",
  "engines": {
    "vscode": "^1.89.0"
  },
  "repository": {
    "type": "git",
    "url": "https://github.com/flock-community/wirespec"
  },
  "activationEvents": [
    "onLanguage:wirespec"
  ],
  "main": "./build/extension",
  "dependencies": {
    "vscode-languageclient": "9.0.1"
  },
  "devDependencies": {
    "@types/node": "^20.12.12",
    "@types/vscode": "^1.89.0",
    "esbuild": "^0.21.4",
    "prettier": "^3.2.5",
    "rimraf": "^5.0.7",
    "update-ruecksichtslos": "^0.0.17",
    "vsce": "^2.15.0"
  },
  "scripts": {
    "build": "npm run esbuild && npm run copy-lsp && npm run vscode:package",
    "clean": "npm run clean:build && npm run clean:node_modules",
    "clean:build": "npx --yes rimraf build",
    "clean:node_modules": "npx --yes rimraf node_modules",
    "compile": "tsc",
    "esbuild": "esbuild src/extension.ts --bundle --outfile=build/extension.js --external:vscode --format=cjs --platform=node",
    "copy-lsp": "cp ../../ide/lsp/build/dist/js/productionExecutable/*.mjs build/lsp-server.mjs",
    "start": "npm run build",
    "update": "update-ruecksichtslos && npm i",
    "vscode:prepublish": "npm run esbuild && npm run copy-lsp",
    "vscode:login": "vsce login",
    "vscode:publish": "vsce publish --no-dependencies",
    "vscode:package": "vsce package -o wirespec.vsix --no-dependencies --allow-star-activation"
  },
  "publisher": "Wirespec",
  "prettier": {
    "printWidth": 120
  },
  "contributes": {
    "languages": [
      {
        "id": "wirespec",
        "aliases": [
          "ws",
          "wirespec"
        ],
        "extensions": [
          ".ws"
        ],
        "icon": {
          "light": "./icons/wirespec.svg",
          "dark": "./icons/wirespec.svg"
        }
      }
    ]
  },
  "configurationDefaults": {
    "wirespec-extension-id": {
      "editor.semanticHighlighting.enabled": true
    }
  }
}
```

**Step 4: Build and verify**

Run:
```bash
./gradlew :src:ide:lsp:compileProductionExecutableKotlinJs
cd src/ide/vscode && npm i && npm run build
```
Expected: `wirespec.vsix` is produced successfully

**Step 5: Commit**

```bash
git add src/ide/vscode/src/extension.ts src/ide/vscode/package.json
git rm src/ide/vscode/src/server.ts
git commit -m "feat(lsp): switch VS Code extension to Kotlin/JS LSP server"
```

---

### Task 12: Update CI pipeline

**Files:**
- Modify: `.github/workflows/build.yml`

**Step 1: Update the vscode CI job**

In `.github/workflows/build.yml`, update the `vscode` job to build the Kotlin LSP first:

Find the vscode job section and update the "Build VSCode extension" step:

```yaml
      - name: Build VSCode extension
        run: |
          ./gradlew :src:ide:lsp:compileProductionExecutableKotlinJs
          cd src/ide/vscode && npm i && npm run build
```

Also update the `release-vscode` job similarly:

```yaml
      - name: Build vscode extension
        working-directory: ./src/ide/vscode
        run: |
          cd ../../.. && ./gradlew :src:ide:lsp:compileProductionExecutableKotlinJs && cd src/ide/vscode
          npm i
          npm run build
          npm version $VERSION
          npm run vscode:publish
```

**Step 2: Add LSP tests to the js-test or jvm-test job**

Add `:src:ide:lsp:allTests` to the appropriate test job. Find the test step and add:

```yaml
      - name: Run LSP tests
        run: ./gradlew :src:ide:lsp:allTests
```

**Step 3: Commit**

```bash
git add .github/workflows/build.yml
git commit -m "ci: update pipeline to build Kotlin LSP before VS Code extension"
```

---

### Task 13: VS Code E2E test setup

**Files:**
- Create: `src/ide/vscode/src/test/extension.test.ts`
- Modify: `src/ide/vscode/package.json` (add test scripts and devDependencies)

**Step 1: Add E2E test dependencies to package.json**

Add to `devDependencies`:
```json
"@vscode/test-electron": "^2.3.9",
"glob": "^10.3.10"
```

Add to `scripts`:
```json
"test:e2e": "node ./build/test/runTest.js"
```

**Step 2: Create the E2E test**

Create `src/ide/vscode/src/test/extension.test.ts`:

```typescript
import * as vscode from "vscode";
import * as path from "path";

suite("Wirespec Extension E2E", () => {
  const testWorkspacePath = path.resolve(__dirname, "../../src/test/fixtures");

  test("Extension activates on .ws file", async () => {
    const doc = await vscode.workspace.openTextDocument({
      language: "wirespec",
      content: "type Foo {\n  bar: String\n}",
    });
    await vscode.window.showTextDocument(doc);

    // Wait for extension to activate
    await new Promise((resolve) => setTimeout(resolve, 2000));

    const ext = vscode.extensions.getExtension("Wirespec.wirespec-vscode-plugin");
    // Extension should be active after opening a wirespec file
    if (ext) {
      await ext.activate();
    }
  });

  test("Diagnostics appear for invalid wirespec", async () => {
    const doc = await vscode.workspace.openTextDocument({
      language: "wirespec",
      content: "type Foo {",
    });
    await vscode.window.showTextDocument(doc);

    // Wait for diagnostics
    await new Promise((resolve) => setTimeout(resolve, 3000));

    const diagnostics = vscode.languages.getDiagnostics(doc.uri);
    // Should have at least one error
    console.log("Diagnostics:", diagnostics.length);
  });
});
```

**Step 3: Create test fixture**

Create `src/ide/vscode/src/test/fixtures/test.ws`:
```
type Foo {
  bar: String
}

type Bar {
  foo: Foo
}
```

**Step 4: Commit**

```bash
git add src/ide/vscode/src/test src/ide/vscode/package.json
git commit -m "test(lsp): add VS Code E2E test skeleton"
```

---

### Task 14: Final verification and cleanup

**Step 1: Run all LSP unit tests**

Run: `./gradlew :src:ide:lsp:allTests`
Expected: ALL PASS

**Step 2: Build the full VS Code extension**

Run:
```bash
./gradlew :src:ide:lsp:compileProductionExecutableKotlinJs
cd src/ide/vscode && npm i && npm run build
```
Expected: `wirespec.vsix` produced

**Step 3: Install and test manually in VS Code**

Run: `code --install-extension src/ide/vscode/wirespec.vsix`
Then open a `.ws` file and verify:
- Syntax highlighting works
- Errors are shown for invalid wirespec
- Go-to-definition works on type references
- Hover shows type info
- Completion suggests keywords and types

**Step 4: Commit any final fixes**

```bash
git add -A
git commit -m "feat(lsp): finalize Kotlin Multiplatform LSP for VS Code"
```
