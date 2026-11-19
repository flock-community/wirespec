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
  private fun TokenProvider.parseArray(): Either<WirespecException, List<Annotation.Value.Single>> = ...
  ```

- **Be sparing with other comments.** No narration, and no KDoc that restates the signature. Comment only constraints
  the code cannot express.

## Design

- **Return what a caller needs; don't remember it for the caller.** Don't add state to a shared class to serve one
  caller, such as a "previous token" in `TokenProvider`. The function that has the information returns it:
  `parseAnnotation` returns the tokens an annotation starts and ends with, so the file header knows where it ends.
- **Match the surrounding code.** The naming, idiom and structure of the file you're editing win over personal
  preference.

## Compiler

The compiler works in stages. Each stage hands the next one its result, or the errors it found:

1. **Tokenize.** `tokenize` splits the source of a file into tokens, with the matchers of the `LanguageSpec`.
2. **Parse.** The parser reads the tokens of each file through a `TokenProvider`, and builds one `AST` of all the files.
3. **Validate.** The `Validator` checks the whole `AST`, across files.
4. **Emit.** Every emitter checks the `AST` for its own target in `Emitter.validate`, and then emits it.

`compile` runs them in this order, and a stage only runs when the one before it succeeded.

- **Put a check in the stage that owns it.** What may follow what is syntax: the parser checks it and raises a
  `ParserException`. A rule about the spec as a whole, across files, belongs in the `Validator`, as a `ValidationError`.
  What one target cannot express belongs in that emitter's `validate`, as an `EmitterException`. In the file header,
  for example:
  - the parser checks that the `@spec:` annotations come before the `@file:` annotations;
  - the validator checks that a spec annotation is unique;
  - the OpenAPI emitter checks that a security requirement names a declared scheme.
- **Raise errors; don't throw them.** Every stage returns an `Either`. A parse function is an `either { }` block: it
  binds what it calls with `.bind()`, and fails with `raise` or `ensure`. An error is a `WirespecException` with its
  file and, when one token is to blame, that token's coordinates, so the CLI and the IDE can point at it.
- **Report every independent error at once.** Files, definitions and validations that don't depend on each other are
  checked with `flattenOrAccumulate`, `mapOrAccumulate` and `zipOrAccumulate`, so one compile shows every mistake
  instead of only the first.

### Tokenizer

- **The first matcher that fits wins.** The `LanguageSpec` lists its matchers in order, and a longer token comes before
  a shorter one it starts with: `[]` before `[`. So the parser gets the longer token: an empty array is one `Brackets`
  token, not a `LeftBracket` and a `RightBracket`. Where that matters, the parser handles both forms.

### Parser

- **A parse function reads the current token, `token`, and consumes it.** `TokenProvider` keeps no history. What came
  before is in the values that parse functions return (see *Return what a caller needs* under Design).
- **Branch on the current token with `when (token.type)`.** There is one branch per token type, and `else` for the
  rest. Tokens that are read the same way share a branch. Don't move a type check into a helper and switch to a `when`
  without a subject: a reader of the parser looks for the token types in the branches.

  ```kotlin
  when (token.type) {
      is LeftBracket, is Brackets -> Annotation.Value.Array(parseArray().bind())
      is LeftCurly -> Annotation.Value.Dict(parseDict().bind())
      LiteralString -> Annotation.Value.Single(token.value.removeSurrounding("\""))
      else -> Annotation.Value.Single(token.value)
  }
  ```

- **Consume a token whose type you know with `expect<T>()`,** not with `eatToken()` after a type check. It says what
  the parser expects, and when the input has another token, it reports that one:

  ```kotlin
  is Brackets -> expect<Brackets>().bind().let { emptyList() }
  ```

- **Reject a token the parser doesn't expect with `raiseWrongToken<T>()`** in the `else` branch, with the type it
  expected: `else -> raiseWrongToken<WirespecType>().bind()`.
