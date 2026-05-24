---
title: Language Server
sidebar_position: 4
---

# Wirespec Language Server (LSP)

The Wirespec compiler ships a [Language Server Protocol](https://microsoft.github.io/language-server-protocol/) implementation that powers in-editor diagnostics, semantic highlighting, go-to-definition, and rename refactoring for `.ws` files. The server is the same Kotlin Multiplatform module on every platform — the VS Code extension consumes a Node build of it, and the IntelliJ plugin will consume the JVM build.

It speaks plain JSON-RPC 2.0, which means anything that can launch a process and exchange framed messages can use it — your editor, your CI pipeline, or a coding agent driving the compiler programmatically.

## Capabilities

The server advertises the following capabilities in its `initialize` response:

| Capability | LSP request | What it does |
|---|---|---|
| `textDocumentSync` (full) | `textDocument/didOpen`, `didChange`, `didClose` | Tracks an in-memory copy of every opened document. |
| `publishDiagnostics` | server-pushed | Surfaces tokenizer and parser errors as squiggles after every open/change. |
| `semanticTokensProvider` | `textDocument/semanticTokens/full` | Classifies tokens as `keyword`, `type`, `variable`, or `method` for editor theming. |
| `definitionProvider` | `textDocument/definition` | For any user-defined type identifier, returns the declaration and every reference in the current document. |
| `renameProvider` (with `prepareProvider`) | `textDocument/prepareRename`, `textDocument/rename` | Renames every occurrence of a user-defined type identifier (those introduced by `type`, `enum`, `endpoint`, `channel`). Refuses keywords, built-in types, and field names. |

Workspace-wide rename and cross-file go-to-definition are not yet supported — the server tracks documents individually.

## Using it from VS Code

The Wirespec VS Code extension already bundles the LSP. Install the extension and the server is launched automatically as a Node subprocess via `vscode-languageclient`'s `TransportKind.ipc` transport.

- **Diagnostics** appear as red squiggles when you have a syntax error.
- **Cmd-click** (or **F12**) on a type identifier jumps between its declaration and its usages.
- **F2** (or "Rename Symbol" in the context menu) on a user-defined type renames it everywhere in the current file.

See the [IDE page](./ide.md) for installation instructions.

## Using it from another editor

The server is a standalone process that speaks LSP over stdio. To use it from Neovim, Helix, Emacs, Sublime, or any other LSP-capable editor:

### Option 1 — Node (recommended)

Build the JS executable once:

```bash
./gradlew :src:ide:lsp:assemble
```

This produces a Node ES-module bundle under `src/ide/lsp/build/compileSync/js/main/productionExecutable/kotlin/`. Configure your editor to launch it with `--stdio`:

```bash
node /path/to/wirespec/src/ide/lsp/build/compileSync/js/main/productionExecutable/kotlin/wirespec-src-ide-lsp.mjs --stdio
```

For example, in Neovim with `nvim-lspconfig`:

```lua
local lspconfig = require("lspconfig")
local configs = require("lspconfig.configs")

if not configs.wirespec then
  configs.wirespec = {
    default_config = {
      cmd = {
        "node",
        "/abs/path/to/wirespec/src/ide/lsp/build/compileSync/js/main/productionExecutable/kotlin/wirespec-src-ide-lsp.mjs",
        "--stdio",
      },
      filetypes = { "wirespec" },
      root_dir = lspconfig.util.root_pattern(".git", "pom.xml", "build.gradle.kts"),
    },
  }
end

lspconfig.wirespec.setup({})
```

### Option 2 — JVM

```bash
./gradlew :src:ide:lsp:jvmJar
java -jar src/ide/lsp/build/libs/lsp-jvm-*.jar
```

The JVM build reads from `System.in` and writes to `System.out`, also using `Content-Length` framing. Useful when you don't want a Node runtime in your toolchain — for example, when embedding the server inside another JVM process such as the IntelliJ plugin.

## Using it from a coding agent

A coding agent — whether scripted, AI-driven, or a CI tool — can drive the language server as a child process to get programmatic access to the compiler's diagnostics, semantic model, and refactoring. This avoids re-implementing Wirespec parsing in the agent and means the agent's understanding of a `.ws` file matches what the IDE sees.

Common agent use cases:

- **Validate before you write.** Before committing a generated `.ws` file, send it to the server via `didOpen` and read the `publishDiagnostics` notification. Treat any diagnostic of severity `1` (error) as a hard failure.
- **Discover references.** Use `textDocument/definition` on a type name to find every place a type is used before you touch it.
- **Refactor safely.** Drive `textDocument/rename` instead of `sed`-style text substitution — the server only edits real identifiers, not similarly-named substrings inside comments or string literals.
- **Inspect the semantic model.** `textDocument/semanticTokens/full` gives you the same token classification (`keyword` / `type` / `variable` / `method`) the editor uses. Useful for tooling that wants to reason about a `.ws` file without re-implementing the tokenizer.

### Wire format

Every message is a JSON-RPC 2.0 envelope framed with a `Content-Length` header, like every LSP server:

```
Content-Length: 79\r\n
\r\n
{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"processId":null}}
```

The full request/response model is documented in the [LSP specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.17/specification/).

### Minimal Node example

The following script spawns the server, opens a document, and prints whatever the server pushes back. It uses no external libraries:

```js
import { spawn } from "node:child_process";

const child = spawn("node", [
  "/abs/path/to/wirespec/src/ide/lsp/build/compileSync/js/main/productionExecutable/kotlin/wirespec-src-ide-lsp.mjs",
  "--stdio",
]);

const send = (msg) => {
  const json = JSON.stringify(msg);
  const len = Buffer.byteLength(json, "utf8");
  child.stdin.write(`Content-Length: ${len}\r\n\r\n${json}`);
};

let buf = "";
child.stdout.on("data", (chunk) => {
  buf += chunk.toString("utf8");
  while (true) {
    const m = buf.match(/Content-Length: (\d+)\r\n\r\n/);
    if (!m) return;
    const start = m.index + m[0].length;
    const length = parseInt(m[1], 10);
    if (buf.length < start + length) return;
    const body = buf.slice(start, start + length);
    buf = buf.slice(start + length);
    console.log(JSON.parse(body));
  }
});

const SOURCE = `type Person {
  name: String,
  age: Integer
}
`;

send({ jsonrpc: "2.0", id: 1, method: "initialize", params: { processId: null } });
send({ jsonrpc: "2.0", method: "initialized", params: {} });
send({
  jsonrpc: "2.0",
  method: "textDocument/didOpen",
  params: {
    textDocument: {
      uri: "file:///agent.ws",
      languageId: "wirespec",
      version: 1,
      text: SOURCE,
    },
  },
});
```

You should see at least three messages from the server: the `initialize` response, a `textDocument/publishDiagnostics` notification (empty `diagnostics` array for a well-formed file), and any subsequent responses you request.

### Rename as a refactoring primitive

Renames are returned as a [`WorkspaceEdit`](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.17/specification/#workspaceEdit) with a `changes` map keyed by document URI:

```js
send({
  jsonrpc: "2.0",
  id: 2,
  method: "textDocument/rename",
  params: {
    textDocument: { uri: "file:///agent.ws" },
    position: { line: 0, character: 6 },
    newName: "User",
  },
});
```

The response looks like:

```json
{
  "jsonrpc": "2.0",
  "id": 2,
  "result": {
    "changes": {
      "file:///agent.ws": [
        { "range": { "start": { "line": 0, "character": 5 }, "end": { "line": 0, "character": 11 } }, "newText": "User" }
      ]
    }
  }
}
```

A `null` result means the rename was refused — either because the cursor isn't on a renameable identifier (`prepareRename` would also have returned `null`) or because the new name is not a valid Wirespec PascalCase identifier (`^[A-Z][A-Za-z0-9_]*$`). Treat `null` as a no-op rather than an error.

### Tips

- Always send `initialize` (and wait for the response) before any document operation. Other requests will be ignored or error out before initialization completes.
- The server announces full-document sync, so every `didChange` notification must carry the complete new text in the last `contentChanges` entry — incremental edits are not yet supported.
- If you only need a one-shot answer ("is this file valid?"), it's fine to spawn the server, do an `initialize` + `didOpen`, read the resulting `publishDiagnostics`, and kill the process. The JVM build cold-starts in well under a second.
