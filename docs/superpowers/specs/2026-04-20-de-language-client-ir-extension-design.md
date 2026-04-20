# De-language ClientIrExtension — Design

**Date:** 2026-04-20
**Status:** Draft
**Scope:** Remove all language-specific transformations from `JavaClientIrExtension`, `PythonClientIrExtension`, and `ScalaClientIrExtension`. Each lands at the same minimal shape as `KotlinClientIrExtension`: convert → sanitize → file wrap. Rust and TypeScript are out of scope (they bypass the IR tree).

## Motivation

Today the per-language client extensions carry six semantic transforms beyond `sanitizeNames` + file packaging:

| Language | Transform | Purpose |
|---|---|---|
| Java | `transformTypeDescriptors` | Render `TypeDescriptor` as `Wirespec.getType(...)` |
| Java | thenApply body rewrite | Compose awaited transport + continuation as a `.thenApply(...)` chain |
| Python | `addSelfReceiverToClientFields` | Prefix struct-field access with `self.` |
| Python | `snakeCaseClientFunctions` | Snake-case function names + their call sites; add `self` parameter |
| Python | `flattenEndpointTypeRefs` | Strip `EndpointName.` prefix from nested type references |
| Scala | `addIdentityTypeToCall` | Add `[A] =>> A` identity-effect generic to `*.Call` interfaces |

Each transform exists because the neutral IR is missing a concept the language needs. Adding the right neutral concepts (and moving the rest into generators) lets `ClientIrExtension` per language be paper-thin and structurally identical.

## Approach

Hybrid:
- **DSL additions** for concepts that are genuinely neutral semantic intent (every typed/OO language has them, but renders them differently).
- **Generator-side rendering** for language idioms (Java's `Wirespec.getType`, Java's `.thenApply` chaining, Python's `self` parameter injection, Scala's identity-effect generic).
- **Sanitization extension** for naming-case (Python snake-case function names).

The convert functions stay neutral — no language hints — and the `XxxClientIrExtension` classes lose every transform except `sanitizeNames` and file wrapping.

## DSL additions

### 1. `ThisExpression`

A new `Statement, Expression` data object in `ir.core.Ast`:

```kotlin
data object ThisExpression : Statement, Expression
```

Represents an implicit reference to the enclosing instance. Used as `FieldCall.receiver` when accessing struct members. The neutral converter emits this explicitly; generators render per-language.

After the change, `FieldCall.receiver` carries clearer semantics:
- `receiver = ThisExpression` — implicit instance member access.
- `receiver = null` — ambient/free-standing symbol (no implicit receiver).
- `receiver = SomeExpression` — explicit receiver.

Generator rendering:
- Kotlin, Scala: omit (bare member access)
- Python, Rust: `self`
- Java, TypeScript: omit (bare in class body); explicit `this` only if disambiguation is needed

### 2. Structured `Type.Custom` name

`Type.Custom.name` changes from `String` to `Name`:

```kotlin
data class Custom(val name: Name, val generics: List<Type> = emptyList()) : Type
```

Multi-part type names use `Name("EndpointName", "Request")` instead of `Type.Custom("EndpointName.Request")`.

A new helper formalizes dotted joins:

```kotlin
fun Name.dotted(): String = parts.joinToString(".")
```

Generator rendering:
- Kotlin, Scala, Java, TypeScript: `name.dotted()`
- Python: `name.parts.last()` — the qualifier is assumed star-imported (matches the `from ..endpoint.X import *` Python already emits)
- Rust: snake-case per segment, joined with `::`

This is a wide mechanical refactor: every `Type.Custom("...")` construction site updates. `IrConverter`, every `XxxIrEmitter`, every `XxxClientIrExtension`, all test fixtures (`IrConverterTest`, `VerifyClientTest`, `VerifyArbitraryTest`, ...). The compiler enforces the migration — missing call sites are compile errors.

### 3. `SanitizationConfig.functionNameCase` + `FunctionCall.name` sanitization

`SanitizationConfig` gains a `functionNameCase: (Name) -> Name = { it }` hook. `sanitizeNames` extends its `statementAndExpression` branch with a `FunctionCall` case:

```kotlin
stmt is FunctionCall -> stmt.copy(
    name = config.functionNameCase(stmt.name),
).transformChildren(tr)
```

Function declarations also gain a sanitization step (today only field/parameter names are sanitized at the declaration site; functions are not). The same `functionNameCase` applies.

Python sets `functionNameCase = { Name.of(it.value().snakeCase()) }`; other languages leave the default identity.

## Generator-side changes (no further DSL impact)

### Java

- `JavaGenerator` renders `TypeDescriptor(t)` as `Wirespec.getType(rootClass.class, containerClass.class)` directly. The pure helpers in `JavaTypeDescriptorTransform.kt` (`Type.findRoot`, `Type.rawContainerClass`, `Type.toJavaName`) survive as private utilities the generator calls. The pre-render `transformTypeDescriptors` step disappears.
- `JavaGenerator.generateFunction` for `isAsync = true` functions detects a specific body shape and emits a `.thenApply(raw -> continuation)` chain instead of sequential statements:

  ```
  Pattern: <prefix statements> + Assignment(name=v, value=FunctionCall(isAwait=true)) + ReturnStatement(expr referencing v)
  Emit:    <prefix statements> + return <FunctionCall>.thenApply(v -> <expr>)
  ```

  Anything not matching the pattern falls back to sequential rendering. Worth one inline comment on the matcher to prevent surprise.

### Python

- `PythonGenerator.generateFunction` for a function nested inside a `Struct` prepends a `self` parameter to the rendered parameter list (without mutating the IR). Every method on a struct gets `self` automatically.
- `PythonGenerator` renders `Type.Custom(Name(parts...))` as `parts.last()`. Combined with the `from ..endpoint.X import *` declaration the extension already emits, qualified references resolve naturally.
- The current `Call`/`Handler` "kept qualified" exception in `flattenEndpointTypeRefs` becomes the same default — they're referenced by their last segment and the star-import brings them in scope.

### Scala

- `ScalaGenerator.generateStruct` inspects each entry of `struct.interfaces`. For any `Type.Custom` whose `name.parts.last() == "Call"` and whose `generics` is empty, the generator auto-injects the identity-effect generic `Type.Custom(Name("[A] =>> A"))`. The current `addIdentityTypeToCall` transform disappears.

  This is safe because servers don't directly implement `*.Call` in struct interfaces today (servers wire up `Handler` via `injectHandleFunction`, which is outside the client extension).

## Convert-side changes

`convertEndpointClient` and `convertClient` in `ir.extensions.ClientIrExtension.kt`:
- `FieldCall(field = X)` becomes `FieldCall(receiver = ThisExpression, field = X)` for accesses to client struct fields (`serialization`, `transportation`).
- `Type.Custom("$endpointNameStr.Call")` becomes `Type.Custom(Name(endpointNameStr, "Call"))`. Same for `Request`, `Response`, etc.
- The `FunctionCall.name` for calls like `"$endpointNameStr.toRawRequest"` becomes `Name(endpointNameStr, "toRawRequest")`. Generators render dotted as before; Python's `functionNameCase` snake-cases each part.

## ClientIrExtension impact

| Language | Today | After |
|---|---|---|
| Kotlin | `convert + sanitize + wrap` | unchanged |
| Java | `convert + sanitize + transformTypeDescriptors + thenApply + wrap` | `convert + sanitize + wrap` |
| Python | `convert + sanitize + addSelf + snakeCase + flatten + wrap` | `convert + sanitize + wrap` |
| Scala | `convert + sanitize + addIdentity + wrap` | `convert + sanitize + wrap` |
| Rust, TypeScript | raw-string emission (out of scope) | unchanged |

The four in-scope `XxxClientIrExtension` classes become structurally identical — only their constructor parameters and file-wrapping logic differ.

## Risks and trade-offs

1. **Type.Custom refactor is the large change.** Mechanical but wide. The compiler enforces correctness.
2. **`null` vs. `ThisExpression` semantics.** The neutral converter must consistently emit `ThisExpression` for implicit-self access. Other converters in the codebase (not just `convertEndpointClient`) may also need to switch from `receiver = null` — audit during implementation.
3. **Java thenApply pattern detection.** Narrow rule (only fires for `isAsync = true` with the specific assign-then-return body produced by `convertEndpointClient`). Anything else falls back to sequential rendering. Inline comment on the rule.
4. **Python last-segment Type rendering** changes how *all* qualified types render in Python — not just client code. Audit needed to confirm no Python emission today depends on dotted qualification.
5. **Scala auto-inject identity generic** assumes any struct implementing `*.Call` is a client. Today true; verified during implementation.
6. **No new tests.** Existing `VerifyUtil` compile-and-run integration tests across all six languages already exercise the generated source — they catch any output drift. The expected diff in generated source is **zero**: this is a pure refactor.

## Success criteria

- `ThisExpression` exists in `ir.core.Ast` and is rendered correctly by all six generators.
- `Type.Custom.name: Name` everywhere; `Name.dotted()` helper exists.
- `SanitizationConfig.functionNameCase` exists; `sanitizeNames` visits `FunctionCall.name` and function declarations.
- `convertEndpointClient` / `convertClient` use `ThisExpression` and structured `Name`s.
- `JavaIrEmitter` no longer references `transformTypeDescriptors`.
- `JavaClientIrExtension`, `PythonClientIrExtension`, `ScalaClientIrExtension` no longer contain any of the listed semantic transforms.
- All four in-scope client extensions (Kotlin, Java, Python, Scala) are structurally identical: `convert → sanitize → wrap`.
- Existing JVM tests pass.
- `make all` succeeds.
- Generated source under `examples/` is byte-identical to today's output.
