# Wirespec Language Server

Editor-agnostic implementation of the Language Server Protocol for Wirespec, written in Kotlin Multiplatform.

This module produces two artifacts:

- **JS (Node)** — published to `build/compileSync/js/main/productionExecutable/kotlin/` and re-bundled into the VS Code extension. See `src/ide/vscode`.
- **JVM** — a fat-jar built by the `:src:ide:lsp:jvmJar` task, runnable as `java -jar lsp-jvm-<version>.jar`. Intended to be consumed by the IntelliJ plugin in a follow-up.

The server speaks JSON-RPC 2.0 over either:

- **Node IPC** (default for the VS Code extension, selected with `--node-ipc` on the JS bin)
- **stdio** (default for everything else, including the JVM build)

## Capabilities

- `textDocument/publishDiagnostics` — surfaces parser errors as squiggles, and warns on a `part` that no `...Part` in the document spreads.
- `textDocument/semanticTokens/full` — colorizes keywords, types, identifiers, HTTP methods, and the `...` spread operator.
- `textDocument/definition` — for a type or part identifier, returns every occurrence in the same document, spreads included.
- `textDocument/rename` — renames a type or part identifier and every reference to it, spreads included.

## Build

```bash
./gradlew :src:ide:lsp:assemble     # builds JS + JVM
./gradlew :src:ide:lsp:jvmJar       # JVM fat-jar only
```
