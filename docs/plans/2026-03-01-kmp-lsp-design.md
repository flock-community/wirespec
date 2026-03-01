# Kotlin Multiplatform LSP for VS Code

## Goal

Rebuild the VS Code LSP server in Kotlin Multiplatform, compiled to JavaScript, bundled into the VS Code extension. The LSP should be reusable for other purposes (e.g., AI agent skills) in the future.

## Decision Summary

- **Module:** Single new KMP module at `src/ide/lsp/`
- **Protocol:** Full Kotlin LSP implementation (no TypeScript server, no `vscode-languageserver`)
- **Transport:** Node.js process spawned by VS Code, communicating over stdio
- **Features:** Full feature set (diagnostics, semantic tokens, go-to-definition, find references, rename, hover, completion, code actions)
- **Testing:** 3 layers (unit, integration, VS Code E2E)

## Module Structure

```
src/ide/lsp/
├── build.gradle.kts
├── src/
│   ├── commonMain/kotlin/community/flock/wirespec/ide/lsp/
│   │   ├── protocol/
│   │   │   ├── Messages.kt        # JSON-RPC request/response/notification
│   │   │   ├── Lifecycle.kt       # Initialize, Shutdown, Exit
│   │   │   ├── TextDocument.kt    # DidOpen, DidChange, DidClose
│   │   │   ├── Language.kt        # Completion, Hover, Definition, References, Rename
│   │   │   └── Diagnostic.kt      # PublishDiagnostics
│   │   ├── server/
│   │   │   ├── LspServer.kt       # Main server loop, dispatches requests
│   │   │   ├── DocumentStore.kt   # Tracks open documents and their content
│   │   │   └── WorkspaceIndex.kt  # Symbol index across files
│   │   └── features/
│   │       ├── DiagnosticsProvider.kt
│   │       ├── SemanticTokensProvider.kt
│   │       ├── DefinitionProvider.kt
│   │       ├── ReferencesProvider.kt
│   │       ├── RenameProvider.kt
│   │       ├── HoverProvider.kt
│   │       ├── CompletionProvider.kt
│   │       └── CodeActionProvider.kt
│   ├── commonTest/kotlin/community/flock/wirespec/ide/lsp/
│   │   ├── protocol/
│   │   │   └── MessagesTest.kt
│   │   ├── server/
│   │   │   ├── DocumentStoreTest.kt
│   │   │   └── WorkspaceIndexTest.kt
│   │   └── features/
│   │       ├── DiagnosticsProviderTest.kt
│   │       ├── SemanticTokensProviderTest.kt
│   │       ├── DefinitionProviderTest.kt
│   │       ├── ReferencesProviderTest.kt
│   │       ├── RenameProviderTest.kt
│   │       ├── HoverProviderTest.kt
│   │       ├── CompletionProviderTest.kt
│   │       └── CodeActionProviderTest.kt
│   ├── jsMain/kotlin/community/flock/wirespec/ide/lsp/
│   │   └── Main.kt                # Node.js entry point (stdin/stdout)
│   └── jsTest/kotlin/community/flock/wirespec/ide/lsp/
│       └── LspIntegrationTest.kt   # Spawn server, send JSON-RPC, verify
```

**Dependencies:**
- `compiler:core` (tokenize, parse, AST)
- `kotlinx-serialization-json` (JSON-RPC message ser/de)
- `kotlinx-io` (stdin/stdout I/O)

## LSP Protocol Layer

### JSON-RPC 2.0 Messages

Modeled as sealed classes with `kotlinx-serialization`:

- `JsonRpcMessage` — base with `jsonrpc: "2.0"`
- `JsonRpcRequest(id, method, params)` — client requests expecting response
- `JsonRpcNotification(method, params)` — client notifications (no response)
- `JsonRpcResponse(id, result?, error?)` — server responses

### Content-Length Framing

LSP uses HTTP-style headers over stdio:
```
Content-Length: 52\r\n\r\n{"jsonrpc":"2.0","method":"initialized","params":{}}
```

Server reads `Content-Length` header, then reads that many bytes of JSON body.

### Method Dispatch

`LspServer` maps method names to handlers:

| Method | Handler | Type |
|--------|---------|------|
| `initialize` | `handleInitialize` | Request |
| `initialized` | `handleInitialized` | Notification |
| `textDocument/didOpen` | `handleDidOpen` | Notification |
| `textDocument/didChange` | `handleDidChange` | Notification |
| `textDocument/didClose` | `handleDidClose` | Notification |
| `textDocument/semanticTokens/full` | `handleSemanticTokens` | Request |
| `textDocument/definition` | `handleDefinition` | Request |
| `textDocument/references` | `handleReferences` | Request |
| `textDocument/rename` | `handleRename` | Request |
| `textDocument/hover` | `handleHover` | Request |
| `textDocument/completion` | `handleCompletion` | Request |
| `textDocument/codeAction` | `handleCodeAction` | Request |
| `shutdown` | `handleShutdown` | Request |
| `exit` | process exit | Notification |

### Server Capabilities

Advertised in `initialize` response:

- `textDocumentSync: Full`
- `semanticTokensProvider` (full, with legend)
- `definitionProvider: true`
- `referencesProvider: true`
- `renameProvider: { prepareProvider: true }`
- `hoverProvider: true`
- `completionProvider: { triggerCharacters: [] }`
- `codeActionProvider: true`

## Analysis Features

### DocumentStore

Tracks open `.ws` files as `Map<FileUri, DocumentState>`:
- `DocumentState` holds source text, cached tokens, and parsed AST
- Re-tokenizes and re-parses on `didChange`
- Publishes diagnostics after each parse

### WorkspaceIndex

Cross-file symbol table built from parsed AST:
- `definitions: Map<String, List<Location>>` — identifier to definition locations
- `references: Map<String, List<Location>>` — identifier to usage locations
- Rebuilt incrementally when a document changes

### Feature Implementations

| Feature | Input | Logic |
|---------|-------|-------|
| **Diagnostics** | Document change | `tokenize()` + `parse()`, map `WirespecException` to LSP `Diagnostic` |
| **Semantic Tokens** | Full document | `tokenize()`, map token types to LSP token types |
| **Go-to-Definition** | Position | Find token at position, look up in `WorkspaceIndex.definitions` |
| **Find References** | Position | Find token at position, look up in `WorkspaceIndex.references` + `definitions` |
| **Rename** | Position + new name | Find all occurrences via index, produce `WorkspaceEdit` |
| **Hover** | Position | Resolve to definition, format type info as markdown |
| **Completion** | Position | Determine context, suggest from index + keywords |
| **Code Actions** | Diagnostic range | Match diagnostic to fix (e.g., "Create type X") |

### Semantic Token Type Mapping

| Wirespec Token | LSP Semantic Token | Modifier |
|----------------|-------------------|----------|
| `WirespecDefinition` (type, enum, endpoint, channel) | `keyword` | — |
| `PascalCaseIdentifier` (type position) | `type` | `declaration` (at def site) |
| `DromedaryCaseIdentifier` (field position) | `property` | — |
| `SpecificType` (WsString, WsInteger, etc.) | `type` | `defaultLibrary` |
| `Comment` | `comment` | — |
| `Annotation` | `decorator` | — |
| `Method` (GET, POST, etc.) | `keyword` | — |
| `StatusCode` | `number` | — |

## VS Code Extension Changes

### Current State (to be replaced)

- `src/extension.ts` — launches TypeScript server via IPC
- `src/server.ts` — full LSP server in TypeScript (~164 lines)
- Dependencies: `vscode-languageserver`, `vscode-languageclient`, `@flock/wirespec`

### New State

- `src/extension.ts` — simplified, launches Kotlin/JS server via stdio
- `src/server.ts` — **deleted**
- Dependencies removed: `vscode-languageserver`, `@flock/wirespec`
- Dependencies kept: `vscode-languageclient`

### Extension Client (simplified)

```typescript
const serverModule = context.asAbsolutePath('build/lsp-server.js');
const serverOptions: ServerOptions = {
  run: { module: serverModule, transport: TransportKind.stdio },
  debug: { module: serverModule, transport: TransportKind.stdio }
};
```

## Build Pipeline

1. Gradle builds `src/ide/lsp/` JS target → produces executable `lsp-server.js`
2. Copy `lsp-server.js` into `src/ide/vscode/build/`
3. esbuild bundles `extension.ts` (client only)
4. `vsce package` creates `.vsix`

### CI Changes

- Add LSP `allTests` to existing CI test jobs
- Build LSP JS artifact before VS Code npm build (replaces `jsNodeProductionLibraryDistribution`)
- Add VS Code E2E test step after `.vsix` build

## Testing Strategy

### Layer 1: Unit Tests (commonTest)

- Feature providers tested with sample wirespec source
- JSON-RPC message serialization/deserialization
- DocumentStore and WorkspaceIndex behavior
- Content-Length framing (parse and write)
- Run via `./gradlew :src:ide:lsp:allTests`

### Layer 2: Integration Tests (jsTest)

- Spawn LSP server as Node.js process
- Send JSON-RPC over stdin, read responses from stdout
- Full lifecycle: `initialize` → `didOpen` → feature requests → `shutdown`
- Verify diagnostics on parse errors
- Verify all feature responses produce correct results

### Layer 3: VS Code E2E Tests

- Use `@vscode/test-electron` framework
- Test workspace with `.ws` files
- Verify: diagnostics, semantic highlighting, go-to-definition, find references, rename
- Run via `npm run test:e2e` in `src/ide/vscode/`
- CI step validates `.vsix` installs and activates correctly
