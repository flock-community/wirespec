# Architecture Pipeline Overview + IR Model Framing

**Date:** 2026-05-09
**Status:** Approved
**Builds on:** `2026-05-08-architecture-menu-design.md` (which created `architecture/` menu and moved the IR Model page into it).

## Problem

The Architecture menu has two pages today:

- `architecture/architecture.md` — a 9-line stub.
- `architecture/architecture-ir.md` — a deep dive on the IR pipeline (Convert → Transform → Generate).

A reader landing on the menu sees no overview of how the Wirespec compiler actually works end-to-end. The IR page jumps straight into IR detail without explaining where the IR sits in the broader compiler flow. Two specific gaps:

1. **No top-level pipeline overview.** Tokenize → Parse → Emit is not documented anywhere in the docs site. New readers cannot place the IR pipeline in context.
2. **IR Model is unframed.** The IR pipeline runs *inside* emitters that opt into the IR approach. The current page does not say this. A reader could reasonably conclude that all emitters use IR, or that IR is the entire compiler. Both are wrong: the codebase has direct AST emitters (e.g., `PythonEmitter`) alongside IR-based ones (e.g., `PythonIrEmitter`).

## Goal

Produce a self-contained Architecture overview at the menu's index page, and reframe the IR Model page so its scope is unambiguous.

Two files change. No new files. No sidebar changes.

## Non-Goals

- Splitting the overview into per-stage pages (Tokenizer.md, Parser.md, EmitterStyles.md). One page is enough for now.
- Documenting every `TokenType`, every `Definition` subtype, or every emitter.
- Migrating internal CLAUDE.md content into docs wholesale.
- Replacing ASCII diagrams with Mermaid or SVG.
- Editing any direct or IR-based emitter source code.

## Source-of-Truth References

The overview content is grounded in these files (verified during brainstorming):

| Topic | Source |
|---|---|
| Top-level compile flow (`tokenize → parse → emit`) | `src/compiler/core/src/commonMain/kotlin/community/flock/wirespec/compiler/core/Compiler.kt` |
| Tokenizer | `src/compiler/core/src/commonMain/kotlin/community/flock/wirespec/compiler/core/tokenize/Tokenizer.kt` |
| Parser entry | `src/compiler/core/src/commonMain/kotlin/community/flock/wirespec/compiler/core/parse/Parser.kt` |
| Per-definition parsers | `src/compiler/core/.../parse/{Type,Endpoint,Channel,Enum,Annotation}Parser.kt` |
| Parser AST | `src/compiler/core/.../parse/ast/{Root,Module,Definition,Reference}.kt` |
| Direct emitter example | `src/compiler/emitters/python/.../PythonEmitter.kt` |
| IR-based emitter example | `src/compiler/emitters/python/.../PythonIrEmitter.kt` |

The codebase calls the lexing stage **"Tokenizer"**, not "Lexer". The overview will use "Tokenizer" as the primary term and note that "Lexer" is a synonym.

## Detailed Changes

### 1. Rewrite `src/site/docs/docs/architecture/architecture.md`

Replace the entire file (currently 9 lines: frontmatter + heading + 2-line stub paragraph) with the content below.

**Frontmatter is unchanged:**
```yaml
---
title: Architecture
sidebar_position: 10
---
```

**Body sections (in order):**

1. **`# Architecture` heading** — same as today.

2. **Intro paragraph (2–3 sentences):**
   The Wirespec compiler turns a `.ws` source file into target-language source code through three stages: **Tokenize**, **Parse**, and **Emit**. This page walks through each stage; deeper material on what happens inside an emitter lives in the linked sub-pages.

3. **`## Pipeline` section with the canonical diagram:**
   ```
   Wirespec source (string)
         │
         ▼  Tokenize   (LanguageSpec.tokenize)
   List<Token>
         │
         ▼  Parse      (Parser.parse)
   AST  (Root → Module[] → Definition[])
         │
         ▼  Emit       (one or more emitters)
   List<Emitted>  (filename + content per generated file)
   ```

4. **`## Tokenize` section (~80 words):**
   Describes scanning the source string into tokens (keywords, identifiers, punctuation, regex literals) using `LanguageSpec.tokenize` from `src/compiler/core/.../tokenize/Tokenizer.kt`. Notes that the codebase calls this the **Tokenizer**; "Lexer" is the same concept. Mentions that whitespace is removed by default and that the output is a `NonEmptyList<Token>`.

5. **`## Parse` section (~100 words):**
   Describes consuming the token stream into a typed AST. Top-level entry: `Parser.parse` in `src/compiler/core/.../parse/Parser.kt`. Per-definition parsers (`TypeParser`, `EndpointParser`, `ChannelParser`, `EnumParser`, `AnnotationParser`) handle each Wirespec construct. The output is an `AST` value containing modules of `Definition`s. Brief note on the AST shape: `Root → Module[] → Definition[]` where `Definition` is a sealed hierarchy (`Type`, `Enum`, `Union`, `Refined`, `Endpoint`, `Channel`).

6. **`## Emit` section (~100 words):**
   Describes how each registered emitter consumes the AST and produces `Emitted` values (a filename + content pair per generated file). Notes that multiple emitters can run on the same AST in one compile, producing output for multiple target languages simultaneously. Points to the entry point in `Compiler.kt`:
   ```kotlin
   fun CompilationContext.compile(source) = emit(parse(source))
   ```

7. **`## Two emitter styles` section (~120 words):**
   Explains that the codebase has two coexisting emitter approaches:

   - **Direct emitters** walk the AST and assemble target-language strings directly. Each language construct is rendered by inspecting the AST node and writing the equivalent target syntax. Example: `PythonEmitter` in `src/compiler/emitters/python/.../PythonEmitter.kt`.
   - **IR-based emitters** run an internal **Convert → Transform → Generate** pipeline. The AST is converted into Wirespec's language-neutral IR, then per-language transforms reshape the IR for the target's idioms, then a generator walks the transformed IR to produce the source string. Example: `PythonIrEmitter`. See **[IR Model](./architecture-ir.md)** for the details of this internal pipeline.

   Both styles satisfy the same `Emitter` contract (input AST, output `List<Emitted>`); the difference is purely internal to each emitter.

### 2. Update `src/site/docs/docs/architecture/architecture-ir.md`

Two surgical edits — frontmatter and body content otherwise unchanged.

**Edit A — Insert a new framing paragraph between the `# IR Model` heading and the existing first paragraph.**

Current top of file (after frontmatter, lines 6–8):
```markdown
# IR Model

Wirespec compiles `.ws` source files into code for multiple target languages. After parsing Wirespec source into an AST, the compiler uses an **Intermediate Representation (IR)** — a language-neutral tree that sits between the parser and code generation. This page explains the IR pipeline and the model each Wirespec definition is converted into.
```

Replace with:
```markdown
# IR Model

The IR pipeline described on this page runs **inside an emitter** — specifically, emitters that opt into the IR approach (e.g., `JavaIrEmitter`, `KotlinIrEmitter`, `PythonIrEmitter`, `RustIrEmitter`, `ScalaIrEmitter`, `TypeScriptIrEmitter`). It sits between the parser's AST and the final string output, replacing the direct AST-walking that older emitters (e.g., `PythonEmitter`) use. For where IR-based emitters fit in the overall compiler flow, see [Architecture](./architecture.md).

Wirespec compiles `.ws` source files into code for multiple target languages. After parsing Wirespec source into an AST, an IR-based emitter uses an **Intermediate Representation (IR)** — a language-neutral tree that sits between the parser and code generation. This page explains the IR pipeline and the model each Wirespec definition is converted into.
```

The change to the second paragraph: "the compiler uses" → "an IR-based emitter uses" (one substitution) so the framing stays consistent.

**Edit B — Update the Pipeline Overview diagram so the first node makes the scope explicit.**

The existing diagram (lines 14–31 of the current file) starts with:
```
Wirespec AST (definitions)
```

Change that label to:
```
Wirespec AST (produced by parser, consumed by emitter)
```

The arrows and the rest of the diagram (Convert / Transform / Generate boxes) are unchanged.

No other content in `architecture-ir.md` is modified.

## Verification

In `src/site/docs/`:

1. `npm run build` succeeds with exit 0 and prints `[SUCCESS] Generated static files in "build"`.
2. No **new** broken-link warnings beyond the pre-existing `/how#contract`, `/how#generate`, `/how#validate` set already known to appear on every page.
3. `build/docs/architecture/index.html` exists and renders the new overview.
4. `build/docs/architecture/ir/index.html` exists and starts with the new framing paragraph.
5. The internal links in both pages resolve:
   - `architecture.md` → `./architecture-ir.md` resolves to `/docs/architecture/ir`.
   - `architecture-ir.md` → `./architecture.md` resolves to `/docs/architecture/`.

## Risks / Notes

- **Naming choice:** Using "Tokenizer" matches the codebase but the user's request said "Lexer". Mitigation: the Tokenize section explicitly notes "Lexer" as a synonym, so a reader searching for either term lands correctly.
- **Drift:** Source files cited in the overview can move. The doc references stable package paths (`src/compiler/core/.../tokenize/`) rather than line numbers, which absorbs minor reorganization. If a Tokenizer or Parser is ever renamed, this page needs updating; that risk is accepted (low frequency).
- **Style overlap with intro:** `intro/intro.md` and `intro/intro-generate.md` describe Wirespec at the *user* level. The Architecture overview is at the *compiler-internals* level. The two should not duplicate or contradict; this spec does not modify the intro pages.
- **No cross-link from intro/:** Intentionally not adding "see Architecture" links from user-facing intro pages. The intro audience does not need internals; if discoverability is an issue later, that is a separate concern.
