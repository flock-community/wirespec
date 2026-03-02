# Rust Shared Code Pipeline Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Replace the raw string template in `RustIrEmitter.shared` with `AstShared.convert() -> transform -> generateRust()` pipeline.

**Architecture:** Use the common IR converter output as the base, apply Rust-specific transforms (unwrap namespace, replace elements that need Rust syntax the IR can't express, inject Rust-only traits), and generate via RustGenerator. Elements that the generator handles correctly stay as IR nodes; elements needing Rust-specific syntax (type bounds, associated types, `#[default]`) become `RawElement`.

**Tech Stack:** Kotlin multiplatform, IR Transform DSL, RustGenerator

---

### Task 1: Run existing tests to establish baseline

**Files:**
- Test: `src/compiler/emitters/rust/src/commonTest/kotlin/community/flock/wirespec/emitters/rust/RustIrEmitterTest.kt`

**Step 1: Run all Rust emitter tests**

Run: `./gradlew :src:compiler:emitters:rust:jvmTest`
Expected: All tests PASS (this is our baseline)

**Step 2: Commit** (nothing to commit, just verification)

---

### Task 2: Replace the shared property with pipeline skeleton

**Files:**
- Modify: `src/compiler/emitters/rust/src/commonMain/kotlin/community/flock/wirespec/emitters/rust/RustIrEmitter.kt:82-215`

**Step 1: Add imports**

Add these imports to RustIrEmitter.kt (some may already exist):

```kotlin
import community.flock.wirespec.compiler.core.parse.ast.Shared as AstShared
import community.flock.wirespec.ir.core.File as LanguageFile
import community.flock.wirespec.ir.core.Interface
import community.flock.wirespec.ir.core.Namespace
import community.flock.wirespec.ir.core.RawElement
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.generator.generateRust
```

**Step 2: Replace the shared property**

Replace lines 82-215 (the entire `override val shared = object : Shared { ... }`) with a pipeline-based implementation. The key challenge: `SharedWirespec.convert()` wraps everything in a `Namespace("Wirespec")` which RustGenerator renders as `pub mod Wirespec { ... }`. But Rust shared output should be flat (no module wrapper). Also, it includes a `Package` element we don't need.

Strategy: Convert, then extract elements from the namespace, apply transforms, and wrap in a new File.

```kotlin
override val shared = object : Shared {
    override val packageString = "shared"

    override val source: String
        get() {
            val converted = AstShared(packageString).convert()
            val namespace = converted.findElement<Namespace>()!!
            val elements = namespace.elements

            // Build a flat file with imports + transformed elements + injected traits
            val rustFile = LanguageFile(
                name = Name.of("Wirespec"),
                elements = listOf(
                    RawElement("use std::any::TypeId;"),
                    RawElement("use std::collections::HashMap;"),
                ) + elements
            )

            return rustFile
                .transform {
                    // Transforms will be added in subsequent tasks
                }
                .generateRust()
        }
}
```

**Step 3: Run tests to see what the generator produces vs expected**

Run: `./gradlew :src:compiler:emitters:rust:jvmTest --tests "*.sharedOutputTest"`
Expected: FAIL — output will differ from expected. Examine the diff to understand what transforms are needed.

**Step 4: Commit**

```bash
git add src/compiler/emitters/rust/src/commonMain/kotlin/community/flock/wirespec/emitters/rust/RustIrEmitter.kt
git commit -m "feat: scaffold pipeline-based shared property for RustIrEmitter"
```

---

### Task 3: Add transforms for elements the generator handles incorrectly

Based on analysis, these IR elements need transforms because the RustGenerator output doesn't match expected:

**Files:**
- Modify: `src/compiler/emitters/rust/src/commonMain/kotlin/community/flock/wirespec/emitters/rust/RustIrEmitter.kt`

**Step 1: Replace Enum interface with RawElement**

The IR `interface Enum { field("label", string) }` generates `pub trait Enum { fn label(&self) -> String; }` but we need `pub trait Enum: Sized { fn label(&self) -> &str; fn from_label(s: &str) -> Option<Self>; }`.

Add to the transform block:

```kotlin
matchingElements { iface: Interface ->
    when (iface.name.pascalCase()) {
        "Enum" -> RawElement(
            """
            |pub trait Enum: Sized {
            |    fn label(&self) -> &str;
            |    fn from_label(s: &str) -> Option<Self>;
            |}
            """.trimMargin()
        )
        else -> iface
    }
}
```

**Step 2: Replace Method enum with RawElement**

The IR `enum Method { GET, PUT, ... }` generates without `#[default]` attribute and without `Default` derive. Replace:

```kotlin
matchingElements { enum: LanguageEnum ->
    when (enum.name.pascalCase()) {
        "Method" -> RawElement(
            """
            |#[derive(Debug, Clone, Default, PartialEq)]
            |pub enum Method {
            |    #[default]
            |    GET,
            |    PUT,
            |    POST,
            |    DELETE,
            |    OPTIONS,
            |    HEAD,
            |    PATCH,
            |    TRACE,
            |}
            """.trimMargin()
        )
        else -> enum
    }
}
```

**Step 3: Run tests**

Run: `./gradlew :src:compiler:emitters:rust:jvmTest --tests "*.sharedOutputTest"`
Expected: Still FAIL but closer to expected. Check diff for remaining issues.

**Step 4: Commit**

```bash
git add src/compiler/emitters/rust/src/commonMain/kotlin/community/flock/wirespec/emitters/rust/RustIrEmitter.kt
git commit -m "feat: add Enum and Method transforms for Rust shared pipeline"
```

---

### Task 4: Replace serialization and complex interfaces with RawElement

**Files:**
- Modify: `src/compiler/emitters/rust/src/commonMain/kotlin/community/flock/wirespec/emitters/rust/RustIrEmitter.kt`

**Step 1: Replace interfaces needing Rust-specific type bounds**

These interfaces have Rust-specific generic bounds (`T: 'static`, `T: Display`, `T: FromStr where ...`), `&T`/`&[u8]`/`&str` argument types, and `r#type` keyword escaping that the IR and generator can't express. Replace them with RawElement in the `matchingElements<Interface>` handler:

```kotlin
matchingElements { iface: Interface ->
    when (iface.name.pascalCase()) {
        "Enum" -> RawElement(/* ... from Task 3 ... */)
        "Request" -> RawElement(
            """
            |pub trait Request<T> {
            |    fn path(&self) -> &dyn Path;
            |    fn method(&self) -> &Method;
            |    fn queries(&self) -> &dyn Queries;
            |    fn headers(&self) -> &dyn RequestHeaders;
            |    fn body(&self) -> &T;
            |}
            """.trimMargin()
        )
        "Response" -> RawElement(
            """
            |pub trait Response<T> {
            |    fn status(&self) -> i32;
            |    fn headers(&self) -> &dyn ResponseHeaders;
            |    fn body(&self) -> &T;
            |}
            """.trimMargin()
        )
        "BodySerializer" -> RawElement(
            """
            |pub trait BodySerializer {
            |    fn serialize_body<T: 'static>(&self, t: &T, r#type: TypeId) -> Vec<u8>;
            |}
            """.trimMargin()
        )
        "BodyDeserializer" -> RawElement(
            """
            |pub trait BodyDeserializer {
            |    fn deserialize_body<T: 'static>(&self, raw: &[u8], r#type: TypeId) -> T;
            |}
            """.trimMargin()
        )
        "PathSerializer" -> RawElement(
            """
            |pub trait PathSerializer {
            |    fn serialize_path<T: std::fmt::Display>(&self, t: &T, r#type: TypeId) -> String;
            |}
            """.trimMargin()
        )
        "PathDeserializer" -> RawElement(
            """
            |pub trait PathDeserializer {
            |    fn deserialize_path<T: std::str::FromStr>(&self, raw: &str, r#type: TypeId) -> T where T::Err: std::fmt::Debug;
            |}
            """.trimMargin()
        )
        "ParamSerializer" -> RawElement(
            """
            |pub trait ParamSerializer {
            |    fn serialize_param<T: 'static>(&self, value: &T, r#type: TypeId) -> Vec<String>;
            |}
            """.trimMargin()
        )
        "ParamDeserializer" -> RawElement(
            """
            |pub trait ParamDeserializer {
            |    fn deserialize_param<T: 'static>(&self, values: &[String], r#type: TypeId) -> T;
            |}
            """.trimMargin()
        )
        "Transportation" -> RawElement(
            """
            |pub trait Transportation {
            |    fn transport(&self, request: &RawRequest) -> RawResponse;
            |}
            """.trimMargin()
        )
        else -> iface
    }
}
```

**Step 2: Run tests**

Run: `./gradlew :src:compiler:emitters:rust:jvmTest --tests "*.sharedOutputTest"`
Expected: Closer to passing. Check remaining diff.

**Step 3: Commit**

```bash
git add src/compiler/emitters/rust/src/commonMain/kotlin/community/flock/wirespec/emitters/rust/RustIrEmitter.kt
git commit -m "feat: replace serialization and complex interfaces with RawElement in Rust shared"
```

---

### Task 5: Fix struct derives and handle Refined trait

**Files:**
- Modify: `src/compiler/emitters/rust/src/commonMain/kotlin/community/flock/wirespec/emitters/rust/RustIrEmitter.kt`

**Step 1: Fix RawRequest/RawResponse struct derives**

RustGenerator adds `Default` to all struct derives: `#[derive(Debug, Clone, Default, PartialEq)]`. But the expected output for RawRequest/RawResponse has `#[derive(Debug, Clone, PartialEq)]` (no Default). Replace these structs:

```kotlin
matchingElements { struct: Struct ->
    when (struct.name.pascalCase()) {
        "RawRequest", "RawResponse" -> RawElement(
            when (struct.name.pascalCase()) {
                "RawRequest" -> """
                    |#[derive(Debug, Clone, PartialEq)]
                    |pub struct RawRequest {
                    |    pub method: String,
                    |    pub path: Vec<String>,
                    |    pub queries: HashMap<String, Vec<String>>,
                    |    pub headers: HashMap<String, Vec<String>>,
                    |    pub body: Option<Vec<u8>>,
                    |}
                """.trimMargin()
                else -> """
                    |#[derive(Debug, Clone, PartialEq)]
                    |pub struct RawResponse {
                    |    pub status_code: i32,
                    |    pub headers: HashMap<String, Vec<String>>,
                    |    pub body: Option<Vec<u8>>,
                    |}
                """.trimMargin()
            }
        )
        else -> struct
    }
}
```

**Step 2: Fix Refined trait**

The IR generates `fn value(&self) -> T;` but expected is `fn value(&self) -> &T;` (reference). Add to the Interface match:

```kotlin
"Refined" -> RawElement(
    """
    |pub trait Refined<T> {
    |    fn value(&self) -> &T;
    |    fn validate(&self) -> bool;
    |}
    """.trimMargin()
)
```

**Step 3: Run tests**

Run: `./gradlew :src:compiler:emitters:rust:jvmTest --tests "*.sharedOutputTest"`
Expected: Closer. Check for remaining diffs (likely Request/Response nested Headers, injected traits).

**Step 4: Commit**

```bash
git add src/compiler/emitters/rust/src/commonMain/kotlin/community/flock/wirespec/emitters/rust/RustIrEmitter.kt
git commit -m "feat: fix struct derives and Refined trait for Rust shared pipeline"
```

---

### Task 6: Inject Rust-only traits (RequestHeaders, ResponseHeaders, Client, Server)

**Files:**
- Modify: `src/compiler/emitters/rust/src/commonMain/kotlin/community/flock/wirespec/emitters/rust/RustIrEmitter.kt`

**Step 1: Build list of Rust-only elements**

These need to be injected at the right positions. The expected output order is:
1. Request trait
2. **RequestHeaders: Headers** (injected after Request)
3. Response trait
4. **ResponseHeaders: Headers** (injected after Response)
5. ... serialization traits ...
6. RawRequest, RawResponse structs
7. Transportation trait
8. **Client** trait (injected after Transportation)
9. **Server** trait (injected after Client)

Strategy: Use `injectAfter` on specific interfaces, or build the complete element list manually after extracting from the namespace.

Since we're already extracting elements from the namespace and building a flat file, it's simpler to build the element list with injections at the right positions. Modify the shared property to iterate through namespace elements and insert at the right spots:

```kotlin
override val source: String
    get() {
        val converted = AstShared(packageString).convert()
        val namespace = converted.findElement<Namespace>()!!

        val transformedElements = buildList {
            add(RawElement("use std::any::TypeId;"))
            add(RawElement("use std::collections::HashMap;"))

            for (element in namespace.elements) {
                add(element)
                // Inject after specific elements
                when {
                    element is Interface && element.name.pascalCase() == "Request" ->
                        add(RawElement("pub trait RequestHeaders: Headers {}"))
                    element is Interface && element.name.pascalCase() == "Response" ->
                        add(RawElement("pub trait ResponseHeaders: Headers {}"))
                }
            }

            // Inject at end
            add(RawElement("""
                |pub trait Client {
                |    type Transport: Transportation;
                |    type Ser: Serialization;
                |    fn transport(&self) -> &Self::Transport;
                |    fn serialization(&self) -> &Self::Ser;
                |}
            """.trimMargin()))
            add(RawElement("""
                |pub trait Server {
                |    type Req;
                |    type Res;
                |    fn path_template(&self) -> &'static str;
                |    fn method(&self) -> Method;
                |}
            """.trimMargin()))
        }

        return LanguageFile(Name.of("Wirespec"), transformedElements)
            .transform {
                // ... all the matchingElements transforms from Tasks 3-5 ...
            }
            .generateRust()
    }
```

**Step 2: Handle Request/Response nested Headers removal**

The IR nests `interface Headers` inside Request and Response. These need to be removed since we're injecting RequestHeaders/ResponseHeaders separately. Add to the Interface transform to strip nested elements from Request/Response (or the RawElement replacement already handles this since we replace the entire Request/Response interface).

Since Request and Response are already replaced with RawElement (Task 4), the nested Headers are already gone.

**Step 3: Run tests**

Run: `./gradlew :src:compiler:emitters:rust:jvmTest --tests "*.sharedOutputTest"`
Expected: Should be very close or PASS now.

**Step 4: Commit**

```bash
git add src/compiler/emitters/rust/src/commonMain/kotlin/community/flock/wirespec/emitters/rust/RustIrEmitter.kt
git commit -m "feat: inject Rust-only traits (Client, Server, RequestHeaders, ResponseHeaders)"
```

---

### Task 7: Fix formatting and pass sharedOutputTest

**Files:**
- Modify: `src/compiler/emitters/rust/src/commonMain/kotlin/community/flock/wirespec/emitters/rust/RustIrEmitter.kt`
- Possibly modify: `src/compiler/emitters/rust/src/commonTest/kotlin/community/flock/wirespec/emitters/rust/RustIrEmitterTest.kt`

**Step 1: Run the shared test and compare output**

Run: `./gradlew :src:compiler:emitters:rust:jvmTest --tests "*.sharedOutputTest"`

Examine the diff between actual and expected output. Common formatting issues:
- Extra blank lines between elements
- Missing/extra newlines
- Indentation differences
- Element ordering

**Step 2: Fix any remaining formatting issues**

Adjust the RawElement strings and element ordering to match expected output. If minor formatting differences exist that don't affect semantics, update the test expectation instead (since we agreed "semantically equivalent" is sufficient).

**Step 3: Run the shared test**

Run: `./gradlew :src:compiler:emitters:rust:jvmTest --tests "*.sharedOutputTest"`
Expected: PASS

**Step 4: Commit**

```bash
git add src/compiler/emitters/rust/src/commonMain/kotlin/community/flock/wirespec/emitters/rust/RustIrEmitter.kt
git commit -m "fix: adjust formatting to pass sharedOutputTest"
```

---

### Task 8: Run full test suite and verify

**Files:**
- Test: all Rust emitter tests

**Step 1: Run all Rust emitter tests**

Run: `./gradlew :src:compiler:emitters:rust:jvmTest`
Expected: All tests PASS

**Step 2: Run all emitter tests (ensure no regression)**

Run: `./gradlew :src:compiler:emitters:rust:allTests`
Expected: All tests PASS

**Step 3: Commit final state**

```bash
git add -A
git commit -m "refactor: replace Rust shared raw template with AstShared.convert() pipeline"
```

---

## Summary of Changes

| File | Change |
|------|--------|
| `RustIrEmitter.kt` | Replace `shared` property: raw template -> `AstShared.convert().transform{}.generateRust()` |
| `RustIrEmitterTest.kt` | Possibly update expected output for minor formatting differences |

## Key Design Decisions

1. **Elements that stay as IR nodes** (generator handles correctly): Model, Endpoint, Channel, Path, Queries, Headers, Handler, BodySerialization, PathSerialization, ParamSerialization, Serializer, Deserializer, Serialization (all empty traits or traits with only `extends`)
2. **Elements replaced with RawElement** (generator can't express Rust-specific syntax): Enum, Refined, Method, Request, Response, BodySerializer, BodyDeserializer, PathSerializer, PathDeserializer, ParamSerializer, ParamDeserializer, Transportation, RawRequest, RawResponse
3. **Elements injected** (not in common IR): RequestHeaders, ResponseHeaders, Client, Server
