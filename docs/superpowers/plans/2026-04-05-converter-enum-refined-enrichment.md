# Converter Enum/Refined Enrichment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move enum entry string values and refined toString generation from emitters into IrConverter, eliminating duplicated transforms in Python, Rust, Java, Kotlin, and Scala emitters.

**Architecture:** Two changes to `IrConverter.kt` — enum entries get quoted string values, refined structs get a toString function. Then simplify each emitter that previously added these manually. `withLabelField()` in `Dsl.kt` is simplified to only sanitize entry names (values already present).

**Tech Stack:** Kotlin Multiplatform, Gradle

---

### Task 1: Add enum entry string values in converter

**Files:**
- Modify: `src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/converter/IrConverter.kt:409-413`
- Test: `src/compiler/ir/src/commonTest/kotlin/community/flock/wirespec/ir/converter/IrConverterTest.kt`

- [ ] **Step 1: Update the enum converter test to expect entry values**

In `IrConverterTest.kt`, update the `testEnumConversion` test. Change the expected entries from `entry("FOO")` / `entry("BAR")` to include string values:

```kotlin
@Test
fun testEnumConversion() {
    val source = """
        enum MyEnum {
            FOO, BAR
        }
    """.trimIndent()

    val result = parse<AstEnum>(source).convert()

    val expected = file("MyEnum") {
        enum("MyEnum", Type.Custom("Wirespec.Enum")) {
            entry("FOO", "\"FOO\"")
            entry("BAR", "\"BAR\"")
        }
    }

    assertEquals(expected, result)
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :src:compiler:ir:allTests --tests "community.flock.wirespec.ir.converter.IrConverterTest.testEnumConversion"`
Expected: FAIL — entries have empty values but test expects `"FOO"` and `"BAR"`.

- [ ] **Step 3: Update the converter to add entry values**

In `IrConverter.kt` line 411, change:
```kotlin
entries.forEach { entry(it) }
```
to:
```kotlin
entries.forEach { entry(it, "\"$it\"") }
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :src:compiler:ir:allTests --tests "community.flock.wirespec.ir.converter.IrConverterTest.testEnumConversion"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/converter/IrConverter.kt src/compiler/ir/src/commonTest/kotlin/community/flock/wirespec/ir/converter/IrConverterTest.kt
git commit -m "feat: add string values to enum entries in IrConverter"
```

---

### Task 2: Add refined toString function in converter

**Files:**
- Modify: `src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/converter/IrConverter.kt:421-430`
- Test: `src/compiler/ir/src/commonTest/kotlin/community/flock/wirespec/ir/converter/IrConverterTest.kt`

- [ ] **Step 1: Update the refined converter test to expect a toString function**

In `IrConverterTest.kt`, update `testRefinedConversion` to expect a toString function after validate. Since `DutchPostalCode` refines a `String`, the toString body should be `VariableReference("value")`:

```kotlin
@Test
fun testRefinedConversion() {
    val source = """
        type DutchPostalCode = String(/^([0-9]{4}[A-Z]{2})$/g)
    """.trimIndent()

    val result = parse<AstRefined>(source).convert()

    val expected = file("DutchPostalCode") {
        struct("DutchPostalCode") {
            implements(type("Wirespec.Refined", string))
            field("value", Type.String)
            function("validate") {
                returnType(Type.Boolean)
                returns(
                    Constraint.RegexMatch(
                        pattern = "^([0-9]{4}[A-Z]{2})\$",
                        rawValue = "/^([0-9]{4}[A-Z]{2})\$/g",
                        value = VariableReference(Name.of("value")),
                    ),
                )
            }
            function("toString") {
                returnType(Type.String)
                returns(VariableReference(Name.of("value")))
            }
        }
    }

    assertEquals(expected, result)
}
```

- [ ] **Step 2: Add a test for refined Integer (non-String) toString**

Add a new test that verifies non-String refined types get `FunctionCall(value, "toString")`:

```kotlin
@Test
fun testRefinedIntegerConversion() {
    val source = """
        type Age = Integer(0, 150)
    """.trimIndent()

    val result = parse<AstRefined>(source).convert()

    val expected = file("Age") {
        struct("Age") {
            implements(type("Wirespec.Refined", Type.Integer()))
            field("value", Type.Integer())
            function("validate") {
                returnType(Type.Boolean)
                returns(
                    Constraint.BoundCheck(
                        min = "0",
                        max = "150",
                        value = VariableReference(Name.of("value")),
                    ),
                )
            }
            function("toString") {
                returnType(Type.String)
                returns(
                    FunctionCall(
                        receiver = VariableReference(Name.of("value")),
                        name = Name.of("toString"),
                    ),
                )
            }
        }
    }

    assertEquals(expected, result)
}
```

Note: You may need to add `import community.flock.wirespec.ir.core.FunctionCall` to the test file imports.

- [ ] **Step 3: Run both tests to verify they fail**

Run: `./gradlew :src:compiler:ir:allTests --tests "community.flock.wirespec.ir.converter.IrConverterTest"`
Expected: FAIL — both `testRefinedConversion` and `testRefinedIntegerConversion` fail because the converter doesn't produce a toString function.

- [ ] **Step 4: Update the converter to add toString**

In `IrConverter.kt`, replace lines 421-430:

```kotlin
fun RefinedWirespec.convert() = file(identifier.toName()) {
    struct(identifier.toName()) {
        implements(type("Wirespec.Refined", reference.convert()))
        field("value", reference.convert())
        function("validate") {
            returnType(Type.Boolean)
            returns(reference.convertConstraint(VariableReference(Name.of("value"))))
        }
    }
}
```

with:

```kotlin
fun RefinedWirespec.convert() = file(identifier.toName()) {
    struct(identifier.toName()) {
        implements(type("Wirespec.Refined", reference.convert()))
        field("value", reference.convert())
        function("validate") {
            returnType(Type.Boolean)
            returns(reference.convertConstraint(VariableReference(Name.of("value"))))
        }
        function("toString") {
            returnType(Type.String)
            returns(
                if (reference.type is ReferenceWirespec.Primitive.Type.String)
                    VariableReference(Name.of("value"))
                else
                    FunctionCall(
                        receiver = VariableReference(Name.of("value")),
                        name = Name.of("toString"),
                    )
            )
        }
    }
}
```

Make sure `FunctionCall` is imported in `IrConverter.kt`. Check existing imports — it may already be imported. If not, add:
```kotlin
import community.flock.wirespec.ir.core.FunctionCall
```

Also add at the top of the `RefinedWirespec.convert()` function scope, the import alias if not already present:
```kotlin
import community.flock.wirespec.compiler.core.parse.ast.Reference as ReferenceWirespec
```
This alias should already exist — verify in the file's import block.

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew :src:compiler:ir:allTests --tests "community.flock.wirespec.ir.converter.IrConverterTest"`
Expected: PASS — all tests including `testRefinedConversion` and `testRefinedIntegerConversion`.

- [ ] **Step 6: Commit**

```bash
git add src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/converter/IrConverter.kt src/compiler/ir/src/commonTest/kotlin/community/flock/wirespec/ir/converter/IrConverterTest.kt
git commit -m "feat: add toString function to refined types in IrConverter"
```

---

### Task 3: Simplify `withLabelField()` in Dsl.kt

**Files:**
- Modify: `src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/core/Dsl.kt:768-790`

- [ ] **Step 1: Simplify entry value handling in withLabelField**

In `Dsl.kt`, the `withLabelField()` function currently rewrites entry values on line 775:
```kotlin
entries = entries.map {
    Enum.Entry(Name.of(sanitizeEntry(it.name.value())), listOf("\"${it.name.value()}\""))
},
```

Since entry values are now set by the converter, simplify to only sanitize names. The values from `it.values` (set by converter) are preserved:
```kotlin
entries = entries.map {
    Enum.Entry(Name.of(sanitizeEntry(it.name.value())), it.values)
},
```

- [ ] **Step 2: Run all emitter tests that use withLabelField**

Run: `./gradlew :src:compiler:emitters:java:allTests :src:compiler:emitters:kotlin:allTests :src:compiler:emitters:scala:allTests`
Expected: PASS — all tests pass because values are now set by converter and preserved by withLabelField.

- [ ] **Step 3: Commit**

```bash
git add src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/core/Dsl.kt
git commit -m "refactor: simplify withLabelField to preserve converter entry values"
```

---

### Task 4: Remove enum entry value transform from Python emitter

**Files:**
- Modify: `src/compiler/emitters/python/src/commonMain/kotlin/community/flock/wirespec/emitters/python/PythonIrEmitter.kt:167-178`

- [ ] **Step 1: Simplify the Python enum emit function**

The current Python `emit(enum)` function maps entries to add quoted values and sanitize names:

```kotlin
override fun emit(enum: Enum, module: Module): File = enum
    .convert()
    .transform {
        matchingElements { languageEnum: LanguageEnum ->
            languageEnum.copy(
                entries = languageEnum.entries.map {
                    LanguageEnum.Entry(Name.of(it.name.value().sanitizeEnum().sanitizeKeywords()), listOf("\"${it.name.value()}\""))
                },
            )
        }
    }
    .sanitizeNames()
```

Replace with a simpler version that only sanitizes entry names (values are now from converter):

```kotlin
override fun emit(enum: Enum, module: Module): File = enum
    .convert()
    .transform {
        matchingElements { languageEnum: LanguageEnum ->
            languageEnum.copy(
                entries = languageEnum.entries.map {
                    it.copy(name = Name.of(it.name.value().sanitizeEnum().sanitizeKeywords()))
                },
            )
        }
    }
    .sanitizeNames()
```

- [ ] **Step 2: Run Python emitter tests**

Run: `./gradlew :src:compiler:emitters:python:allTests`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add src/compiler/emitters/python/src/commonMain/kotlin/community/flock/wirespec/emitters/python/PythonIrEmitter.kt
git commit -m "refactor: simplify Python enum emit, entry values from converter"
```

---

### Task 5: Remove enum entry value transform from Rust emitter

**Files:**
- Modify: `src/compiler/emitters/rust/src/commonMain/kotlin/community/flock/wirespec/emitters/rust/RustIrEmitter.kt:326-336`

- [ ] **Step 1: Simplify the Rust enum emit function**

The current Rust `emit(enum)` function:

```kotlin
override fun emit(enum: Enum, module: Module): File = enum
    .convert()
    .transform {
        matchingElements { languageEnum: LanguageEnum ->
            languageEnum.copy(
                entries = languageEnum.entries.map {
                    LanguageEnum.Entry(Name.of(it.name.value().sanitizeEnum().sanitizeKeywords()), listOf("\"${it.name.value()}\""))
                },
            )
        }
    }
```

Replace with a simpler version:

```kotlin
override fun emit(enum: Enum, module: Module): File = enum
    .convert()
    .transform {
        matchingElements { languageEnum: LanguageEnum ->
            languageEnum.copy(
                entries = languageEnum.entries.map {
                    it.copy(name = Name.of(it.name.value().sanitizeEnum().sanitizeKeywords()))
                },
            )
        }
    }
```

- [ ] **Step 2: Run Rust emitter tests**

Run: `./gradlew :src:compiler:emitters:rust:allTests`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add src/compiler/emitters/rust/src/commonMain/kotlin/community/flock/wirespec/emitters/rust/RustIrEmitter.kt
git commit -m "refactor: simplify Rust enum emit, entry values from converter"
```

---

### Task 6: Simplify refined toString in Java emitter

**Files:**
- Modify: `src/compiler/emitters/java/src/commonMain/kotlin/community/flock/wirespec/emitters/java/JavaIrEmitter.kt:218-241`

- [ ] **Step 1: Remove the manual toString injection from Java refined emit**

The current Java `emit(refined)` adds a toString, validate override, and value() accessor. Since the converter now provides toString and validate, we only need to add override markers and the value() accessor.

Replace the `emit(refined)` function. The current code builds elements from scratch:
```kotlin
elements = listOf(
    function("toString", isOverride = true) { ... },
) + s.elements.map { element ->
    if (element is LanguageFunction && element.name == Name.of("validate")) {
        element.copy(isOverride = true)
    } else element
} + listOf(
    function("value", isOverride = true) { ... },
),
```

Simplify to reuse converter-generated toString and validate, marking them as override and appending value():
```kotlin
elements = s.elements.map { element ->
    if (element is LanguageFunction) {
        element.copy(isOverride = true)
    } else element
} + listOf(
    function("value", isOverride = true) {
        returnType(refined.reference.convert())
        returns(VariableReference(Name.of("value")))
    },
),
```

This marks both `validate` and `toString` (from converter) as `isOverride = true`, and adds the Java-specific `value()` accessor.

- [ ] **Step 2: Run Java emitter tests**

Run: `./gradlew :src:compiler:emitters:java:allTests`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add src/compiler/emitters/java/src/commonMain/kotlin/community/flock/wirespec/emitters/java/JavaIrEmitter.kt
git commit -m "refactor: simplify Java refined emit, reuse converter toString"
```

---

### Task 7: Simplify refined toString in Kotlin emitter

**Files:**
- Modify: `src/compiler/emitters/kotlin/src/commonMain/kotlin/community/flock/wirespec/emitters/kotlin/KotlinIrEmitter.kt:195-216`

- [ ] **Step 1: Simplify Kotlin refined emit**

The current Kotlin `emit(refined)` manually builds toString and validate from scratch. Since the converter now provides both, simplify to reuse them with override marking.

The current code:
```kotlin
override fun emit(refined: Refined): File {
    val file = refined.convert().sanitizeNames()
    val struct = file.findElement<Struct>()!!
    val toStringExpr = when (refined.reference.type) {
        is Reference.Primitive.Type.String -> "value"
        else -> "value.toString()"
    }
    val updatedStruct = struct.copy(
        fields = struct.fields.map { f -> f.copy(isOverride = true) },
        elements = listOf(
            function("toString", isOverride = true) {
                returnType(LanguageType.String)
                returns(RawExpression(toStringExpr))
            },
            function("validate", isOverride = true) {
                returnType(LanguageType.Boolean)
                returns(refined.reference.convertConstraint(VariableReference(Name.of("value"))))
            },
        ),
    )
    return LanguageFile(Name.of(refined.identifier.sanitize()), listOf(updatedStruct))
}
```

Replace with:
```kotlin
override fun emit(refined: Refined): File {
    val file = refined.convert().sanitizeNames()
    val struct = file.findElement<Struct>()!!
    val updatedStruct = struct.copy(
        fields = struct.fields.map { f -> f.copy(isOverride = true) },
        elements = struct.elements.map { element ->
            if (element is LanguageFunction) {
                element.copy(isOverride = true)
            } else element
        },
    )
    return LanguageFile(Name.of(refined.identifier.sanitize()), listOf(updatedStruct))
}
```

This reuses the converter-generated validate and toString, only marking them as override. The Kotlin generator already handles `VariableReference("value")` and `FunctionCall(value, "toString")` correctly.

- [ ] **Step 2: Run Kotlin emitter tests**

Run: `./gradlew :src:compiler:emitters:kotlin:allTests`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add src/compiler/emitters/kotlin/src/commonMain/kotlin/community/flock/wirespec/emitters/kotlin/KotlinIrEmitter.kt
git commit -m "refactor: simplify Kotlin refined emit, reuse converter toString"
```

---

### Task 8: Simplify refined toString in Scala emitter

**Files:**
- Modify: `src/compiler/emitters/scala/src/commonMain/kotlin/community/flock/wirespec/emitters/scala/ScalaIrEmitter.kt`

- [ ] **Step 1: Simplify Scala refined emit**

Find the `emit(refined)` function in `ScalaIrEmitter.kt`. It should follow a similar pattern to Kotlin — manually building toString and validate. Simplify to reuse converter-generated functions, marking them as override:

```kotlin
override fun emit(refined: Refined): File {
    val file = refined.convert().sanitizeNames()
    val struct = file.findElement<Struct>()!!
    val updatedStruct = struct.copy(
        fields = struct.fields.map { f -> f.copy(isOverride = true) },
        elements = struct.elements.map { element ->
            if (element is LanguageFunction) {
                element.copy(isOverride = true)
            } else element
        },
    )
    return LanguageFile(Name.of(refined.identifier.sanitize()), listOf(updatedStruct))
}
```

Note: Verify this matches the existing Scala pattern — if Scala has additional elements (like Kotlin's RawExpression), those need to be handled. Read the current Scala emit(refined) before applying.

- [ ] **Step 2: Run Scala emitter tests**

Run: `./gradlew :src:compiler:emitters:scala:allTests`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add src/compiler/emitters/scala/src/commonMain/kotlin/community/flock/wirespec/emitters/scala/ScalaIrEmitter.kt
git commit -m "refactor: simplify Scala refined emit, reuse converter toString"
```

---

### Task 9: Simplify refined toString in Python emitter

**Files:**
- Modify: `src/compiler/emitters/python/src/commonMain/kotlin/community/flock/wirespec/emitters/python/PythonIrEmitter.kt:184-209`

- [ ] **Step 1: Simplify Python refined emit**

The current Python `emit(refined)` builds `__str__` and validate from scratch. Since the converter now provides toString and validate, simplify to:
1. Rename toString to `__str__`
2. Add `self` parameter
3. Transform `VariableReference("value")` to `FieldCall(self, "value")` in function bodies

Replace the current code with:

```kotlin
override fun emit(refined: Refined): File {
    val file = refined.convert()
    val struct = file.findElement<Struct>()!!
    val updatedStruct = struct.copy(
        elements = struct.elements.mapNotNull { element ->
            when {
                element is LanguageFunction && element.name == Name.of("validate") -> {
                    val constraintExpr = refined.reference.convertConstraint(
                        FieldCall(VariableReference(Name.of("self")), Name.of("value"))
                    )
                    function("validate") {
                        arg("self", LanguageType.Custom(""))
                        returnType(LanguageType.Boolean)
                        returns(constraintExpr)
                    }
                }
                element is LanguageFunction && element.name == Name.of("toString") -> {
                    val toStringExpr = when (refined.reference.type) {
                        is Reference.Primitive.Type.String -> "self.value"
                        else -> "str(self.value)"
                    }
                    function("__str__") {
                        arg("self", LanguageType.Custom(""))
                        returnType(LanguageType.String)
                        returns(RawExpression(toStringExpr))
                    }
                }
                else -> element
            }
        },
    )
    return file
        .transform {
            matchingElements { _: Struct -> updatedStruct }
        }
        .sanitizeNames()
}
```

Note: Python still needs to rebuild the function bodies because it needs `self` receiver and `FieldCall(self, value)` patterns. The converter's toString body (`VariableReference("value")`) doesn't work directly in Python. The simplification here is structural — we use the converter's function as a signal of what to generate rather than inspecting the refined reference type from scratch.

- [ ] **Step 2: Run Python emitter tests**

Run: `./gradlew :src:compiler:emitters:python:allTests`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add src/compiler/emitters/python/src/commonMain/kotlin/community/flock/wirespec/emitters/python/PythonIrEmitter.kt
git commit -m "refactor: simplify Python refined emit, reuse converter toString signal"
```

---

### Task 10: Simplify refined toString in Rust emitter

**Files:**
- Modify: `src/compiler/emitters/rust/src/commonMain/kotlin/community/flock/wirespec/emitters/rust/RustIrEmitter.kt:341-347`

- [ ] **Step 1: Simplify Rust refined emit**

The current Rust `emit(refined)` replaces struct elements with `buildValidateFunction()` and `buildToStringFunction()`. Since the converter now provides both, simplify to transform the existing functions rather than rebuilding from scratch.

However, Rust has the same issue as Python — it needs `&self` parameter and Rust-specific expressions (`self.value.clone()`, `format!`). The converter's generic expressions don't work directly.

Replace the current code with:

```kotlin
override fun emit(refined: Refined): File =
    refined.convert()
        .transform {
            matchingElements { s: Struct ->
                s.copy(elements = listOf(buildValidateFunction(refined), buildToStringFunction(refined)))
            }
        }
```

Actually — this is identical to what Rust already has. The Rust emitter already rebuilds these functions because Rust syntax is too different. **No change needed for Rust refined emit.** The `buildValidateFunction()` and `buildToStringFunction()` helpers remain as-is since they produce Rust-specific code (`&self`, `clone()`, `format!`).

Skip this task — Rust refined emit is already correct.

---

### Task 11: Run full verification

- [ ] **Step 1: Run all emitter tests**

Run: `./gradlew :src:compiler:emitters:java:allTests :src:compiler:emitters:kotlin:allTests :src:compiler:emitters:typescript:allTests :src:compiler:emitters:python:allTests :src:compiler:emitters:rust:allTests :src:compiler:emitters:scala:allTests :src:compiler:ir:allTests`
Expected: ALL PASS

- [ ] **Step 2: Run the full make all target if available**

Run: `make all` or `./gradlew build`
Expected: PASS — full build succeeds

- [ ] **Step 3: Commit any formatting fixes**

If spotless or other formatters flag issues:
```bash
./gradlew spotlessApply
git add -A
git commit -m "style: apply spotless formatting"
```
