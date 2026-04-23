# Generator Emitter Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Emit a per-model `XxxGenerator` file in every target language (Java, Kotlin, Python, Rust, Scala, TypeScript) that produces test data by delegating every leaf value to a user-supplied `Wirespec.Generator` callback. Adds a new `GeneratorField` sealed hierarchy and `Generator` functional interface to the shared Wirespec runtime.

**Architecture:** Build on the existing IR DSL. Three pieces: (1) shared-runtime additions to `SharedWirespec.convert()` in `IrConverter.kt`; (2) a new `GeneratorConverter.kt` that turns each parser-AST `Model` into an IR `File` containing a `generate` function; (3) an `emitGenerator` hook on `IrEmitter` overridden by each language emitter, routing output to a `generator/` subpackage. A new `ClassReference(type: Type)` IR expression renders as each language's native class-reference idiom (`Address::class`, `Address.class`, `Address`, `classOf[Address]`, `TypeId::of::<Address>()`).

**Tech Stack:** Kotlin Multiplatform, Gradle, kotest/JUnit, Docker-based verify infrastructure.

**Reference spec:** `docs/superpowers/specs/2026-04-23-generator-emitter-design.md`.

---

## File Structure

**New files:**
- `src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/converter/GeneratorConverter.kt` — IR builder for generator functions.
- `src/compiler/ir/src/commonTest/kotlin/community/flock/wirespec/ir/converter/GeneratorConverterTest.kt` — unit tests.
- `src/compiler/test/src/commonMain/kotlin/community/flock/wirespec/compiler/test/CompileGeneratorTest.kt` — verify fixture.
- `src/verify/src/test/kotlin/community/flock/wirespec/verify/VerifyGeneratorTest.kt` — Docker cross-language verification.

**Modified files:**
- `src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/core/Ast.kt` — add `ClassReference(type: Type)` expression.
- `src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/core/Dsl.kt` — expose `classRef(type: Type)` DSL.
- `src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/core/Transform.kt` — add `transformChildren` branch for `ClassReference`.
- `src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/generator/{Java,Kotlin,Python,Rust,Scala,TypeScript}Generator.kt` — render `ClassReference`, and tweak `KotlinGenerator`'s `Type.Reflect` rendering from `KType` to `KClass<*>`.
- `src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/converter/IrConverter.kt` — extend `SharedWirespec.convert()` with `GeneratorField` + `Generator`.
- `src/compiler/ir/src/commonTest/kotlin/community/flock/wirespec/ir/converter/IrConverterTest.kt` — add `testSharedContainsGeneratorField`.
- `src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/emit/EmitHelpers.kt` — add string-subpackage overloads of `placeInPackage` / `placeInModule`.
- `src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/emit/IrEmitter.kt` — add `emitGenerator(definition, module): File?` hook and thread `generatorFiles` through `emit(module, logger)`.
- `src/compiler/emitters/{java,kotlin,python,rust,scala,typescript}/src/commonMain/kotlin/.../XxxIrEmitter.kt` — override `emitGenerator`, route to `generator/` subpackage.
- `src/compiler/emitters/{java,kotlin,python,rust,scala,typescript}/src/commonTest/kotlin/.../XxxIrEmitterTest.kt` — update shared-runtime fixture; add `testEmitGeneratorFor{Type,Enum,Union,Refined,ArrayField,DictField,NullableField}`.

---

## Phase 1 — Shared Runtime + IR Infrastructure

### Task 1: Add `ClassReference(type: Type)` IR expression

**Files:**
- Modify: `src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/core/Ast.kt`
- Modify: `src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/core/Dsl.kt`
- Modify: `src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/core/Transform.kt`

- [ ] **Step 1: Add the data class to `Ast.kt`**

Open `src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/core/Ast.kt`. Insert the new data class immediately after the `Literal` declaration (around line 266):

```kotlin
data class ClassReference(val type: Type) : Expression
```

Save. No existing line moves except by insertion.

- [ ] **Step 2: Add a DSL convenience in `Dsl.kt`**

Open `src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/core/Dsl.kt`. Inside `BaseBuilder` (after the `literal(value: Double)` overload around line 31), add:

```kotlin
    fun classRef(type: Type): ClassReference = ClassReference(type)
    fun classRef(typeName: String): ClassReference = ClassReference(Type.Custom(typeName))
```

- [ ] **Step 3: Add a `transformChildren` branch in `Transform.kt`**

Open `src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/core/Transform.kt`. Find the `fun Expression.transformChildren(...)` function (grep for `fun Expression.transformChildren`). Add a `when` arm:

```kotlin
        is ClassReference -> copy(type = transformer.transformType(type))
```

Place it in alphabetical order alongside the other expression arms.

- [ ] **Step 4: Build to verify**

Run: `./gradlew :src:compiler:ir:compileCommonMainKotlinMetadata`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/core/Ast.kt \
        src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/core/Dsl.kt \
        src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/core/Transform.kt
git commit -m "$(cat <<'EOF'
feat(ir): add ClassReference expression for language-native class refs

Renders as Address::class / Address.class / Address / classOf[Address] /
TypeId::of::<Address>() per language (generators updated in follow-ups).

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 2: Render `ClassReference` in each language generator

Each language's `XxxGenerator.kt` has a single `Expression.emit()` or equivalent `when` switch. Add a `ClassReference` arm to all six.

**Files:**
- Modify: `src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/generator/JavaGenerator.kt`
- Modify: `src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/generator/KotlinGenerator.kt`
- Modify: `src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/generator/PythonGenerator.kt`
- Modify: `src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/generator/RustGenerator.kt`
- Modify: `src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/generator/ScalaGenerator.kt`
- Modify: `src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/generator/TypeScriptGenerator.kt`

- [ ] **Step 1: Java generator**

Locate the `when (this)` block that handles expressions in `JavaGenerator.kt` (grep for `is Literal ->` in that file to find it). Add:

```kotlin
is ClassReference -> "${type.emitGenerics()}.class"
```

Use `type.emitGenerics()` so generics are rendered when the referenced type has them (consistent with how `ConstructorStatement` handles types).

- [ ] **Step 2: Kotlin generator — render `ClassReference` and retype `Type.Reflect`**

In `KotlinGenerator.kt`:

a. Find the `Expression` rendering `when` block and add:
```kotlin
is ClassReference -> "${type.emitGenerics()}::class"
```

b. Change line 344 from:
```kotlin
        Type.Reflect -> "KType"
```
to:
```kotlin
        Type.Reflect -> "KClass<*>"
```

c. In `KotlinIrEmitter.kt`, update the wirespec imports (currently at line 73-76):
```kotlin
    private val wirespecImports = listOf(
        import("$DEFAULT_SHARED_PACKAGE_STRING.kotlin", "Wirespec"),
        import("kotlin.reflect", "typeOf"),
    )
```
to also import `KClass`:
```kotlin
    private val wirespecImports = listOf(
        import("$DEFAULT_SHARED_PACKAGE_STRING.kotlin", "Wirespec"),
        import("kotlin.reflect", "typeOf"),
        import("kotlin.reflect", "KClass"),
    )
```

d. Update the shared-runtime emit block around line 112 to inject `KClass` alongside `KType`:
```kotlin
                    file.copy(elements = packageElements + import("kotlin.reflect", "KType") + import("kotlin.reflect", "KClass") + rest)
```

- [ ] **Step 3: Python generator**

In `PythonGenerator.kt`, add to the `Expression` rendering:
```kotlin
is ClassReference -> type.emitGenerics()
```
(Python uses the bare class name as its class reference — no suffix needed.)

- [ ] **Step 4: Rust generator**

In `RustGenerator.kt`, add:
```kotlin
is ClassReference -> "std::any::TypeId::of::<${type.emitGenerics()}>()"
```

- [ ] **Step 5: Scala generator**

In `ScalaGenerator.kt`, add:
```kotlin
is ClassReference -> "classOf[${type.emitGenerics()}]"
```

- [ ] **Step 6: TypeScript generator**

In `TypeScriptGenerator.kt`, add:
```kotlin
is ClassReference -> type.emitGenerics()
```
(TypeScript passes the class constructor directly.)

- [ ] **Step 7: Build**

Run: `./gradlew :src:compiler:ir:compileCommonMainKotlinMetadata`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 8: Commit**

```bash
git add src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/generator/*Generator.kt \
        src/compiler/emitters/kotlin/src/commonMain/kotlin/community/flock/wirespec/emitters/kotlin/KotlinIrEmitter.kt
git commit -m "feat(ir): render ClassReference per-language; retype Kotlin Type.Reflect to KClass<*>"
```

---

### Task 3: Add `GeneratorField` hierarchy + `Generator` interface to shared runtime

**Files:**
- Modify: `src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/converter/IrConverter.kt:61-207`

- [ ] **Step 1: Extend `SharedWirespec.convert()`**

Open `IrConverter.kt`. Inside the `namespace("Wirespec") { … }` block (starts at line 64), after the last `interface`("Transportation") block (lines 200-205) but still inside the `Wirespec` namespace, append:

```kotlin
        `interface`("GeneratorField", isSealed = true) {
            typeParam(type("T"), Type.Nullable(Type.Any))
        }
        struct("GeneratorFieldString") {
            implements(type("GeneratorField", string))
            field("regex", string.nullable())
        }
        struct("GeneratorFieldInteger") {
            implements(type("GeneratorField", integer64))
            field("min", integer64.nullable())
            field("max", integer64.nullable())
        }
        struct("GeneratorFieldNumber") {
            implements(type("GeneratorField", number64))
            field("min", number64.nullable())
            field("max", number64.nullable())
        }
        struct("GeneratorFieldBoolean") {
            implements(type("GeneratorField", boolean))
        }
        struct("GeneratorFieldBytes") {
            implements(type("GeneratorField", bytes))
        }
        struct("GeneratorFieldEnum") {
            implements(type("GeneratorField", string))
            field("values", list(string))
        }
        struct("GeneratorFieldUnion") {
            implements(type("GeneratorField", string))
            field("variants", list(string))
        }
        struct("GeneratorFieldArray") {
            implements(type("GeneratorField", integer64))
            field("inner", type("GeneratorField", Type.Wildcard).nullable())
        }
        struct("GeneratorFieldNullable") {
            implements(type("GeneratorField", boolean))
            field("inner", type("GeneratorField", Type.Wildcard).nullable())
        }
        struct("GeneratorFieldDict") {
            implements(type("GeneratorField", integer64))
            field("key", type("GeneratorField", Type.Wildcard).nullable())
            field("value", type("GeneratorField", Type.Wildcard).nullable())
        }
        `interface`("Generator") {
            function("generate") {
                typeParam(type("T"), Type.Nullable(Type.Any))
                returnType(type("T"))
                arg("path", list(string))
                arg("type", reflect)
                arg("field", type("GeneratorField", type("T")))
            }
        }
```

- [ ] **Step 2: Build the IR module**

Run: `./gradlew :src:compiler:ir:compileCommonMainKotlinMetadata`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/converter/IrConverter.kt
git commit -m "feat(ir): add GeneratorField hierarchy and Generator interface to shared runtime"
```

---

### Task 4: Add `IrConverterTest.testSharedContainsGeneratorField`

**Files:**
- Modify: `src/compiler/ir/src/commonTest/kotlin/community/flock/wirespec/ir/converter/IrConverterTest.kt`

- [ ] **Step 1: Write the failing test**

Open `IrConverterTest.kt`. Append a new `@Test` method at the end of the class:

```kotlin
    @Test
    fun testSharedContainsGeneratorField() {
        val file = community.flock.wirespec.compiler.core.parse.ast.Shared("com.example").convert()
        val wirespecNamespace = file.elements
            .filterIsInstance<community.flock.wirespec.ir.core.Namespace>()
            .first { it.name == community.flock.wirespec.ir.core.Name.of("Wirespec") }
        val interfaces = wirespecNamespace.elements
            .filterIsInstance<community.flock.wirespec.ir.core.Interface>()
        val structs = wirespecNamespace.elements
            .filterIsInstance<community.flock.wirespec.ir.core.Struct>()

        val generatorField = interfaces.first { it.name == community.flock.wirespec.ir.core.Name.of("GeneratorField") }
        assertTrue(generatorField.isSealed, "GeneratorField should be sealed")

        val generator = interfaces.first { it.name == community.flock.wirespec.ir.core.Name.of("Generator") }
        val generateFn = generator.elements
            .filterIsInstance<community.flock.wirespec.ir.core.Function>()
            .first { it.name == community.flock.wirespec.ir.core.Name.of("generate") }
        assertEquals(3, generateFn.parameters.size, "generate() should take path, type, and field")
        assertEquals(community.flock.wirespec.ir.core.Type.Reflect, generateFn.parameters[1].type, "second param must be Type.Reflect")

        val expectedVariants = setOf(
            "GeneratorFieldString", "GeneratorFieldInteger", "GeneratorFieldNumber",
            "GeneratorFieldBoolean", "GeneratorFieldBytes", "GeneratorFieldEnum",
            "GeneratorFieldUnion", "GeneratorFieldArray", "GeneratorFieldNullable",
            "GeneratorFieldDict",
        )
        val actualVariants = structs.map { it.name.value() }.toSet()
        assertTrue(expectedVariants.all { it in actualVariants },
            "Missing variants: ${expectedVariants - actualVariants}")
    }
```

- [ ] **Step 2: Run the test**

Run: `./gradlew :src:compiler:ir:jvmTest --tests "community.flock.wirespec.ir.converter.IrConverterTest.testSharedContainsGeneratorField"`
Expected: PASS.

- [ ] **Step 3: Run the full IR test module**

Run: `./gradlew :src:compiler:ir:jvmTest`
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add src/compiler/ir/src/commonTest/kotlin/community/flock/wirespec/ir/converter/IrConverterTest.kt
git commit -m "test(ir): assert shared runtime contains GeneratorField hierarchy and Generator"
```

---

### Task 5: Update each language's `XxxIrEmitterTest` shared-runtime fixture

The shared Wirespec runtime now emits 10 additional structs + 2 additional interfaces. Every `XxxIrEmitterTest` that asserts on the shared runtime block will fail until its expected output is extended.

**Files:**
- Modify: `src/compiler/emitters/java/src/commonTest/kotlin/community/flock/wirespec/emitters/java/JavaIrEmitterTest.kt`
- Modify: `src/compiler/emitters/kotlin/src/commonTest/kotlin/community/flock/wirespec/emitters/kotlin/KotlinIrEmitterTest.kt`
- Modify: `src/compiler/emitters/python/src/commonTest/kotlin/community/flock/wirespec/emitters/python/PythonIrEmitterTest.kt`
- Modify: `src/compiler/emitters/rust/src/commonTest/kotlin/community/flock/wirespec/emitters/rust/RustIrEmitterTest.kt`
- Modify: `src/compiler/emitters/scala/src/commonTest/kotlin/community/flock/wirespec/emitters/scala/ScalaIrEmitterTest.kt`
- Modify: `src/compiler/emitters/typescript/src/commonTest/kotlin/community/flock/wirespec/emitters/typescript/TypeScriptIrEmitterTest.kt`

- [ ] **Step 1: Run all six emitter tests to capture the actual output**

Run: `./gradlew :src:compiler:emitters:java:jvmTest :src:compiler:emitters:kotlin:jvmTest :src:compiler:emitters:python:jvmTest :src:compiler:emitters:rust:jvmTest :src:compiler:emitters:scala:jvmTest :src:compiler:emitters:typescript:jvmTest --continue`
Expected: FAIL. For each failing test, the assertion-failure diff shows the actual emitted shared block. Use that as the source of truth for the fixture updates.

- [ ] **Step 2: Per emitter, replace the expected shared-runtime string with the actual output**

For each of the six `XxxIrEmitterTest.kt` files, locate the test whose expected string contains `interface Wirespec` / `public interface Wirespec` / `pub trait Wirespec` / etc. (grep for `interface Wirespec` inside each test file). Paste the actual output from Step 1's diff into the expected-value multiline string. Indentation and whitespace must match exactly.

- [ ] **Step 3: Re-run each test to confirm it passes**

Run the six test commands individually (or together with `--continue`). Expected: PASS.

- [ ] **Step 4: Commit each language as its own commit**

```bash
git add src/compiler/emitters/java/src/commonTest/kotlin/community/flock/wirespec/emitters/java/JavaIrEmitterTest.kt
git commit -m "test(java): extend shared-runtime fixture for GeneratorField hierarchy"

git add src/compiler/emitters/kotlin/src/commonTest/kotlin/community/flock/wirespec/emitters/kotlin/KotlinIrEmitterTest.kt
git commit -m "test(kotlin): extend shared-runtime fixture for GeneratorField hierarchy"

git add src/compiler/emitters/python/src/commonTest/kotlin/community/flock/wirespec/emitters/python/PythonIrEmitterTest.kt
git commit -m "test(python): extend shared-runtime fixture for GeneratorField hierarchy"

git add src/compiler/emitters/rust/src/commonTest/kotlin/community/flock/wirespec/emitters/rust/RustIrEmitterTest.kt
git commit -m "test(rust): extend shared-runtime fixture for GeneratorField hierarchy"

git add src/compiler/emitters/scala/src/commonTest/kotlin/community/flock/wirespec/emitters/scala/ScalaIrEmitterTest.kt
git commit -m "test(scala): extend shared-runtime fixture for GeneratorField hierarchy"

git add src/compiler/emitters/typescript/src/commonTest/kotlin/community/flock/wirespec/emitters/typescript/TypeScriptIrEmitterTest.kt
git commit -m "test(typescript): extend shared-runtime fixture for GeneratorField hierarchy"
```

---

## Phase 2 — `GeneratorConverter.kt`

### Task 6: Add `GeneratorConverter.kt` with helper for primitive-to-field descriptors

**Files:**
- Create: `src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/converter/GeneratorConverter.kt`

- [ ] **Step 1: Write the initial file skeleton with helpers only**

Create `GeneratorConverter.kt` with exactly this content:

```kotlin
package community.flock.wirespec.ir.converter

import community.flock.wirespec.compiler.core.parse.ast.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.ast.FieldIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Identifier
import community.flock.wirespec.ir.core.BinaryOp
import community.flock.wirespec.ir.core.ClassReference
import community.flock.wirespec.ir.core.ConstructorStatement
import community.flock.wirespec.ir.core.Expression
import community.flock.wirespec.ir.core.File
import community.flock.wirespec.ir.core.FunctionCall
import community.flock.wirespec.ir.core.Literal
import community.flock.wirespec.ir.core.LiteralList
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.NullLiteral
import community.flock.wirespec.ir.core.RawExpression
import community.flock.wirespec.ir.core.Type
import community.flock.wirespec.ir.core.VariableReference
import community.flock.wirespec.ir.core.file
import community.flock.wirespec.compiler.core.parse.ast.Enum as EnumWirespec
import community.flock.wirespec.compiler.core.parse.ast.Reference as ReferenceWirespec
import community.flock.wirespec.compiler.core.parse.ast.Refined as RefinedWirespec
import community.flock.wirespec.compiler.core.parse.ast.Type as TypeWirespec
import community.flock.wirespec.compiler.core.parse.ast.Union as UnionWirespec

private fun Identifier.toGeneratorName(): Name = when (this) {
    is FieldIdentifier -> {
        val parts = value.split(Regex("[.\\s-]+")).filter { it.isNotEmpty() }
        Name(parts + "Generator")
    }
    is DefinitionIdentifier -> Name(
        Name.of(value).parts.filter { part -> part.any { it.isLetterOrDigit() } } + "Generator",
    )
}

private fun FieldIdentifier.toFieldName(): Name {
    val parts = value.split(Regex("[.\\s-]+")).filter { it.isNotEmpty() }
    return Name(parts)
}

private fun pathPlus(segment: String): BinaryOp =
    BinaryOp(
        VariableReference(Name.of("path")),
        BinaryOp.Operator.PLUS,
        Literal(segment, Type.String),
    )

private fun ReferenceWirespec.Primitive.toFieldDescriptor(): Expression = when (val t = type) {
    is ReferenceWirespec.Primitive.Type.String -> {
        val constraint = t.constraint
        ConstructorStatement(
            type = Type.Custom("Wirespec.GeneratorFieldString"),
            namedArguments = mapOf(
                Name.of("regex") to if (constraint != null) {
                    Literal(
                        constraint.value.split("/").drop(1).dropLast(1).joinToString("/"),
                        Type.String,
                    )
                } else NullLiteral,
            ),
        )
    }
    is ReferenceWirespec.Primitive.Type.Integer -> ConstructorStatement(
        type = Type.Custom("Wirespec.GeneratorFieldInteger"),
        namedArguments = mapOf(
            Name.of("min") to (t.constraint?.min?.let { Literal(it.toLong(), Type.Integer()) } ?: NullLiteral),
            Name.of("max") to (t.constraint?.max?.let { Literal(it.toLong(), Type.Integer()) } ?: NullLiteral),
        ),
    )
    is ReferenceWirespec.Primitive.Type.Number -> ConstructorStatement(
        type = Type.Custom("Wirespec.GeneratorFieldNumber"),
        namedArguments = mapOf(
            Name.of("min") to (t.constraint?.min?.let { Literal(it.toDouble(), Type.Number()) } ?: NullLiteral),
            Name.of("max") to (t.constraint?.max?.let { Literal(it.toDouble(), Type.Number()) } ?: NullLiteral),
        ),
    )
    ReferenceWirespec.Primitive.Type.Boolean -> ConstructorStatement(
        type = Type.Custom("Wirespec.GeneratorFieldBoolean"),
    )
    ReferenceWirespec.Primitive.Type.Bytes -> ConstructorStatement(
        type = Type.Custom("Wirespec.GeneratorFieldBytes"),
    )
}

private fun ReferenceWirespec.toFieldDescriptorOrNull(): Expression = when (this) {
    is ReferenceWirespec.Primitive -> toFieldDescriptor()
    else -> NullLiteral
}

private fun generatorCallExpression(
    typeName: String,
    fieldNameStr: String,
    fieldDescriptor: Expression,
): FunctionCall = FunctionCall(
    receiver = VariableReference(Name.of("generator")),
    name = Name.of("generate"),
    arguments = mapOf(
        Name.of("path") to pathPlus(fieldNameStr),
        Name.of("type") to ClassReference(Type.Custom(typeName)),
        Name.of("field") to fieldDescriptor,
    ),
)
```

(NullLiteral, BinaryOp.Operator.PLUS — adjust import path if your core's `NullLiteral` lives in `ir.core`. Grep `src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/core` for `object NullLiteral` to confirm. `BinaryOp.Operator.PLUS` lives on the `BinaryOp` companion — grep `BinaryOp` in Ast.kt to confirm the enum-variant name.)

- [ ] **Step 2: Build**

Run: `./gradlew :src:compiler:ir:compileCommonMainKotlinMetadata`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/converter/GeneratorConverter.kt
git commit -m "feat(ir): add GeneratorConverter skeleton with field-descriptor helpers"
```

---

### Task 7: Implement `TypeWirespec.convertToGenerator()`

**Files:**
- Modify: `src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/converter/GeneratorConverter.kt`

- [ ] **Step 1: Add the `Type` entry point and `toGeneratorExpression` helper**

Append to `GeneratorConverter.kt`:

```kotlin
fun TypeWirespec.convertToGenerator(): File {
    val generatorName = identifier.toGeneratorName()
    val typeName = identifier.value

    return file(generatorName) {
        namespace(generatorName) {
            function("generate") {
                arg("path", list(string))
                arg("generator", type("Wirespec.Generator"))
                returnType(type(typeName))
                returns(
                    ConstructorStatement(
                        type = Type.Custom(typeName),
                        namedArguments = shape.value.associate { field ->
                            val fieldName = field.identifier.toFieldName()
                            fieldName to field.reference.toGeneratorExpression(typeName, field.identifier.value)
                        },
                    ),
                )
            }
        }
    }
}

private fun ReferenceWirespec.toGeneratorExpression(typeName: String, fieldNameStr: String): Expression = when (val ref = this) {
    is ReferenceWirespec.Primitive -> generatorCallExpression(typeName, fieldNameStr, ref.toFieldDescriptor())
    is ReferenceWirespec.Custom -> FunctionCall(
        receiver = RawExpression("${ref.value}Generator"),
        name = Name.of("generate"),
        arguments = mapOf(
            Name.of("path") to pathPlus(fieldNameStr),
            Name.of("generator") to VariableReference(Name.of("generator")),
        ),
    )
    is ReferenceWirespec.Iterable -> generatorCallExpression(
        typeName,
        fieldNameStr,
        ConstructorStatement(
            type = Type.Custom("Wirespec.GeneratorFieldArray"),
            namedArguments = mapOf(Name.of("inner") to ref.reference.toFieldDescriptorOrNull()),
        ),
    )
    is ReferenceWirespec.Dict -> generatorCallExpression(
        typeName,
        fieldNameStr,
        ConstructorStatement(
            type = Type.Custom("Wirespec.GeneratorFieldDict"),
            namedArguments = mapOf(
                Name.of("key") to NullLiteral,
                Name.of("value") to ref.reference.toFieldDescriptorOrNull(),
            ),
        ),
    )
    is ReferenceWirespec.Any, is ReferenceWirespec.Unit -> NullLiteral
}
```

Note: this implementation does not yet handle `isNullable = true` at the reference level (nullable wrapping is deferred to a later task to keep the first green). All fields are treated as if non-nullable at this point.

- [ ] **Step 2: Build**

Run: `./gradlew :src:compiler:ir:compileCommonMainKotlinMetadata`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/converter/GeneratorConverter.kt
git commit -m "feat(ir): implement TypeWirespec.convertToGenerator (non-nullable fields)"
```

---

### Task 8: Handle nullable references by wrapping in `GeneratorFieldNullable`

**Files:**
- Modify: `src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/converter/GeneratorConverter.kt`

- [ ] **Step 1: Look up how nullability is represented on `ReferenceWirespec`**

Run: `grep -n "isNullable\|data class Nullable\|class Primitive\|class Custom\|class Iterable\|class Dict" src/compiler/core/src/commonMain/kotlin/community/flock/wirespec/compiler/core/parse/ast/Reference.kt`
Expected: a field `isNullable: Boolean` on each reference variant. Confirm.

- [ ] **Step 2: Wrap nullable emissions**

Edit `toGeneratorExpression` so the non-Any/Unit branches test `ref.isNullable`:

```kotlin
private fun ReferenceWirespec.toGeneratorExpression(typeName: String, fieldNameStr: String): Expression {
    val nonNullDescriptor: Expression? = when (val ref = this) {
        is ReferenceWirespec.Primitive -> ref.toFieldDescriptor()
        is ReferenceWirespec.Iterable -> ConstructorStatement(
            type = Type.Custom("Wirespec.GeneratorFieldArray"),
            namedArguments = mapOf(Name.of("inner") to ref.reference.toFieldDescriptorOrNull()),
        )
        is ReferenceWirespec.Dict -> ConstructorStatement(
            type = Type.Custom("Wirespec.GeneratorFieldDict"),
            namedArguments = mapOf(
                Name.of("key") to NullLiteral,
                Name.of("value") to ref.reference.toFieldDescriptorOrNull(),
            ),
        )
        is ReferenceWirespec.Custom -> null // handled below
        is ReferenceWirespec.Any, is ReferenceWirespec.Unit -> null
    }

    return when (val ref = this) {
        is ReferenceWirespec.Custom -> {
            val inner = FunctionCall(
                receiver = RawExpression("${ref.value}Generator"),
                name = Name.of("generate"),
                arguments = mapOf(
                    Name.of("path") to pathPlus(fieldNameStr),
                    Name.of("generator") to VariableReference(Name.of("generator")),
                ),
            )
            if (ref.isNullable) {
                // Ask the callback whether to emit null; if false, invoke the sub-generator.
                // Emitted as a conditional raw expression so per-language generators render naturally.
                RawExpression(
                    "if (generator.generate(path + \"${fieldNameStr}\", ${ref.value}::class, Wirespec.GeneratorFieldNullable(inner = null))) null else ${ref.value}Generator.generate(path + \"${fieldNameStr}\", generator)",
                )
            } else inner
        }
        is ReferenceWirespec.Any, is ReferenceWirespec.Unit -> NullLiteral
        else -> {
            val descriptor = nonNullDescriptor ?: NullLiteral
            if (this.isNullable) {
                generatorCallExpression(
                    typeName,
                    fieldNameStr,
                    ConstructorStatement(
                        type = Type.Custom("Wirespec.GeneratorFieldNullable"),
                        namedArguments = mapOf(Name.of("inner") to descriptor),
                    ),
                )
            } else generatorCallExpression(typeName, fieldNameStr, descriptor)
        }
    }
}
```

Note: the `RawExpression` branch for nullable-custom references is a pragmatic fallback — it renders uniformly in Kotlin but may need per-language adjustment in Task 13+ if snapshot tests show language-syntax mismatches. If a snapshot test fails for a non-Kotlin language, swap the `RawExpression` for a proper `IfExpression` IR node (see `IfExpression` in `Ast.kt`).

- [ ] **Step 3: Build**

Run: `./gradlew :src:compiler:ir:compileCommonMainKotlinMetadata`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/converter/GeneratorConverter.kt
git commit -m "feat(ir): wrap nullable field emissions in GeneratorFieldNullable"
```

---

### Task 9: Implement `EnumWirespec.convertToGenerator()`

**Files:**
- Modify: `src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/converter/GeneratorConverter.kt`

- [ ] **Step 1: Append the Enum entry point**

```kotlin
fun EnumWirespec.convertToGenerator(): File {
    val generatorName = identifier.toGeneratorName()
    val typeName = identifier.value

    return file(generatorName) {
        namespace(generatorName) {
            function("generate") {
                arg("path", list(string))
                arg("generator", type("Wirespec.Generator"))
                returnType(type(typeName))
                returns(
                    ConstructorStatement(
                        type = Type.Custom(typeName),
                        namedArguments = mapOf(
                            Name.of("label") to FunctionCall(
                                receiver = VariableReference(Name.of("generator")),
                                name = Name.of("generate"),
                                arguments = mapOf(
                                    Name.of("path") to pathPlus("value"),
                                    Name.of("type") to ClassReference(Type.Custom(typeName)),
                                    Name.of("field") to ConstructorStatement(
                                        type = Type.Custom("Wirespec.GeneratorFieldEnum"),
                                        namedArguments = mapOf(
                                            Name.of("values") to LiteralList(
                                                values = entries.map { Literal(it, Type.String) },
                                                type = Type.String,
                                            ),
                                        ),
                                    ),
                                ),
                            ),
                        ),
                    ),
                )
            }
        }
    }
}
```

- [ ] **Step 2: Build + commit**

```bash
./gradlew :src:compiler:ir:compileCommonMainKotlinMetadata
git add src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/converter/GeneratorConverter.kt
git commit -m "feat(ir): implement EnumWirespec.convertToGenerator"
```

---

### Task 10: Implement `RefinedWirespec.convertToGenerator()`

**Files:**
- Modify: `src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/converter/GeneratorConverter.kt`

- [ ] **Step 1: Append the Refined entry point**

```kotlin
fun RefinedWirespec.convertToGenerator(): File {
    val generatorName = identifier.toGeneratorName()
    val typeName = identifier.value

    return file(generatorName) {
        namespace(generatorName) {
            function("generate") {
                arg("path", list(string))
                arg("generator", type("Wirespec.Generator"))
                returnType(type(typeName))
                returns(
                    ConstructorStatement(
                        type = Type.Custom(typeName),
                        namedArguments = mapOf(
                            Name.of("value") to FunctionCall(
                                receiver = VariableReference(Name.of("generator")),
                                name = Name.of("generate"),
                                arguments = mapOf(
                                    Name.of("path") to pathPlus("value"),
                                    Name.of("type") to ClassReference(Type.Custom(typeName)),
                                    Name.of("field") to reference.toFieldDescriptor(),
                                ),
                            ),
                        ),
                    ),
                )
            }
        }
    }
}
```

- [ ] **Step 2: Build + commit**

```bash
./gradlew :src:compiler:ir:compileCommonMainKotlinMetadata
git add src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/converter/GeneratorConverter.kt
git commit -m "feat(ir): implement RefinedWirespec.convertToGenerator"
```

---

### Task 11: Implement `UnionWirespec.convertToGenerator()`

**Files:**
- Modify: `src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/converter/GeneratorConverter.kt`

- [ ] **Step 1: Append the Union entry point**

```kotlin
fun UnionWirespec.convertToGenerator(): File {
    val generatorName = identifier.toGeneratorName()
    val typeName = identifier.value
    val variantNames = entries.filterIsInstance<ReferenceWirespec.Custom>().map { it.value }

    return file(generatorName) {
        namespace(generatorName) {
            function("generate") {
                arg("path", list(string))
                arg("generator", type("Wirespec.Generator"))
                returnType(type(typeName))
                assign(
                    "variant",
                    FunctionCall(
                        receiver = VariableReference(Name.of("generator")),
                        name = Name.of("generate"),
                        arguments = mapOf(
                            Name.of("path") to pathPlus("variant"),
                            Name.of("type") to ClassReference(Type.Custom(typeName)),
                            Name.of("field") to ConstructorStatement(
                                type = Type.Custom("Wirespec.GeneratorFieldUnion"),
                                namedArguments = mapOf(
                                    Name.of("variants") to LiteralList(
                                        values = variantNames.map { Literal(it, Type.String) },
                                        type = Type.String,
                                    ),
                                ),
                            ),
                        ),
                    ),
                )
                switch(VariableReference(Name.of("variant"))) {
                    for (variantName in variantNames) {
                        case(Literal(variantName, Type.String)) {
                            returns(
                                FunctionCall(
                                    receiver = RawExpression("${variantName}Generator"),
                                    name = Name.of("generate"),
                                    arguments = mapOf(
                                        Name.of("path") to BinaryOp(
                                            VariableReference(Name.of("path")),
                                            BinaryOp.Operator.PLUS,
                                            Literal(variantName, Type.String),
                                        ),
                                        Name.of("generator") to VariableReference(Name.of("generator")),
                                    ),
                                ),
                            )
                        }
                    }
                }
                error(Literal("Unknown variant", Type.String))
            }
        }
    }
}
```

- [ ] **Step 2: Build + commit**

```bash
./gradlew :src:compiler:ir:compileCommonMainKotlinMetadata
git add src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/converter/GeneratorConverter.kt
git commit -m "feat(ir): implement UnionWirespec.convertToGenerator with switch dispatch"
```

---

### Task 12: Add `GeneratorConverterTest`

**Files:**
- Create: `src/compiler/ir/src/commonTest/kotlin/community/flock/wirespec/ir/converter/GeneratorConverterTest.kt`

- [ ] **Step 1: Write the test file**

Create the file with this content:

```kotlin
package community.flock.wirespec.ir.converter

import community.flock.wirespec.compiler.core.parse.ast.*
import community.flock.wirespec.compiler.core.parse.ast.Enum
import community.flock.wirespec.compiler.core.parse.ast.Type as TypeWirespec
import community.flock.wirespec.compiler.core.parse.ast.Union as UnionWirespec
import community.flock.wirespec.ir.core.*
import community.flock.wirespec.ir.core.Type
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GeneratorConverterTest {

    private fun definitionId(name: String) = DefinitionIdentifier(name)
    private fun fieldId(name: String) = FieldIdentifier(name)

    @Test
    fun testTypeConvertToGeneratorProducesNamespace() {
        val address = TypeWirespec(
            comment = null, annotations = emptyList(),
            identifier = definitionId("Address"),
            shape = TypeWirespec.Shape(
                listOf(
                    Field(emptyList(), fieldId("street"),
                        Reference.Primitive(false, Reference.Primitive.Type.String(null))),
                    Field(emptyList(), fieldId("number"),
                        Reference.Primitive(false, Reference.Primitive.Type.Integer(null))),
                ),
            ),
            extends = emptyList(),
        )

        val file = address.convertToGenerator()
        val namespace = file.findElement<Namespace>()!!
        assertEquals(Name.of("AddressGenerator"), namespace.name)

        val fn = namespace.findElement<Function>()!!
        assertEquals(Name.of("generate"), fn.name)
        assertEquals(2, fn.parameters.size)
    }

    @Test
    fun testTypeConvertToGeneratorEmitsClassReferenceAsType() {
        val address = TypeWirespec(
            comment = null, annotations = emptyList(),
            identifier = definitionId("Address"),
            shape = TypeWirespec.Shape(
                listOf(
                    Field(emptyList(), fieldId("street"),
                        Reference.Primitive(false, Reference.Primitive.Type.String(null))),
                ),
            ),
            extends = emptyList(),
        )

        val file = address.convertToGenerator()
        val classRefs = file.findAll<ClassReference>()
        assertTrue(classRefs.any { it.type == Type.Custom("Address") },
            "expected a ClassReference(Type.Custom(\"Address\")) in the generated body")
    }

    @Test
    fun testEnumConvertToGenerator() {
        val color = Enum(
            comment = null, annotations = emptyList(),
            identifier = definitionId("Color"),
            entries = setOf("RED", "GREEN", "BLUE"),
        )
        val file = color.convertToGenerator()
        val namespace = file.findElement<Namespace>()!!
        assertEquals(Name.of("ColorGenerator"), namespace.name)
        val classRefs = file.findAll<ClassReference>()
        assertTrue(classRefs.any { it.type == Type.Custom("Color") })
    }

    @Test
    fun testRefinedConvertToGeneratorWithRegex() {
        val uuid = Refined(
            comment = null, annotations = emptyList(),
            identifier = definitionId("UUID"),
            reference = Reference.Primitive(
                isNullable = false,
                type = Reference.Primitive.Type.String(
                    Reference.Primitive.Type.Constraint.RegEx("/^[0-9a-f]{8}$/g"),
                ),
            ),
        )
        val file = uuid.convertToGenerator()
        val classRefs = file.findAll<ClassReference>()
        assertTrue(classRefs.any { it.type == Type.Custom("UUID") })
        val literals = file.findAll<Literal>()
        assertTrue(
            literals.any { it.value == "^[0-9a-f]{8}$" },
            "Refined should emit regex literal stripped of slashes and flags",
        )
    }

    @Test
    fun testUnionConvertToGeneratorHasSwitch() {
        val shape = UnionWirespec(
            comment = null, annotations = emptyList(),
            identifier = definitionId("Shape"),
            entries = setOf(
                Reference.Custom(isNullable = false, value = "Circle"),
                Reference.Custom(isNullable = false, value = "Square"),
            ),
        )
        val file = shape.convertToGenerator()
        val switches = file.findAll<Switch>()
        assertEquals(1, switches.size, "Union generator must contain one switch")
        assertEquals(2, switches[0].cases.size)
    }
}
```

- [ ] **Step 2: Run the tests**

Run: `./gradlew :src:compiler:ir:jvmTest --tests "community.flock.wirespec.ir.converter.GeneratorConverterTest"`
Expected: PASS.

- [ ] **Step 3: Confirm nothing else regressed**

Run: `./gradlew :src:compiler:ir:jvmTest`
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add src/compiler/ir/src/commonTest/kotlin/community/flock/wirespec/ir/converter/GeneratorConverterTest.kt
git commit -m "test(ir): add GeneratorConverterTest for Type/Enum/Union/Refined"
```

---

## Phase 3 — `emitGenerator` Hook + Per-Emitter Wiring

### Task 13: Add string-subpackage overloads to `EmitHelpers.kt`

**Files:**
- Modify: `src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/emit/EmitHelpers.kt`

- [ ] **Step 1: Replace the file contents**

Read the current file first (it is 42 lines, already captured earlier in this plan). Then overwrite with:

```kotlin
package community.flock.wirespec.ir.emit

import arrow.core.NonEmptyList
import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.emit.plus
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.ir.core.Element
import community.flock.wirespec.ir.core.File
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.Package

fun File.placeInPackage(
    packageName: PackageName,
    subPackage: String,
): File {
    val subPackageName = packageName + subPackage
    return File(
        name = Name.of(subPackageName.toDir() + name.pascalCase()),
        elements = listOf(Package(subPackageName.value)) + elements,
    )
}

fun File.placeInPackage(
    packageName: PackageName,
    definition: Definition,
): File = placeInPackage(packageName, definition.namespace())

fun File.prependImports(imports: List<Element>?): File = if (imports == null) {
    this
} else {
    copy(elements = imports + elements)
}

fun File.placeInModule(
    packageName: PackageName,
    subPackage: String,
): File {
    val subPackageName = packageName + subPackage
    return copy(name = Name.of(subPackageName.toDir() + name.pascalCase()))
}

fun File.placeInModule(
    packageName: PackageName,
    definition: Definition,
): File = placeInModule(packageName, definition.namespace())

fun NonEmptyList<File>.withSharedSource(
    emitShared: EmitShared,
    sharedFile: () -> File,
): NonEmptyList<File> = if (emitShared.value) this + sharedFile() else this
```

Note: if `Definition.namespace()` is not an extension already imported, add the import:
```kotlin
import community.flock.wirespec.compiler.core.emit.namespace
```
(Grep `grep -rn "fun Definition.namespace\|fun.*Definition.*namespace" src/compiler/core/` to find the exact path.)

- [ ] **Step 2: Build**

Run: `./gradlew :src:compiler:ir:compileCommonMainKotlinMetadata :src:compiler:emitters:kotlin:compileCommonMainKotlinMetadata`
Expected: BUILD SUCCESSFUL — the Definition-overloads still have identical signatures, so existing callers compile unchanged.

- [ ] **Step 3: Commit**

```bash
git add src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/emit/EmitHelpers.kt
git commit -m "feat(ir): add subPackage-string overloads to placeInPackage/placeInModule"
```

---

### Task 14: Add `emitGenerator` hook to `IrEmitter`

**Files:**
- Modify: `src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/emit/IrEmitter.kt`

- [ ] **Step 1: Add the hook and thread it through `emit(module, logger)`**

Open `IrEmitter.kt`. Locate the `emit(module: Module, logger: Logger)` method (around line 44). Replace its body and add a new default-null hook:

```kotlin
    fun emit(module: Module, logger: Logger): NonEmptyList<File> {
        val definitionFiles = module.statements.map { emit(it, module, logger) }
        val endpoints = module.statements.toList().filterIsInstance<Endpoint>()
        val clientFiles = endpoints.map { endpoint ->
            logger.info("Emitting Client for endpoint ${endpoint.identifier.value}")
            emitEndpointClient(endpoint)
        }
        val generatorFiles = module.statements.toList()
            .filterIsInstance<Model>()
            .mapNotNull { model ->
                logger.info("Emitting Generator for ${model::class.simpleName} ${model.identifier.value}")
                emitGenerator(model, module)
            }
        return definitionFiles + clientFiles + generatorFiles
    }

    fun emitGenerator(definition: Definition, module: Module): File? = null
```

Add the `Model` import to `IrEmitter.kt`:
```kotlin
import community.flock.wirespec.compiler.core.parse.ast.Model
```

- [ ] **Step 2: Build**

Run: `./gradlew :src:compiler:ir:compileCommonMainKotlinMetadata :src:compiler:ir:jvmMainClasses`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Run all IR tests to confirm no regression**

Run: `./gradlew :src:compiler:ir:jvmTest`
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/emit/IrEmitter.kt
git commit -m "feat(ir): add emitGenerator hook + thread generator files through module emit"
```

---

### Task 15: Override `emitGenerator` in KotlinIrEmitter and add snapshot tests

**Files:**
- Modify: `src/compiler/emitters/kotlin/src/commonMain/kotlin/community/flock/wirespec/emitters/kotlin/KotlinIrEmitter.kt`
- Modify: `src/compiler/emitters/kotlin/src/commonTest/kotlin/community/flock/wirespec/emitters/kotlin/KotlinIrEmitterTest.kt`

- [ ] **Step 1: Add imports and the override in `KotlinIrEmitter.kt`**

Add imports:
```kotlin
import community.flock.wirespec.ir.converter.convertToGenerator
import community.flock.wirespec.ir.emit.placeInPackage
```
(The latter may already be imported — check before adding.)

At the bottom of the class body, inside `KotlinIrEmitter`, add:

```kotlin
    override fun emitGenerator(definition: Definition, module: Module): File? {
        val generatorFile: community.flock.wirespec.ir.core.File? = when (definition) {
            is Type -> definition.convertToGenerator()
            is Enum -> definition.convertToGenerator()
            is Refined -> definition.convertToGenerator()
            is Union -> definition.convertToGenerator()
            else -> null
        } ?: return null

        return File(
            name = community.flock.wirespec.ir.core.Name.of((packageName + "generator").toDir() + generatorFile.name.pascalCase()),
            elements = listOf(LanguagePackage((packageName + "generator").value)) + wirespecImports + generatorFile.elements,
        ).let { ir ->
            // Render to Kotlin source
            File(ir.name, listOf(RawElement(generator.generate(ir))))
        }
    }
```

Wait — re-reading the existing `emit(definition, module, logger)` at line 133: it runs `super.emit(...)` and then calls `prependImports` and `placeInPackage`, then the outer `emit(module, logger)` wraps the File into `Emitted(...)` with `generator.generate(file)`. So `emitGenerator` should return the **IR File** (before source generation), and the wrapping outer `emit(module, logger)` handles the source conversion.

Replace the override with this simpler form:

```kotlin
    override fun emitGenerator(definition: Definition, module: Module): community.flock.wirespec.ir.core.File? {
        val generatorFile = when (definition) {
            is Type -> definition.convertToGenerator()
            is Enum -> definition.convertToGenerator()
            is Refined -> definition.convertToGenerator()
            is Union -> definition.convertToGenerator()
            else -> return null
        }
        return generatorFile
            .sanitizeNames(sanitizationConfig)
            .prependImports(wirespecImports)
            .placeInPackage(packageName = packageName, subPackage = "generator")
    }
```

Use the correct `File` import — whichever alias `KotlinIrEmitter.kt` already uses for `community.flock.wirespec.ir.core.File` (it is aliased as `LanguageFile` near the top of the file; use `LanguageFile` as the return type if that matches the file's conventions).

- [ ] **Step 2: Write a failing snapshot test**

In `KotlinIrEmitterTest.kt`, append:

```kotlin
    @Test
    fun testEmitGeneratorForType() {
        val emitter = KotlinIrEmitter()
        val addressModule = module(listOf(TYPE_ADDRESS))
        val result = emitter.emit(addressModule, NoopLogger).map { it.file to it.result }
        val generatorFile = result.firstOrNull { (file, _) -> file.contains("AddressGenerator") }
        assertNotNull(generatorFile, "AddressGenerator file should be emitted")
        assertEquals(
            """
            |package community.flock.wirespec.generated.generator
            |
            |import community.flock.wirespec.generated.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |import kotlin.reflect.KClass
            |
            |object AddressGenerator {
            |  fun generate(path: kotlin.collections.List<kotlin.String>, generator: Wirespec.Generator): Address = Address(street = generator.generate((path + "street"), Address::class, Wirespec.GeneratorFieldString(regex = null)))
            |}
            |
            """.trimMargin(),
            generatorFile.second,
        )
    }
```

If this fixture isn't faithful to the current `KotlinIrEmitterTest` style (import ordering, whitespace, struct helper names), adapt by copying the shape of an existing test in the same file (e.g., the `testEmitterType` at line 31). The key assertions are: generator lives in `…/generator/AddressGenerator.kt`, has `object AddressGenerator`, calls `generator.generate(...)` with `Address::class` as the second argument.

Assumes a helper `TYPE_ADDRESS` and `module(…)` exist in the test file — they already do (grep `TYPE_ADDRESS` in the file); if not, copy the fixture-constructor pattern from `testEmitterType`.

- [ ] **Step 3: Run the test — expect FAIL**

Run: `./gradlew :src:compiler:emitters:kotlin:jvmTest --tests "*testEmitGeneratorForType*"`
Expected: FAIL with a diff between the assertion and actual output.

- [ ] **Step 4: Iterate fixture until PASS**

Study the actual output in the diff and update the expected string to match (whitespace, imports, brace style). Re-run until PASS.

- [ ] **Step 5: Add tests for Enum, Union, Refined, Array, Dict, Nullable**

Copy `testEmitGeneratorForType` as a template and add:
- `testEmitGeneratorForEnum` — module with a single `Enum` definition (fixture `ENUM_COLOR` if present, else build one inline).
- `testEmitGeneratorForUnion` — module with `Union { Circle | Square }` plus the variant types.
- `testEmitGeneratorForRefined` — module with a `Refined` (UUID) whose constraint is `/^[0-9a-f]+$/g`.
- `testEmitGeneratorForArrayField` — Type with a `List<Int>` field.
- `testEmitGeneratorForDictField` — Type with a `Dict<String, Int>` field.
- `testEmitGeneratorForNullableField` — Type with a nullable primitive field.

Each test asserts the emitted `XxxGenerator.kt` file appears in `/generator/` and the body matches the expected string (derived by running once and capturing the actual output).

- [ ] **Step 6: Run all Kotlin emitter tests**

Run: `./gradlew :src:compiler:emitters:kotlin:jvmTest`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add src/compiler/emitters/kotlin/src/commonMain/kotlin/community/flock/wirespec/emitters/kotlin/KotlinIrEmitter.kt \
        src/compiler/emitters/kotlin/src/commonTest/kotlin/community/flock/wirespec/emitters/kotlin/KotlinIrEmitterTest.kt
git commit -m "feat(kotlin): emit per-model Generator files + snapshot tests"
```

---

### Task 16: Override `emitGenerator` in JavaIrEmitter and add snapshot tests

**Files:**
- Modify: `src/compiler/emitters/java/src/commonMain/kotlin/community/flock/wirespec/emitters/java/JavaIrEmitter.kt`
- Modify: `src/compiler/emitters/java/src/commonTest/kotlin/community/flock/wirespec/emitters/java/JavaIrEmitterTest.kt`

- [ ] **Step 1: Add the override**

Mirror Task 15 Step 1 but use Java's `wirespecImport` (single import literal) and the Java `sanitizationConfig`. Add `import community.flock.wirespec.ir.converter.convertToGenerator`.

```kotlin
    override fun emitGenerator(definition: Definition, module: Module): community.flock.wirespec.ir.core.File? {
        val generatorFile = when (definition) {
            is AstType -> definition.convertToGenerator()
            is Enum -> definition.convertToGenerator()
            is Refined -> definition.convertToGenerator()
            is Union -> definition.convertToGenerator()
            else -> return null
        }
        return generatorFile
            .sanitizeNames(sanitizationConfig)
            .prependImports(listOf(wirespecImport))
            .placeInPackage(packageName = packageName, subPackage = "generator")
    }
```

Use the exact type aliases already present at the top of `JavaIrEmitter.kt` (e.g. `AstType` for the parser `Type`, `LanguageFile` for the IR `File`, etc.).

- [ ] **Step 2-6: Snapshot tests**

Mirror Task 15 Steps 2–6 in `JavaIrEmitterTest.kt`. Expected Java output shapes:

```java
public final class AddressGenerator {
  public static Address generate(java.util.List<java.lang.String> path, Wirespec.Generator generator) {
    return new Address(
      generator.generate(path.plus("street"), Address.class, new Wirespec.GeneratorFieldString(java.util.Optional.empty()))
    );
  }
}
```

Adjust to match actual emitter output (`path.plus`, `Optional.empty()` vs `null`, record vs class, etc. — let the first failing test drive the expected string).

- [ ] **Step 7: Commit**

```bash
git add src/compiler/emitters/java/src/commonMain/kotlin/community/flock/wirespec/emitters/java/JavaIrEmitter.kt \
        src/compiler/emitters/java/src/commonTest/kotlin/community/flock/wirespec/emitters/java/JavaIrEmitterTest.kt
git commit -m "feat(java): emit per-model Generator files + snapshot tests"
```

---

### Task 17: Override `emitGenerator` in PythonIrEmitter + tests

**Files:**
- Modify: `src/compiler/emitters/python/src/commonMain/kotlin/community/flock/wirespec/emitters/python/PythonIrEmitter.kt`
- Modify: `src/compiler/emitters/python/src/commonTest/kotlin/community/flock/wirespec/emitters/python/PythonIrEmitterTest.kt`

- [ ] **Step 1: Override `emitGenerator`**

Python uses `placeInModule` (module-relative imports, not package declarations):

```kotlin
    override fun emitGenerator(definition: Definition, module: Module): community.flock.wirespec.ir.core.File? {
        val generatorFile = when (definition) {
            is Type -> definition.convertToGenerator()
            is Enum -> definition.convertToGenerator()
            is Refined -> definition.convertToGenerator()
            is Union -> definition.convertToGenerator()
            else -> return null
        }
        return generatorFile
            .sanitizeNames(sanitizationConfig)
            .prependImports(buildImports("..wirespec"))  // match existing Python pattern
            .placeInModule(packageName = packageName, subPackage = "generator")
    }
```

(Match existing `emit(type, module)` method's import-building call for the exact pattern.)

- [ ] **Step 2–6: Snapshot tests**

Mirror Task 15's per-definition tests but with Python's output conventions. Example `testEmitGeneratorForType` expected output (verify against actual):

```python
from .wirespec import Wirespec
from ..model.address import Address

class AddressGenerator:
    @staticmethod
    def generate(path: list[str], generator: Wirespec.Generator) -> Address:
        return Address(
            street=generator.generate(path + ["street"], Address, Wirespec.GeneratorFieldString(regex=None)),
        )
```

- [ ] **Step 7: Commit**

```bash
git add src/compiler/emitters/python/src/commonMain/kotlin/community/flock/wirespec/emitters/python/PythonIrEmitter.kt \
        src/compiler/emitters/python/src/commonTest/kotlin/community/flock/wirespec/emitters/python/PythonIrEmitterTest.kt
git commit -m "feat(python): emit per-model Generator files + snapshot tests"
```

---

### Task 18: Override `emitGenerator` in RustIrEmitter + tests

**Files:**
- Modify: `src/compiler/emitters/rust/src/commonMain/kotlin/community/flock/wirespec/emitters/rust/RustIrEmitter.kt`
- Modify: `src/compiler/emitters/rust/src/commonTest/kotlin/community/flock/wirespec/emitters/rust/RustIrEmitterTest.kt`

- [ ] **Step 1: Override `emitGenerator`**

Rust inlines the subpackage concat (as the existing `emit()` methods do). Add:

```kotlin
    override fun emitGenerator(definition: Definition, module: Module): community.flock.wirespec.ir.core.File? {
        val generatorFile = when (definition) {
            is Type -> definition.convertToGenerator()
            is Enum -> definition.convertToGenerator()
            is Refined -> definition.convertToGenerator()
            is Union -> definition.convertToGenerator()
            else -> return null
        }.sanitizeNames(sanitizationConfig)

        val subPackageName = packageName + "generator"
        return File(
            name = Name.of(subPackageName.toDir() + generatorFile.name.pascalCase().toSnakeCase()),
            elements = listOf(RawElement(modelImport)) + generatorFile.elements,
        )
    }
```

Match exact conventions (import style, `modelImport` variable name) by reading `RustIrEmitter.kt`'s existing `emit(type, module)` method.

- [ ] **Step 2–6: Snapshot tests**

Output filename is snake_case: `address_generator.rs`. Struct name is PascalCase: `pub struct AddressGenerator`. Function takes `&impl Wirespec::Generator`.

- [ ] **Step 7: Commit**

```bash
git add src/compiler/emitters/rust/src/commonMain/kotlin/community/flock/wirespec/emitters/rust/RustIrEmitter.kt \
        src/compiler/emitters/rust/src/commonTest/kotlin/community/flock/wirespec/emitters/rust/RustIrEmitterTest.kt
git commit -m "feat(rust): emit per-model Generator files + snapshot tests"
```

---

### Task 19: Override `emitGenerator` in ScalaIrEmitter + tests

**Files:**
- Modify: `src/compiler/emitters/scala/src/commonMain/kotlin/community/flock/wirespec/emitters/scala/ScalaIrEmitter.kt`
- Modify: `src/compiler/emitters/scala/src/commonTest/kotlin/community/flock/wirespec/emitters/scala/ScalaIrEmitterTest.kt`

Scala uses `placeInPackage` like Kotlin/Java. Mirror Task 15.

- [ ] **Step 1: Override**

```kotlin
    override fun emitGenerator(definition: Definition, module: Module): community.flock.wirespec.ir.core.File? {
        val generatorFile = when (definition) {
            is Type -> definition.convertToGenerator()
            is Enum -> definition.convertToGenerator()
            is Refined -> definition.convertToGenerator()
            is Union -> definition.convertToGenerator()
            else -> return null
        }
        return generatorFile
            .sanitizeNames(sanitizationConfig)
            .prependImports(listOf(RawElement(wirespecImport)))
            .placeInPackage(packageName = packageName, subPackage = "generator")
    }
```

Use the exact `wirespecImport` name from `ScalaIrEmitter.kt`.

- [ ] **Step 2–6: Snapshot tests** (Scala uses `object AddressGenerator { def generate(path: Seq[String], generator: Wirespec.Generator): Address = Address(...) }`).

- [ ] **Step 7: Commit**

```bash
git add src/compiler/emitters/scala/src/commonMain/kotlin/community/flock/wirespec/emitters/scala/ScalaIrEmitter.kt \
        src/compiler/emitters/scala/src/commonTest/kotlin/community/flock/wirespec/emitters/scala/ScalaIrEmitterTest.kt
git commit -m "feat(scala): emit per-model Generator files + snapshot tests"
```

---

### Task 20: Override `emitGenerator` in TypeScriptIrEmitter + tests

**Files:**
- Modify: `src/compiler/emitters/typescript/src/commonMain/kotlin/community/flock/wirespec/emitters/typescript/TypeScriptIrEmitter.kt`
- Modify: `src/compiler/emitters/typescript/src/commonTest/kotlin/community/flock/wirespec/emitters/typescript/TypeScriptIrEmitterTest.kt`

TypeScript inlines `PackageName("") + subPackage`. Filename is PascalCase.

- [ ] **Step 1: Override**

```kotlin
    override fun emitGenerator(definition: Definition, module: Module): community.flock.wirespec.ir.core.File? {
        val generatorFile = when (definition) {
            is AstType -> definition.convertToGenerator()
            is AstEnum -> definition.convertToGenerator()
            is Refined -> definition.convertToGenerator()
            is Union -> definition.convertToGenerator()
            else -> return null
        }.sanitizeNames(sanitizationConfig)

        val subPackageName = PackageName("") + "generator"
        return File(
            name = Name.of(subPackageName.toDir() + generatorFile.name.pascalCase().sanitizeSymbol()),
            elements = listOf(RawElement("import {Wirespec} from '../Wirespec'\n")) + generatorFile.elements,
        )
    }
```

- [ ] **Step 2–6: Snapshot tests** (TS output uses `export namespace AddressGenerator { export function generate(path: Array<string>, generator: Wirespec.Generator): Address { … } }`).

- [ ] **Step 7: Commit**

```bash
git add src/compiler/emitters/typescript/src/commonMain/kotlin/community/flock/wirespec/emitters/typescript/TypeScriptIrEmitter.kt \
        src/compiler/emitters/typescript/src/commonTest/kotlin/community/flock/wirespec/emitters/typescript/TypeScriptIrEmitterTest.kt
git commit -m "feat(typescript): emit per-model Generator files + snapshot tests"
```

---

### Task 21: Phase 3 integration — full build

- [ ] **Step 1: Clean full build**

Run: `./gradlew clean build`
Expected: BUILD SUCCESSFUL across all modules. Every language now emits `XxxGenerator.{ext}` files into `…/generator/` alongside the existing model output.

- [ ] **Step 2: Inspect generated output on disk**

Run: `find src/verify/build/generated -name "*Generator*" -not -path "*/client/*" -not -path "*/endpoint/*" | head`
Expected: a mix of `AddressGenerator.kt`, `AddressGenerator.java`, `address_generator.py`, `address_generator.rs`, `AddressGenerator.ts`, `AddressGenerator.scala` files exist under each language's `generator/` subfolder.

No commit — this is a verification step.

---

## Phase 4 — Docker Verify Tests

### Task 22: Add `CompileGeneratorTest` fixture

**Files:**
- Create: `src/compiler/test/src/commonMain/kotlin/community/flock/wirespec/compiler/test/CompileGeneratorTest.kt`

- [ ] **Step 1: Locate existing fixture patterns**

Run: `ls src/compiler/test/src/commonMain/kotlin/community/flock/wirespec/compiler/test/`
Expected: files like `CompileTypeTest.kt`, `CompileEnumTest.kt`. Read one (e.g. `cat src/compiler/test/src/commonMain/kotlin/community/flock/wirespec/compiler/test/CompileEnumTest.kt`) to learn the `object … : Fixture` structure.

- [ ] **Step 2: Create the new fixture**

```kotlin
package community.flock.wirespec.compiler.test

object CompileGeneratorTest : Fixture {
    override val source = """
        type UUID /^[0-9a-f]{8}$/g
        enum Color { RED, GREEN, BLUE }
        type Address { street: String, number: Integer, postalCode: UUID }
        type Person { name: String, age: Integer, addresses: Address[], favoriteColor: Color, nickname: String? }
    """.trimIndent()
}
```

If `Fixture` requires additional members (ast, emitter configuration), mirror those from the existing fixtures.

- [ ] **Step 3: Build the test module**

Run: `./gradlew :src:compiler:test:compileCommonMainKotlinMetadata`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add src/compiler/test/src/commonMain/kotlin/community/flock/wirespec/compiler/test/CompileGeneratorTest.kt
git commit -m "test: add CompileGeneratorTest fixture with Type/Enum/Union/Refined coverage"
```

---

### Task 23: Add `VerifyGeneratorTest` parameterized across all six languages

**Files:**
- Create: `src/verify/src/test/kotlin/community/flock/wirespec/verify/VerifyGeneratorTest.kt`

- [ ] **Step 1: Inspect existing verify tests**

Run: `ls src/verify/src/test/kotlin/community/flock/wirespec/verify/` and open one of the existing `Verify*Test.kt` files (e.g. `VerifyComplexModelTest.kt`) to see the Docker-based parameterization pattern.

- [ ] **Step 2: Create the verify test**

```kotlin
package community.flock.wirespec.verify

import community.flock.wirespec.compiler.test.CompileGeneratorTest
// + other imports mirroring a neighboring Verify*Test.kt file

class VerifyGeneratorTest {

    @ParameterizedTest
    @EnumSource(Language::class)  // use whichever enum the existing tests use
    fun `generator runtime - each language`(language: Language) {
        val source = CompileGeneratorTest.source
        // 1. compile `source` via the pipeline for `language`.
        // 2. write a small test program per language that:
        //    a. implements Wirespec.Generator with a deterministic callback
        //    b. calls PersonGenerator.generate(listOf("Person"), generator)
        //    c. asserts that the returned Person has:
        //       - name == "test-string"
        //       - age == 42
        //       - addresses.size == 2
        //       - addresses[0].street == "test-string"
        //       - addresses[0].postalCode.value matches UUID regex
        //       - favoriteColor one of RED/GREEN/BLUE
        //       - nickname either null or "test-string"
        // 3. run the compiled program in the language's Docker container via the
        //    existing `runInDocker(language, program)` helper (name may differ —
        //    check neighboring VerifyXxxTest.kt for the actual name).
        // 4. assert exit code 0 and no assertion-failure output.
    }
}
```

Mirror the exact Docker-runner helper API used by the existing verify tests. The callback in each language must dispatch on the class-reference argument — e.g. Kotlin:

```kotlin
val generator = object : Wirespec.Generator {
    override fun <T> generate(path: List<String>, type: KClass<*>, field: Wirespec.GeneratorField<T>): T = when (field) {
        is Wirespec.GeneratorFieldString -> "test-string" as T
        is Wirespec.GeneratorFieldInteger -> 42L as T
        is Wirespec.GeneratorFieldBoolean -> true as T
        is Wirespec.GeneratorFieldEnum -> field.values.first() as T
        is Wirespec.GeneratorFieldUnion -> field.variants.first() as T
        is Wirespec.GeneratorFieldArray -> 2 as T
        is Wirespec.GeneratorFieldDict -> 1 as T
        is Wirespec.GeneratorFieldNullable -> false as T
        is Wirespec.GeneratorFieldBytes -> ByteArray(0) as T
        is Wirespec.GeneratorFieldNumber -> 1.0 as T
    }
}
```

Equivalent programs in Java, Python, Scala, Rust, TypeScript follow the same pattern using their respective class-reference idioms.

- [ ] **Step 3: Run the verify test**

Run: `./gradlew :src:verify:test --tests "community.flock.wirespec.verify.VerifyGeneratorTest"`
Expected: PASS for every language. If a language fails with a compile error in the generated program, capture the error, check whether the converter emitted something the generator can't render (most likely: a nullable-custom reference), and fix the converter.

- [ ] **Step 4: Full verify suite**

Run: `./gradlew :src:verify:test`
Expected: PASS (no regressions in existing verify tests).

- [ ] **Step 5: Commit**

```bash
git add src/verify/src/test/kotlin/community/flock/wirespec/verify/VerifyGeneratorTest.kt
git commit -m "test(verify): cross-language runtime verification for Generator emitter"
```

---

### Task 24: Full tree build and cross-module integration

- [ ] **Step 1: Clean full build**

Run: `./gradlew clean build`
Expected: BUILD SUCCESSFUL across all modules, all tests green.

- [ ] **Step 2: Confirm generator output shape on disk**

Run each:
```
find src/verify/build/generated -name "AddressGenerator.*" -not -path "*/client/*"
find src/verify/build/generated -name "address_generator.*"
```
Expected: generator files exist under `/generator/` subfolders for every language.

- [ ] **Step 3: Verify no stale model-subpackage references**

Run: `grep -rn "PersonGenerator\|AddressGenerator\|ColorGenerator" src/compiler src/verify --include='*.kt' | grep -v "/generator/\|/model/\|/build/" | head`
Expected: empty or only test-fixture matches.

- [ ] **Step 4: Final commit (if any adjustments surface)**

```bash
git status
# If Steps 1–3 surface a miss, fix it, stage the file, and commit with a "fix:"-prefixed message.
# Otherwise skip this step.
```

---

## Spec Coverage Check

| Spec section | Task(s) |
|---|---|
| Shared runtime: `GeneratorField` hierarchy | Task 3 |
| Shared runtime: `Generator` interface with `type: KClass<*>` parameter | Task 3 + Task 2 Step 2 (Kotlin `Type.Reflect` → `KClass<*>`) |
| `ClassReference` expression across languages | Tasks 1, 2 |
| `GeneratorConverter.kt` — Type/Enum/Union/Refined entry points | Tasks 6–11 |
| Array / Dict / Nullable field handling | Tasks 7 (Array, Dict non-null) + 8 (Nullable wrapping) |
| `EmitHelpers` string-subpackage overloads | Task 13 |
| `emitGenerator` hook on `IrEmitter` | Task 14 |
| Per-language emitter overrides + snapshot tests (×6) | Tasks 15–20 |
| IR-level converter unit tests | Tasks 4, 12 |
| Docker verify tests across all six languages | Tasks 22–23 |
| Shared-runtime fixture update per emitter | Task 5 |
| File output in `generator/` subpackage (all languages) | Tasks 15–20 + Task 21 |
