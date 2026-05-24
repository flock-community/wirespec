---
title: Language Server
sidebar_position: 4
---

# Wirespec Language Server (LSP)

The Wirespec compiler ships a [Language Server Protocol](https://microsoft.github.io/language-server-protocol/) implementation that powers in-editor diagnostics, semantic highlighting, go-to-definition, and rename refactoring for `.ws` files. The server is the same Kotlin Multiplatform module on every platform — the VS Code extension consumes a Node build of it, and any other LSP-capable editor can install the same Node build from npm.

It speaks plain JSON-RPC 2.0 over either Node IPC (used automatically by VS Code) or stdio (used by every other editor).

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

The Wirespec VS Code extension already bundles the LSP. Install the extension and the server is launched automatically.

- **Diagnostics** appear as red squiggles when you have a syntax error.
- **Cmd-click** (or **F12**) on a type identifier jumps between its declaration and its usages.
- **F2** (or "Rename Symbol" in the context menu) on a user-defined type renames it everywhere in the current file.

See the [IDE page](./ide.md) for installation instructions.

## Using it from another editor

The language server ships inside the [`@flock/wirespec`](https://www.npmjs.com/package/@flock/wirespec) npm package as a `wirespec-lsp` bin. Install the package globally:

```bash
npm install -g @flock/wirespec
```

This puts a `wirespec-lsp` binary on your `PATH` (alongside the `wirespec` CLI). Point your editor at it with `--stdio`:

```bash
wirespec-lsp --stdio
```

For one-shot use without a global install, `npx --yes @flock/wirespec@{{WIRESPEC_VERSION}} wirespec-lsp --stdio` also works — useful for editor configs that spawn the LSP per project.

For example, in Neovim with `nvim-lspconfig`:

```lua
local lspconfig = require("lspconfig")
local configs = require("lspconfig.configs")

if not configs.wirespec then
  configs.wirespec = {
    default_config = {
      cmd = { "wirespec-lsp", "--stdio" },
      filetypes = { "wirespec" },
      root_dir = lspconfig.util.root_pattern(".git", "pom.xml", "build.gradle.kts"),
    },
  }
end

lspconfig.wirespec.setup({})
```
