# Wirespec style guide

Every change to Wirespec follows this guide, whoever writes it: a contributor, a maintainer or Claude. Reviews point to
it, and the pull request template asks for it. When a review settles a question of style, the answer is added here, so
the next change gets it right without the same comment.

The formatter checks layout: run `./gradlew :<module>:spotlessApply` before you commit. This guide covers what it
cannot check.

## Idiomatic Kotlin

- **Prefer expressions over statements.** Use expression bodies (`fun x(): T = ...`), `when` and `if` as expressions,
  and single-expression chains.
- **Spell out the return type of anything effectively public.** Explicit API mode is on for every module, so the
  compiler requires it.
- **Chain calls where you can.** Use `map`/`filter`/`flatMap`/`fold` pipelines instead of intermediate variables and
  mutable accumulation.
- **Use scope functions, and pick the right one:** `apply` for configuration, `let` for null-safe transforms, `also`
  for side effects, `run` and `with` where a block needs a receiver.
- **Handle null with `?.let`, `takeIf` and `?:` chains.** Never write `?: return null`.
- **Immutability first:** `val` over `var`, `List` over `MutableList`, data classes and sealed hierarchies.
- **Use the narrowest visibility that works:**
  - `private` if it is only used in its own file;
  - `internal` if it is only used inside its own module;
  - `public` only when another module or a consumer needs it.

  Never add `public` just to silence the compiler: check who actually calls it first.
- **Make `when` over a sealed type exhaustive.** No `else` branch that hides missing cases.
- **Use extension functions for behaviour on types you don't own.** Keep them small and composable.

## Data classes

- **No default values in data classes.** Every construction states every value. A default hides at the call site what
  a value is, and a new field with a default slips past every construction that should decide on it. Where a bare value
  would not read clearly, name the argument:

  ```kotlin
  Module(fileUri, statements, annotations = emptyList())
  ```

  Functions may have defaults; constructors of data classes may not.

## Readability

- **Name the parts of an expression that needs a second read.** Logic inside a string template, or a cast and a safe
  call inside a constructor argument, goes into a well-named extension function or value:

  ```kotlin
  // Not
  "@spec:${annotation.name}${(annotation.parameters.find { it.name == "default" }?.value as? Annotation.Value.Single)?.let { "(\"${it.value}\")" }.orEmpty()} is declared twice"

  // But
  "${annotation.label()} is declared twice"
  ```

- **Explain a workaround where it is.** When code works around another layer, say why at that spot, so a reader does
  not have to ask. Examples are a token the tokenizer merges and a field a library lacks:

  ```kotlin
  /**
   * An array starts with `[`, or is the empty `[]`: the tokenizer reads `[]` as one [Brackets] token, the suffix of a type
   * such as `String[]`, so an empty array never has a [LeftBracket] of its own.
   */
  private fun TokenType.startsArray(): Boolean = this is LeftBracket || this is Brackets
  ```

- **Be sparing with other comments.** No narration, and no KDoc that restates the signature. Comment only constraints
  the code cannot express.

## Design

- **Return what a caller needs; don't remember it for the caller.** Don't add state to a shared class to serve one
  caller, such as a "previous token" in `TokenProvider`. The function that has the information returns it:
  `parseAnnotation` returns the tokens an annotation starts and ends with, so the file header knows where it ends.
- **Match the surrounding code.** The naming, idiom and structure of the file you're editing win over personal
  preference.

## Parser

- **Consume a token whose type you know with `expect<T>()`,** not with `eatToken()` after a type check. It says what
  the parser expects, and when the input has another token, it reports that one:

  ```kotlin
  is Brackets -> expect<Brackets>().bind().let { emptyList() }
  ```
