# Rust Shared Code Pipeline Design

## Goal

Replace the raw string template in `RustIrEmitter.shared` (lines 82-215) with the standard pipeline: `AstShared.convert() -> transform -> generateRust()`, matching how Kotlin, Java, TypeScript, Python, and Scala emitters handle their shared code.

## Current State

The Rust emitter's shared code is a ~130-line raw Rust string template that defines all Wirespec traits, structs, and enums. Every other language emitter uses the IR pipeline instead:

```kotlin
// Other emitters (Kotlin, Java, etc.)
AstShared(packageString).convert().transform { ... }.generateXxx()

// Rust (current)
override val source = """
    |pub trait Model { ... }
    |pub trait Enum: Sized { ... }
    |...
""".trimMargin()
```

## Architecture

```
AstShared(packageString)
    .convert()           // Produces language-neutral IR (interfaces, enums, structs)
    .transform { ... }   // Rust-specific adaptations
    .generateRust()      // RustGenerator produces Rust source code
```

## Transforms Required

### A. Element Transforms

1. **Enum interface** - Replace with Rust-specific version. The IR defines `field("label", string)` but Rust needs `fn label(&self) -> &str`, `fn from_label(s: &str) -> Option<Self>`, and `Sized` bound. Use `RawElement` for the replacement since these constructs aren't expressible in the IR.

2. **Method enum default** - The IR `enum Method { GET, ... }` needs `#[default]` on GET and `Default` in the derive. The RustGenerator adds `#[derive(Debug, Clone, Default, PartialEq)]` for structs but may need a transform or `RawElement` for the `#[default]` attribute on a variant.

3. **Transportation async** - The IR marks `transport` as `asyncFunction`. The Rust shared template uses synchronous `fn`. Strip the async flag via transform.

4. **Request/Response nested Headers** - The IR nests `interface Headers` inside both `Request` and `Response`. The Rust template has separate top-level `RequestHeaders: Headers` and `ResponseHeaders: Headers` traits. Extract and rename via transform.

5. **Serialization type bounds** - Functions like `serialize_body`, `deserialize_path` need Rust-specific generic bounds (`T: 'static`, `T: std::fmt::Display`, `T: std::str::FromStr`). These aren't in the IR. Apply via `RawElement` replacement or type parameter transforms on affected interfaces.

### B. Injected Elements (via `injectAfter`)

These Rust-only traits don't exist in the common IR:

- `RequestHeaders: Headers` - marker trait
- `ResponseHeaders: Headers` - marker trait
- `Client` trait - with associated types `Transport: Transportation` and `Ser: Serialization`
- `Server` trait - with `path_template() -> &'static str` and `method() -> Method`

Inject these after the `Wirespec` namespace using `injectAfter<Namespace>`, as `RawElement` blocks (associated types and specific Rust syntax can't be expressed in the IR DSL).

### C. Type Mapping

No custom type transforms needed. `RustGenerator` already handles:
- `Reflect -> std::any::TypeId`
- `Bytes -> Vec<u8>`
- `Dict -> std::collections::HashMap`
- `Array -> Vec`
- `Nullable -> Option`
- `String -> String`, `Boolean -> bool`, `Integer -> i32`

## Implementation Strategy

```kotlin
override val shared = object : Shared {
    override val packageString = "shared"

    private val rustOnlyTraits = buildList {
        // RequestHeaders, ResponseHeaders, Client, Server
        // as RawElement or IR DSL constructs
    }

    override val source = AstShared(packageString)
        .convert()
        .transform {
            // 1. Replace Enum interface with Rust-specific version
            matchingElements { iface: Interface ->
                when (iface.name) {
                    Name.of("Enum") -> RawElement(rustEnumTrait)
                    else -> iface
                }
            }

            // 2. Strip async from Transportation.transport
            matchingElements { fn: Function ->
                if (fn.isAsync) fn.copy(isAsync = false) else fn
            }

            // 3. Handle serialization type bounds
            // Replace affected interfaces with RawElement versions

            // 4. Handle Request/Response nested Headers
            // Extract and rename

            // 5. Inject Rust-only traits
            injectAfter { namespace: Namespace ->
                if (namespace.name == Name.of("Wirespec")) rustOnlyTraits
                else emptyList()
            }
        }
        .generateRust()
}
```

## Output Requirements

- Semantically equivalent to the current raw template
- Must compile as valid Rust
- Minor formatting differences acceptable
- Existing tests pass: `./gradlew :src:compiler:emitters:rust:allTests`

## Files Changed

- `src/compiler/emitters/rust/src/commonMain/kotlin/.../RustIrEmitter.kt` - Replace `shared` property
- Possibly `src/compiler/ir/src/commonMain/kotlin/.../generator/RustGenerator.kt` - If generator needs minor fixes for shared code edge cases

## Testing

- Run existing Rust emitter tests as validation baseline
- Compare generated shared output with the current template for semantic equivalence
- Verify the Rust petstore example still compiles
