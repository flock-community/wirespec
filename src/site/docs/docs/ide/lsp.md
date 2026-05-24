---
title: Language Server
sidebar_position: 4
---

# Wirespec Language Server (LSP)

The Wirespec compiler ships a [Language Server Protocol](https://microsoft.github.io/language-server-protocol/) implementation that powers in-editor diagnostics, semantic highlighting, go-to-definition, and rename refactoring for `.ws` files. It is the engine behind the [VS Code extension](./ide.md#vs-code) — that extension bundles the server and launches it automatically, so there is nothing to install or configure separately.

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

## What this looks like in VS Code

Install the [Wirespec extension](./ide.md#vs-code) and the server runs in the background. Then in any `.ws` file:

- **Diagnostics** appear as red squiggles when you have a syntax error.
- **Cmd-click** (or **F12**) on a type identifier jumps between its declaration and its usages.
- **F2** (or "Rename Symbol" in the context menu) on a user-defined type renames it everywhere in the current file.
