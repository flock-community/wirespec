# Wirespec Language Server

Editor-agnostic implementation of the Language Server Protocol for Wirespec. Provides diagnostics, semantic highlighting, and go-to-definition by delegating to the Wirespec compiler.

This module was extracted from `src/ide/vscode` and is currently consumed by the VS Code extension via a local `file:` link. It is planned to be rebuilt as a Kotlin Multiplatform module (JS + JVM targets) so that it can also serve the IntelliJ plugin and be published to npm for other LSP-capable editors. See [`docs/superpowers/specs/2026-05-24-extract-lsp-module-design.md`](../../../docs/superpowers/specs/2026-05-24-extract-lsp-module-design.md) for the migration plan.

## Build

```bash
npm install
npm run build
```

Produces `build/server.js`, a single CommonJS bundle that can be spawned as a Node subprocess by an LSP client.
