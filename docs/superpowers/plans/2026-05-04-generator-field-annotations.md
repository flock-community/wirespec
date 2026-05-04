# Field Annotations on the Arbitrary-Data Generator Callback — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Surface parser-AST field annotations to the runtime `Wirespec.Generator.generate(...)` callback as a fourth `annotations` argument, in language-neutral IR plus Kotlin/Java verification and spring-boot example update.

**Architecture:** `GeneratorConverter` walks `Field.annotations` and emits an IR `LiteralList<LiteralMap<String, Any>>` as the new fourth argument to every `generator.generate(...)` call. `IrConverter.SharedWirespec` adds the same parameter to the emitted `Generator` interface. Coercion (`"true"`→bool, `^-?[0-9]+$`→Long, decimal→Double, else→String) happens at converter time when each `Annotation.Value.Single` is turned into an IR `Literal`. Rust runtime is deferred (Rust generator converter has pre-existing issues and is excluded from `VerifyGeneratorTest`).

**Tech Stack:** Kotlin Multiplatform, Gradle, kotest. Compiler core: `src/compiler/core`. IR module: `src/compiler/ir`. Verify: `src/verify`. Spring-boot example: `examples/gradle-spring-boot`.

**Spec:** `docs/superpowers/specs/2026-05-04-generator-field-annotations-design.md`

---

## File map

- **Modify:** `src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/converter/GeneratorConverter.kt` — coercion helper, annotation-to-IR helper, wire into `generatorCallExpression()` and the four `convertToGenerator` entry points.
- **Modify:** `src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/converter/IrConverter.kt` — add `annotations` parameter to the emitted `Generator.generate` interface (lines 250–258).
- **Modify:** `src/compiler/ir/src/commonTest/kotlin/community/flock/wirespec/ir/converter/GeneratorConverterTest.kt` — new tests for coercion + annotation emission.
- **Modify:** `src/verify/src/test/kotlin/community/flock/wirespec/verify/VerifyGeneratorTest.kt` — update the in-test `Generator` struct to accept the new parameter; pass empty list at runtime.
- **Modify:** `examples/gradle-spring-boot/src/main/wirespec/projects.ws` — add `@Email` annotation to one `MemberInput` field.
- **Modify:** `examples/gradle-spring-boot/src/test/kotlin/community/flock/wirespec/examples/spring/testutil/TestGenerators.kt` — update `SeededGenerator.generate(...)` to accept the new parameter; branch on `@Email` to demo the loop.

All converter helpers live as top-level (file-private or `internal`) functions in `GeneratorConverter.kt`. Why: that file already owns the converter-time coercion logic (`toFieldDescriptor`, `pathPlus`); keeping the new helpers next door preserves the one-file-one-responsibility shape established on this branch.

---

## Task 1: Coercion helper — `coerceAnnotationValueLiteral(String): Literal`

**Files:**
- Modify: `src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/converter/GeneratorConverter.kt` (append)
- Modify: `src/compiler/ir/src/commonTest/kotlin/community/flock/wirespec/ir/converter/GeneratorConverterTest.kt` (append)

- [ ] **Step 1: Write the failing test**

Append to `GeneratorConverterTest.kt`, inside the `class GeneratorConverterTest` body:

```kotlin
@Test
fun testCoerceAnnotationValueLiteralBoolean() {
    assertEquals(Literal(true, Type.Boolean), coerceAnnotationValueLiteral("true"))
    assertEquals(Literal(false, Type.Boolean), coerceAnnotationValueLiteral("false"))
}

@Test
fun testCoerceAnnotationValueLiteralInteger() {
    assertEquals(Literal(0L, Type.Integer()), coerceAnnotationValueLiteral("0"))
    assertEquals(Literal(42L, Type.Integer()), coerceAnnotationValueLiteral("42"))
    assertEquals(Literal(-7L, Type.Integer()), coerceAnnotationValueLiteral("-7"))
}

@Test
fun testCoerceAnnotationValueLiteralDouble() {
    assertEquals(Literal(1.5, Type.Number()), coerceAnnotationValueLiteral("1.5"))
    assertEquals(Literal(-3.14, Type.Number()), coerceAnnotationValueLiteral("-3.14"))
    assertEquals(Literal(1.0e10, Type.Number()), coerceAnnotationValueLiteral("1.0e10"))
}

@Test
fun testCoerceAnnotationValueLiteralStringFallback() {
    assertEquals(Literal("hello", Type.String), coerceAnnotationValueLiteral("hello"))
    assertEquals(Literal("True", Type.String), coerceAnnotationValueLiteral("True"))      // case-sensitive
    assertEquals(Literal("1e10", Type.String), coerceAnnotationValueLiteral("1e10"))      // no decimal point → not Double
    assertEquals(Literal("+1", Type.String), coerceAnnotationValueLiteral("+1"))          // explicit + sign → String
    assertEquals(Literal("", Type.String), coerceAnnotationValueLiteral(""))
}
```

You will also need this import at the top of the test file (alongside existing imports):

```kotlin
import community.flock.wirespec.ir.core.Literal
```

(`Literal` is already imported.)

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :src:compiler:ir:jvmTest --tests "community.flock.wirespec.ir.converter.GeneratorConverterTest.testCoerceAnnotationValueLiteralBoolean"`

Expected: FAIL — `Unresolved reference: coerceAnnotationValueLiteral`.

- [ ] **Step 3: Implement the helper**

Append to `GeneratorConverter.kt` (file-level, after `toFieldDescriptor`):

```kotlin
private val INTEGER_REGEX = Regex("^-?[0-9]+$")
private val DOUBLE_REGEX = Regex("^-?[0-9]+\\.[0-9]+([eE]-?[0-9]+)?$")

internal fun coerceAnnotationValueLiteral(raw: String): Literal = when {
    raw == "true" -> Literal(true, Type.Boolean)
    raw == "false" -> Literal(false, Type.Boolean)
    INTEGER_REGEX.matches(raw) -> Literal(raw.toLong(), Type.Integer())
    DOUBLE_REGEX.matches(raw) -> Literal(raw.toDouble(), Type.Number())
    else -> Literal(raw, Type.String)
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :src:compiler:ir:jvmTest --tests "community.flock.wirespec.ir.converter.GeneratorConverterTest.testCoerceAnnotationValueLiteral*"`

Expected: PASS for all four tests.

- [ ] **Step 5: Commit**

```bash
git add src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/converter/GeneratorConverter.kt src/compiler/ir/src/commonTest/kotlin/community/flock/wirespec/ir/converter/GeneratorConverterTest.kt
git commit -m "feat(ir): add annotation-value coercion helper for generator converter"
```

---

## Task 2: Annotation-to-IR helper — `Annotation.toIrLiteralMap()`

Each parser `Annotation` becomes an IR `LiteralMap` with two entries — `"name"` (String literal) and `"parameters"` (nested `LiteralMap`). `Value.Array` becomes an IR `LiteralList`; `Value.Dict` recurses.

**Files:**
- Modify: `src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/converter/GeneratorConverter.kt` (append)
- Modify: `src/compiler/ir/src/commonTest/kotlin/community/flock/wirespec/ir/converter/GeneratorConverterTest.kt` (append)

- [ ] **Step 1: Write the failing tests**

Add this import to the top of `GeneratorConverterTest.kt`:

```kotlin
import community.flock.wirespec.compiler.core.parse.ast.Annotation
import community.flock.wirespec.ir.core.LiteralList
import community.flock.wirespec.ir.core.LiteralMap
```

Append these tests:

```kotlin
@Test
fun testAnnotationToIrLiteralMapBare() {
    val ann = Annotation(name = "Deprecated", parameters = emptyList())
    val ir = ann.toIrLiteralMap()
    assertEquals(
        LiteralMap(
            values = mapOf(
                "name" to Literal("Deprecated", Type.String),
                "parameters" to LiteralMap(emptyMap(), Type.String, Type.Any),
            ),
            keyType = Type.String,
            valueType = Type.Any,
        ),
        ir,
    )
}

@Test
fun testAnnotationToIrLiteralMapMixedSingleParams() {
    val ann = Annotation(
        name = "Range",
        parameters = listOf(
            Annotation.Parameter("min", Annotation.Value.Single("0")),
            Annotation.Parameter("max", Annotation.Value.Single("1.5")),
            Annotation.Parameter("label", Annotation.Value.Single("hello")),
            Annotation.Parameter("active", Annotation.Value.Single("true")),
        ),
    )
    val ir = ann.toIrLiteralMap()
    val params = (ir.values.getValue("parameters") as LiteralMap).values
    assertEquals(Literal(0L, Type.Integer()), params.getValue("min"))
    assertEquals(Literal(1.5, Type.Number()), params.getValue("max"))
    assertEquals(Literal("hello", Type.String), params.getValue("label"))
    assertEquals(Literal(true, Type.Boolean), params.getValue("active"))
}

@Test
fun testAnnotationToIrLiteralMapArrayParam() {
    val ann = Annotation(
        name = "Tags",
        parameters = listOf(
            Annotation.Parameter(
                name = "items",
                value = Annotation.Value.Array(
                    listOf(Annotation.Value.Single("1"), Annotation.Value.Single("2")),
                ),
            ),
        ),
    )
    val ir = ann.toIrLiteralMap()
    val items = (ir.values.getValue("parameters") as LiteralMap).values.getValue("items")
    assertEquals(
        LiteralList(
            values = listOf(Literal(1L, Type.Integer()), Literal(2L, Type.Integer())),
            type = Type.Any,
        ),
        items,
    )
}

@Test
fun testAnnotationToIrLiteralMapDictParam() {
    val ann = Annotation(
        name = "Meta",
        parameters = listOf(
            Annotation.Parameter(
                name = "info",
                value = Annotation.Value.Dict(
                    listOf(
                        Annotation.Parameter("a", Annotation.Value.Single("1")),
                        Annotation.Parameter("b", Annotation.Value.Single("hello")),
                    ),
                ),
            ),
        ),
    )
    val ir = ann.toIrLiteralMap()
    val info = (ir.values.getValue("parameters") as LiteralMap).values.getValue("info") as LiteralMap
    assertEquals(Literal(1L, Type.Integer()), info.values.getValue("a"))
    assertEquals(Literal("hello", Type.String), info.values.getValue("b"))
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :src:compiler:ir:jvmTest --tests "community.flock.wirespec.ir.converter.GeneratorConverterTest.testAnnotation*"`

Expected: FAIL — `Unresolved reference: toIrLiteralMap`.

- [ ] **Step 3: Implement the helper**

Append to `GeneratorConverter.kt`:

```kotlin
import community.flock.wirespec.compiler.core.parse.ast.Annotation as AnnotationWirespec
```

(Place this import alongside the other parser-AST aliases at the top of the file.)

Then add at file scope (after `coerceAnnotationValueLiteral`):

```kotlin
private fun annotationValueToIrExpression(value: AnnotationWirespec.Value): Expression = when (value) {
    is AnnotationWirespec.Value.Single -> coerceAnnotationValueLiteral(value.value)
    is AnnotationWirespec.Value.Array -> LiteralList(
        values = value.value.map { coerceAnnotationValueLiteral(it.value) },
        type = Type.Any,
    )
    is AnnotationWirespec.Value.Dict -> LiteralMap(
        values = value.value.associate { it.name to annotationValueToIrExpression(it.value) },
        keyType = Type.String,
        valueType = Type.Any,
    )
}

internal fun AnnotationWirespec.toIrLiteralMap(): LiteralMap = LiteralMap(
    values = mapOf(
        "name" to Literal(name, Type.String),
        "parameters" to LiteralMap(
            values = parameters.associate { it.name to annotationValueToIrExpression(it.value) },
            keyType = Type.String,
            valueType = Type.Any,
        ),
    ),
    keyType = Type.String,
    valueType = Type.Any,
)
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :src:compiler:ir:jvmTest --tests "community.flock.wirespec.ir.converter.GeneratorConverterTest.testAnnotation*"`

Expected: PASS for all four tests.

- [ ] **Step 5: Commit**

```bash
git add src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/converter/GeneratorConverter.kt src/compiler/ir/src/commonTest/kotlin/community/flock/wirespec/ir/converter/GeneratorConverterTest.kt
git commit -m "feat(ir): add Annotation -> IR LiteralMap conversion with coercion"
```

---

## Task 3: List-of-annotations helper

A small wrapper that turns `List<Annotation>` into the IR `LiteralList<LiteralMap>` we'll pass as the new fourth argument.

**Files:**
- Modify: `src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/converter/GeneratorConverter.kt` (append)
- Modify: `src/compiler/ir/src/commonTest/kotlin/community/flock/wirespec/ir/converter/GeneratorConverterTest.kt` (append)

- [ ] **Step 1: Write the failing test**

Append to `GeneratorConverterTest.kt`:

```kotlin
@Test
fun testAnnotationsToIrListEmpty() {
    val ir = annotationsToIrList(emptyList())
    assertEquals(LiteralList(values = emptyList(), type = Type.Any), ir)
}

@Test
fun testAnnotationsToIrListPreservesOrderAndDuplicates() {
    val anns = listOf(
        Annotation("Validate", listOf(Annotation.Parameter("min", Annotation.Value.Single("0")))),
        Annotation("Validate", listOf(Annotation.Parameter("max", Annotation.Value.Single("100")))),
    )
    val ir = annotationsToIrList(anns)
    assertEquals(2, ir.values.size)
    val first = (ir.values[0] as LiteralMap).values.getValue("parameters") as LiteralMap
    val second = (ir.values[1] as LiteralMap).values.getValue("parameters") as LiteralMap
    assertEquals(Literal(0L, Type.Integer()), first.values.getValue("min"))
    assertEquals(Literal(100L, Type.Integer()), second.values.getValue("max"))
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :src:compiler:ir:jvmTest --tests "community.flock.wirespec.ir.converter.GeneratorConverterTest.testAnnotationsToIrList*"`

Expected: FAIL — `Unresolved reference: annotationsToIrList`.

- [ ] **Step 3: Implement the helper**

Append to `GeneratorConverter.kt`:

```kotlin
internal fun annotationsToIrList(annotations: List<AnnotationWirespec>): LiteralList = LiteralList(
    values = annotations.map { it.toIrLiteralMap() },
    type = Type.Any,
)
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :src:compiler:ir:jvmTest --tests "community.flock.wirespec.ir.converter.GeneratorConverterTest.testAnnotationsToIrList*"`

Expected: PASS for both tests.

- [ ] **Step 5: Commit**

```bash
git add src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/converter/GeneratorConverter.kt src/compiler/ir/src/commonTest/kotlin/community/flock/wirespec/ir/converter/GeneratorConverterTest.kt
git commit -m "feat(ir): add annotationsToIrList helper for generator converter"
```

---

## Task 4: Wire annotations into `generatorCallExpression`

Add `annotations` as a parameter on the helper and as a fourth argument to the emitted `generator.generate(...)` call. Then thread `field.annotations` through every call site in the four `convertToGenerator` entry points (`Type`, `Refined`, `Enum`, `Union`) plus the inline `FunctionCall` for `Iterable.element`.

**Files:**
- Modify: `src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/converter/GeneratorConverter.kt`
- Modify: `src/compiler/ir/src/commonTest/kotlin/community/flock/wirespec/ir/converter/GeneratorConverterTest.kt` (append)

- [ ] **Step 1: Write the failing test**

Append to `GeneratorConverterTest.kt`:

```kotlin
@Test
fun testTypeConvertToGeneratorThreadsFieldAnnotations() {
    val person = TypeWirespec(
        comment = null,
        annotations = emptyList(),
        identifier = definitionId("Person"),
        shape = TypeWirespec.Shape(
            listOf(
                Field(
                    annotations = listOf(
                        Annotation(
                            name = "Email",
                            parameters = emptyList(),
                        ),
                    ),
                    identifier = fieldId("email"),
                    reference = Reference.Primitive(Reference.Primitive.Type.String(null), false),
                ),
                Field(
                    annotations = emptyList(),
                    identifier = fieldId("name"),
                    reference = Reference.Primitive(Reference.Primitive.Type.String(null), false),
                ),
            ),
        ),
        extends = emptyList(),
    )

    val file = person.convertToGenerator()
    val calls = file.collectExpressions<community.flock.wirespec.ir.core.FunctionCall>()
        .filter { it.name == Name.of("generate") && it.receiver is community.flock.wirespec.ir.core.VariableReference }

    assertEquals(2, calls.size, "expected one generator.generate() call per primitive field")

    val emailAnnotations = calls[0].arguments.getValue(Name.of("annotations")) as LiteralList
    assertEquals(1, emailAnnotations.values.size)
    val emailAnn = emailAnnotations.values.single() as LiteralMap
    assertEquals(Literal("Email", Type.String), emailAnn.values.getValue("name"))

    val nameAnnotations = calls[1].arguments.getValue(Name.of("annotations")) as LiteralList
    assertTrue(nameAnnotations.values.isEmpty(), "field with no annotations should pass empty list")
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :src:compiler:ir:jvmTest --tests "community.flock.wirespec.ir.converter.GeneratorConverterTest.testTypeConvertToGeneratorThreadsFieldAnnotations"`

Expected: FAIL — `arguments.getValue(Name.of("annotations"))` returns null / NoSuchElementException because the call only has three arguments today.

- [ ] **Step 3: Add the parameter to `generatorCallExpression`**

Replace the existing `generatorCallExpression` in `GeneratorConverter.kt` (currently lines 108–120):

```kotlin
internal fun generatorCallExpression(
    typeName: String,
    fieldNameStr: String,
    fieldDescriptor: Expression,
    annotations: Expression = LiteralList(emptyList(), Type.Any),
): FunctionCall = FunctionCall(
    receiver = VariableReference(Name.of("generator")),
    name = Name.of("generate"),
    arguments = mapOf(
        Name.of("path") to pathPlus(fieldNameStr),
        Name.of("type") to ClassReference(Type.Custom(typeName)),
        Name.of("field") to fieldDescriptor,
        Name.of("annotations") to annotations,
    ),
)
```

- [ ] **Step 4: Thread annotations through `TypeWirespec.convertToGenerator` → `toGeneratorExpression`**

Change `toGeneratorExpression` to take a `List<AnnotationWirespec>` and forward it.

In `convertToGenerator` (currently lines 122–144), update the call to pass the field's annotations:

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
                            fieldName to field.reference.toGeneratorExpression(
                                typeName,
                                field.identifier.value,
                                field.annotations,
                            )
                        },
                    ),
                )
            }
        }
    }
}
```

In `toGeneratorExpression` (currently lines 146–275), change the signature and forward annotations to every `generatorCallExpression` and inline `FunctionCall(... receiver = VariableReference("generator")...)`:

```kotlin
private fun ReferenceWirespec.toGeneratorExpression(
    typeName: String,
    fieldNameStr: String,
    annotations: List<AnnotationWirespec>,
): Expression {
    val annotationsArg = annotationsToIrList(annotations)
    val nullableCheck: Expression = generatorCallExpression(
        typeName,
        fieldNameStr,
        ConstructorStatement(
            type = Type.Custom("Wirespec.GeneratorFieldNullable"),
            namedArguments = mapOf(
                Name.of("inner") to when (val ref = this) {
                    is ReferenceWirespec.Primitive -> NullableOf(ref.toFieldDescriptor())
                    is ReferenceWirespec.Iterable -> NullableOf(
                        ConstructorStatement(
                            type = Type.Custom("Wirespec.GeneratorFieldArray"),
                            namedArguments = mapOf(Name.of("inner") to ref.reference.toFieldDescriptorOrNull()),
                        ),
                    )
                    is ReferenceWirespec.Dict -> NullableOf(
                        ConstructorStatement(
                            type = Type.Custom("Wirespec.GeneratorFieldDict"),
                            namedArguments = mapOf(
                                Name.of("key") to NullableEmpty,
                                Name.of("value") to ref.reference.toFieldDescriptorOrNull(),
                            ),
                        ),
                    )
                    is ReferenceWirespec.Custom, is ReferenceWirespec.Any, is ReferenceWirespec.Unit -> NullableEmpty
                },
            ),
        ),
        annotationsArg,
    )

    val nonNullExpr: Expression = when (val ref = this) {
        is ReferenceWirespec.Primitive -> generatorCallExpression(
            typeName, fieldNameStr, ref.toFieldDescriptor(), annotationsArg,
        )
        is ReferenceWirespec.Custom -> FunctionCall(
            receiver = RawExpression("${ref.value}Generator"),
            name = Name.of("generate"),
            arguments = mapOf(
                Name.of("path") to pathPlus(fieldNameStr),
                Name.of("generator") to VariableReference(Name.of("generator")),
            ),
        )
        is ReferenceWirespec.Iterable -> {
            val countCall = generatorCallExpression(
                typeName,
                fieldNameStr,
                ConstructorStatement(
                    type = Type.Custom("Wirespec.GeneratorFieldArray"),
                    namedArguments = mapOf(Name.of("inner") to ref.reference.toFieldDescriptorOrNull()),
                ),
                annotationsArg,
            )
            val indexAsString = StringTemplate(
                listOf(
                    StringTemplate.Part.Text(""),
                    StringTemplate.Part.Expr(VariableReference(Name.of("i"))),
                ),
            )
            val indexedPath = ListConcat(
                listOf(
                    VariableReference(Name.of("path")),
                    LiteralList(
                        listOf(Literal(fieldNameStr, Type.String), indexAsString),
                        Type.String,
                    ),
                ),
            )
            val elementExpr: Expression = when (val inner = ref.reference) {
                is ReferenceWirespec.Custom -> FunctionCall(
                    receiver = RawExpression("${inner.value}Generator"),
                    name = Name.of("generate"),
                    arguments = mapOf(
                        Name.of("path") to indexedPath,
                        Name.of("generator") to VariableReference(Name.of("generator")),
                    ),
                )
                is ReferenceWirespec.Primitive -> FunctionCall(
                    receiver = VariableReference(Name.of("generator")),
                    name = Name.of("generate"),
                    arguments = mapOf(
                        Name.of("path") to indexedPath,
                        Name.of("type") to ClassReference(Type.Custom(typeName)),
                        Name.of("field") to inner.toFieldDescriptor(),
                        Name.of("annotations") to LiteralList(emptyList(), Type.Any),
                    ),
                )
                else -> NullLiteral
            }
            MapExpression(
                receiver = BinaryOp(
                    Literal(0, Type.Integer()),
                    BinaryOp.Operator.UNTIL,
                    countCall,
                ),
                variable = Name.of("i"),
                body = elementExpr,
            )
        }
        is ReferenceWirespec.Dict -> generatorCallExpression(
            typeName,
            fieldNameStr,
            ConstructorStatement(
                type = Type.Custom("Wirespec.GeneratorFieldDict"),
                namedArguments = mapOf(
                    Name.of("key") to NullableEmpty,
                    Name.of("value") to ref.reference.toFieldDescriptorOrNull(),
                ),
            ),
            annotationsArg,
        )
        is ReferenceWirespec.Any, is ReferenceWirespec.Unit -> NullLiteral
    }

    return if (this.isNullable) {
        IfExpression(
            condition = nullableCheck,
            thenExpr = NullableEmpty,
            elseExpr = NullableOf(nonNullExpr),
        )
    } else {
        nonNullExpr
    }
}
```

(The element-level `FunctionCall` for `Iterable.element` keeps an empty annotations list — the iterable's own annotations are conveyed at the count-call site, not on each element.)

- [ ] **Step 5: Forward annotations in `Refined.convertToGenerator`**

Replace the body of `RefinedWirespec.convertToGenerator()` (currently lines 277–306):

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
                                    Name.of("annotations") to annotationsToIrList(annotations),
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

(Refined types' annotations live on the Refined definition itself.)

- [ ] **Step 6: Forward annotations in `Enum.convertToGenerator` and `Union.convertToGenerator`**

In `EnumWirespec.convertToGenerator()`, find the inner `FunctionCall` (currently around lines 322–339) and add the annotations argument:

```kotlin
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
    Name.of("annotations") to annotationsToIrList(annotations),
),
```

Apply the same change to the `FunctionCall` in `UnionWirespec.convertToGenerator()` (currently around lines 360–377):

```kotlin
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
    Name.of("annotations") to annotationsToIrList(annotations),
),
```

- [ ] **Step 7: Run the new test to verify it passes**

Run: `./gradlew :src:compiler:ir:jvmTest --tests "community.flock.wirespec.ir.converter.GeneratorConverterTest.testTypeConvertToGeneratorThreadsFieldAnnotations"`

Expected: PASS.

- [ ] **Step 8: Run the full GeneratorConverterTest suite to ensure no regression**

Run: `./gradlew :src:compiler:ir:jvmTest --tests "community.flock.wirespec.ir.converter.GeneratorConverterTest"`

Expected: PASS for all tests (the existing four tests still pass because they only check namespace/class-references/switches, not call argument cardinality).

- [ ] **Step 9: Commit**

```bash
git add src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/converter/GeneratorConverter.kt src/compiler/ir/src/commonTest/kotlin/community/flock/wirespec/ir/converter/GeneratorConverterTest.kt
git commit -m "feat(ir): thread field annotations into generator.generate() emission"
```

---

## Task 5: Update emitted `Generator.generate` interface signature

The shared Wirespec runtime is itself emitted by `IrConverter.SharedWirespec.convert()`. Add the new `annotations` parameter to the emitted `Generator.generate` function so user-side implementations are required to accept it.

**Files:**
- Modify: `src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/converter/IrConverter.kt`
- Modify: `src/compiler/ir/src/commonTest/kotlin/community/flock/wirespec/ir/converter/IrConverterTest.kt` (append)

- [ ] **Step 1: Write the failing test**

Append to `IrConverterTest.kt`:

```kotlin
@Test
fun testSharedGeneratorInterfaceHasAnnotationsParameter() {
    val shared = SharedWirespec.convert()
    val generatorIface = shared
        .filterIsInstance<community.flock.wirespec.ir.core.Interface>()
        .single { it.name == community.flock.wirespec.ir.core.Name.of("Generator") }
    val generateFn = generatorIface.elements
        .filterIsInstance<community.flock.wirespec.ir.core.Function>()
        .single { it.name == community.flock.wirespec.ir.core.Name.of("generate") }
    val paramNames = generateFn.parameters.map { it.name }
    assertEquals(
        listOf(
            community.flock.wirespec.ir.core.Name.of("path"),
            community.flock.wirespec.ir.core.Name.of("type"),
            community.flock.wirespec.ir.core.Name.of("field"),
            community.flock.wirespec.ir.core.Name.of("annotations"),
        ),
        paramNames,
    )
}
```

(If the test file's package or fully-qualified `Interface` accessor differs, use `import` statements at the top — but the test data exposes `SharedWirespec.convert()`, so adapt to whatever existing test in `IrConverterTest.kt` is doing.)

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :src:compiler:ir:jvmTest --tests "community.flock.wirespec.ir.converter.IrConverterTest.testSharedGeneratorInterfaceHasAnnotationsParameter"`

Expected: FAIL — `paramNames` has only three entries.

- [ ] **Step 3: Add the parameter**

In `IrConverter.kt` at lines 250–258, add the new arg. The IR DSL (`BaseBuilder` in `Dsl.kt`) has `string`, `list(...)`, `dict(...)`, `reflect`, etc. but **no** `any` helper, so use `Type.Any` directly — this matches the existing mixed style on line 239 (`type("GeneratorField", Type.Wildcard).nullable()`).

```kotlin
`interface`("Generator") {
    function("generate") {
        typeParam(type("T"))
        returnType(type("T"))
        arg("path", list(string))
        arg("type", reflect)
        arg("field", type("GeneratorField", type("T")))
        arg("annotations", list(dict(string, Type.Any)))
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :src:compiler:ir:jvmTest --tests "community.flock.wirespec.ir.converter.IrConverterTest.testSharedGeneratorInterfaceHasAnnotationsParameter"`

Expected: PASS.

- [ ] **Step 5: Run the rest of the IR module's tests to catch downstream regressions**

Run: `./gradlew :src:compiler:ir:jvmTest`

Expected: PASS for all tests.

- [ ] **Step 6: Commit**

```bash
git add src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/converter/IrConverter.kt src/compiler/ir/src/commonTest/kotlin/community/flock/wirespec/ir/converter/IrConverterTest.kt
git commit -m "feat(ir): add annotations parameter to emitted Generator.generate interface"
```

---

## Task 6: Verify full Kotlin + Java code generation

`VerifyGeneratorTest` exercises the end-to-end pipeline: it compiles a generated `PersonGenerator.kt`/`.java`, instantiates a runtime `Wirespec.Generator`, and checks the output. The runtime `Generator` struct in this test must now match the new 4-arg signature.

**Files:**
- Modify: `src/verify/src/test/kotlin/community/flock/wirespec/verify/VerifyGeneratorTest.kt`

- [ ] **Step 1: Read the existing `Generator` struct definition**

Open the file and locate the `struct("Generator")` block (starts around line 75). The current `function("generate", isOverride = true)` has parameters `path`, `type`, `field`. You must add an `annotations` parameter.

- [ ] **Step 2: Add the annotations parameter to the struct's generate function**

Inside the `struct("Generator") { ... function("generate", isOverride = true) { ... } }` block, after the `arg("field", ...)` line, add the new arg. Use the bare `Type.X` constructors to match the surrounding style (the existing `arg("path", Type.Array(Type.String))` line on line 79 of the test file uses the same form):

```kotlin
arg("annotations", Type.Array(Type.Dict(Type.String, Type.Any)))
```

The function body itself doesn't need to use `annotations` — the test verifies the round trip, not annotation-aware behaviour. Leave the existing body alone.

- [ ] **Step 3: Run the verify test for both Kotlin and Java**

Run: `./gradlew :src:verify:test --tests "community.flock.wirespec.verify.VerifyGeneratorTest"`

Expected: PASS for both Kotlin and Java parametrised cases.

If the test fails because the generated `PersonGenerator` still produces 3-arg calls, that means earlier tasks didn't propagate. Investigate; do not proceed.

If the test fails because the test fixture's `Generator` struct doesn't compile (e.g., Kotlin/Java syntax mismatch on `Type.Dict(...)` rendering), inspect the generated test source in the build output — the verify test prints generated source on failure — and align the type rendering.

- [ ] **Step 4: Commit**

```bash
git add src/verify/src/test/kotlin/community/flock/wirespec/verify/VerifyGeneratorTest.kt
git commit -m "test(verify): update Generator stub for new annotations parameter"
```

---

## Task 7: Annotate a field in the spring-boot example and consume it in `SeededGenerator`

Demonstrates the end-to-end loop: a `.ws` annotation reaches a custom `Generator` and changes its output.

**Files:**
- Modify: `examples/gradle-spring-boot/src/main/wirespec/projects.ws`
- Modify: `examples/gradle-spring-boot/src/test/kotlin/community/flock/wirespec/examples/spring/testutil/TestGenerators.kt`

- [ ] **Step 1: Identify the `MemberInput` definition in `projects.ws`**

Open `examples/gradle-spring-boot/src/main/wirespec/projects.ws` and find the `type MemberInput { ... }` block. It currently has fields like `name: String, email: String`.

- [ ] **Step 2: Add an `@Email` annotation to the email field**

Update the `MemberInput` block (annotation names must start with a capital letter per the lexer):

```
type MemberInput {
    name: String,
    @Email email: String
}
```

If the file uses different syntax for trailing fields, match it. The annotation is bare (no parameters).

- [ ] **Step 3: Run the wirespec generator to refresh `*Generator.kt`**

Run: `./gradlew :examples:gradle-spring-boot:wirespecJvmGenerate` (or the project's standard generation task — check the `examples/gradle-spring-boot` Gradle script if this name doesn't exist).

Expected: `examples/gradle-spring-boot/build/generated/.../MemberInputGenerator.kt` regenerates and the call to `generator.generate(...)` for the email field now passes a list containing one annotation map with `name = "Email"`.

If generation fails, inspect the error — the lexer requires `@[A-Z][a-zA-Z0-9_]*` (already satisfied by `@Email`).

- [ ] **Step 4: Update `SeededGenerator` to accept and use the new parameter**

In `examples/gradle-spring-boot/src/test/kotlin/community/flock/wirespec/examples/spring/testutil/TestGenerators.kt`, change the `generate` override:

```kotlin
@Suppress("UNCHECKED_CAST")
override fun <T : Any> generate(
    path: List<String>,
    type: KType,
    field: Wirespec.GeneratorField<T>,
    annotations: List<Map<String, Any?>>,
): T {
    if (annotations.any { it["name"] == "Email" }) {
        counter += 1
        return "user-$counter@example.com" as T
    }
    return when (field) {
        is Wirespec.GeneratorFieldString -> generateString(field) as T
        is Wirespec.GeneratorFieldInteger -> randomLong(field.min, field.max) as T
        is Wirespec.GeneratorFieldNumber -> randomDouble(field.min, field.max) as T
        Wirespec.GeneratorFieldBoolean -> random.nextBoolean() as T
        Wirespec.GeneratorFieldBytes -> ByteArray(0) as T
        is Wirespec.GeneratorFieldEnum -> field.values.random(random) as T
        is Wirespec.GeneratorFieldUnion -> field.variants.random(random) as T
        is Wirespec.GeneratorFieldArray -> 1 as T
        is Wirespec.GeneratorFieldNullable -> false as T
        is Wirespec.GeneratorFieldDict -> 1 as T
    }
}
```

- [ ] **Step 5: Run the spring-boot example tests**

Run: `./gradlew :examples:gradle-spring-boot:test`

Expected: existing tests still pass; `TestGenerators.memberInput(seed = 0L)` now produces a `MemberInput` whose `email` matches `user-N@example.com` instead of `value-N-…`.

If any existing test asserts the exact old email format on a deterministic seed, update the expected value to match the new `@Email`-aware output.

- [ ] **Step 6: Commit**

```bash
git add examples/gradle-spring-boot/src/main/wirespec/projects.ws examples/gradle-spring-boot/src/test/kotlin/community/flock/wirespec/examples/spring/testutil/TestGenerators.kt
git commit -m "feat(example): demonstrate @Email annotation reaching SeededGenerator"
```

---

## Task 8: Run the full build to catch missed call sites in other languages

The IR converter changes affect TypeScript and Python emitters too — they should generate compilable code automatically because the IR is language-neutral, but cross-check.

**Files:** none modified in this task.

- [ ] **Step 1: Run the IR-emitter test suites for TypeScript and Python**

```bash
./gradlew :src:compiler:emitters:typescript:jvmTest
./gradlew :src:compiler:emitters:python:jvmTest
```

Expected: PASS. These suites produce snapshot output for sample `.ws` inputs; the snapshots will now contain the 4-arg `generator.generate(...)` calls.

- [ ] **Step 2: Update snapshots if needed**

If the emitter tests are snapshot-based and fail with "expected 3-arg, got 4-arg", regenerate snapshots. The exact command depends on the framework — check the emitter's `commonTest/.../Snapshot*` directory or the test output for instructions. Inspect the diff to confirm the only change is the additional `annotations: ...` argument; if anything else changed, stop and investigate.

- [ ] **Step 3: Run the full top-level build**

```bash
./gradlew build -x detekt -x ktlintCheck
```

Expected: PASS. The exclusions are to keep this run focused on compile + test correctness; lint can run separately.

- [ ] **Step 4: Commit any snapshot updates**

```bash
git add -p   # review every hunk before staging
git commit -m "test(emitters): refresh snapshots for generator.generate annotations arg"
```

(Use `git add -p` so any unrelated drift in snapshots is caught before staging.)

---

## Out of scope — Rust runtime

The spec calls for a Rust `AnnotationValue` enum. This plan **does not** implement it because:

1. `VerifyGeneratorTest` already excludes Rust (per its scope comment: "Rust dotted names" issues).
2. The Rust emitter renders `Type.Any` and `Type.Dict(Type.String, Type.Any)` in some way today — that rendering may already be unsuitable for typed annotations, but fixing it intersects pre-existing Rust converter bugs and is out of scope for surfacing annotations.

Track as a follow-up plan: "Rust generator runtime — annotations + existing dotted-name issues".

---

## Self-review notes

- Spec coverage: every spec section (API shape, coercion rules, per-language signatures, converter emission, tests, example update, migration) maps to a task above. Rust runtime is explicitly deferred with a follow-up note (matches the spec's scope but defers the enum to a later branch where the Rust converter's other issues can also be addressed).
- Type consistency: helper names — `coerceAnnotationValueLiteral`, `toIrLiteralMap`, `annotationsToIrList` — used identically across tasks.
- Bite-sized steps: each task has 5–9 steps, each one a single 2–5 minute action.
