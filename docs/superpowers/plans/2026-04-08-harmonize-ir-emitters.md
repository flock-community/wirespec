# Harmonize IrEmitter Implementations Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make all 6 IrEmitter implementations structurally consistent by extracting shared building blocks and aligning each emitter's pipeline.

**Architecture:** Extract shared sanitization, import/package wrapping, and shared-source utilities into the `ir/emit` module. Each emitter adopts these building blocks while keeping language-specific composition. Raw strings are replaced with IR DSL where feasible, and endpoint emission pipelines are reordered to: convert -> flatten? -> enrich -> sanitize -> wrap.

**Tech Stack:** Kotlin Multiplatform, Wirespec IR DSL (`transform {}`, `struct {}`, `function {}`, `interface {}`)

---

## File Structure

### New files
- `src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/emit/Sanitization.kt` — shared `SanitizationConfig` and `sanitizeNames(config)` extension
- `src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/emit/EmitHelpers.kt` — `wrapWithPackage()`, `wrapWithModuleImport()`, `withSharedSource()`
- `src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/emit/SharedBuilder.kt` — `buildClientServerInterfaces()` shared builder
- `src/compiler/ir/src/commonTest/kotlin/community/flock/wirespec/ir/emit/SanitizationTest.kt` — tests for shared sanitization

### Modified files
- `src/compiler/emitters/java/src/commonMain/kotlin/community/flock/wirespec/emitters/java/JavaIrEmitter.kt`
- `src/compiler/emitters/kotlin/src/commonMain/kotlin/community/flock/wirespec/emitters/kotlin/KotlinIrEmitter.kt`
- `src/compiler/emitters/scala/src/commonMain/kotlin/community/flock/wirespec/emitters/scala/ScalaIrEmitter.kt`
- `src/compiler/emitters/python/src/commonMain/kotlin/community/flock/wirespec/emitters/python/PythonIrEmitter.kt`
- `src/compiler/emitters/typescript/src/commonMain/kotlin/community/flock/wirespec/emitters/typescript/TypeScriptIrEmitter.kt`
- `src/compiler/emitters/rust/src/commonMain/kotlin/community/flock/wirespec/emitters/rust/RustIrEmitter.kt`

### Test verification commands
```
./gradlew :src:compiler:ir:allTests
./gradlew :src:compiler:emitters:java:allTests
./gradlew :src:compiler:emitters:kotlin:allTests
./gradlew :src:compiler:emitters:scala:allTests
./gradlew :src:compiler:emitters:python:allTests
./gradlew :src:compiler:emitters:typescript:allTests
./gradlew :src:compiler:emitters:rust:allTests
```

---

### Task 1: Extract Shared SanitizationConfig and sanitizeNames()

**Files:**
- Create: `src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/emit/Sanitization.kt`
- Create: `src/compiler/ir/src/commonTest/kotlin/community/flock/wirespec/ir/emit/SanitizationTest.kt`

- [ ] **Step 1: Write the failing test for shared sanitizeNames**

Create test file `src/compiler/ir/src/commonTest/kotlin/community/flock/wirespec/ir/emit/SanitizationTest.kt`:

```kotlin
package community.flock.wirespec.ir.emit

import community.flock.wirespec.ir.core.Field
import community.flock.wirespec.ir.core.FieldCall
import community.flock.wirespec.ir.core.Function
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.Parameter
import community.flock.wirespec.ir.core.ReturnStatement
import community.flock.wirespec.ir.core.Struct
import community.flock.wirespec.ir.core.Type
import community.flock.wirespec.ir.core.VariableReference
import kotlin.test.Test
import kotlin.test.assertEquals

class SanitizationTest {

    private val javaLikeConfig = SanitizationConfig(
        reservedKeywords = setOf("class", "return", "import"),
        escapeKeyword = { "_$it" },
        fieldNameCase = { name ->
            val sanitized = if (name.parts.size > 1) name.camelCase() else name.value()
            Name(listOf(sanitized))
        },
        parameterNameCase = { name ->
            Name(listOf(name.camelCase()))
        },
        sanitizeSymbol = { str ->
            str.split(".", " ", "-")
                .mapIndexed { index, s ->
                    if (index > 0) s.replaceFirstChar { it.uppercase() } else s
                }
                .joinToString("")
                .filter { it.isLetterOrDigit() || it == '_' }
        },
    )

    @Test
    fun sanitizeFieldNames() {
        val struct = Struct(
            name = Name.of("Person"),
            fields = listOf(
                Field(Name.of("first-name"), Type.String),
                Field(Name.of("last.name"), Type.String),
            ),
        )

        val result = struct.sanitizeNames(javaLikeConfig)

        assertEquals("firstName", result.fields[0].name.value())
        assertEquals("lastName", result.fields[1].name.value())
    }

    @Test
    fun sanitizeReservedKeywordField() {
        val struct = Struct(
            name = Name.of("Item"),
            fields = listOf(
                Field(Name.of("class"), Type.String),
            ),
        )

        val result = struct.sanitizeNames(javaLikeConfig)

        assertEquals("_class", result.fields[0].name.value())
    }

    @Test
    fun sanitizeParameterNames() {
        val fn = Function(
            name = Name.of("doSomething"),
            parameters = listOf(
                Parameter(Name.of("my-param"), Type.String),
            ),
            returnType = Type.String,
            body = listOf(ReturnStatement(VariableReference(Name.of("my-param")))),
        )

        val result = fn.sanitizeNames(javaLikeConfig)

        assertEquals("myParam", result.parameters[0].name.value())
    }

    @Test
    fun sanitizeFieldCallExpressions() {
        val fn = Function(
            name = Name.of("getValue"),
            parameters = emptyList(),
            returnType = Type.String,
            body = listOf(
                ReturnStatement(
                    FieldCall(
                        receiver = VariableReference(Name.of("obj")),
                        field = Name.of("field-name"),
                    )
                )
            ),
        )

        val result = fn.sanitizeNames(javaLikeConfig)

        val returnStmt = result.body[0] as ReturnStatement
        val fieldCall = returnStmt.expression as FieldCall
        assertEquals("fieldName", fieldCall.field.value())
    }

    @Test
    fun sanitizeWithExtraStatementTransform() {
        val configWithExtra = javaLikeConfig.copy(
            extraStatementTransforms = { stmt, tr ->
                when (stmt) {
                    is VariableReference -> VariableReference(
                        name = Name(listOf(stmt.name.camelCase()))
                    )
                    else -> stmt.transformChildren(tr)
                }
            }
        )

        val fn = Function(
            name = Name.of("test"),
            parameters = emptyList(),
            returnType = Type.String,
            body = listOf(ReturnStatement(VariableReference(Name.of("some-var")))),
        )

        val result = fn.sanitizeNames(configWithExtra)

        val returnStmt = result.body[0] as ReturnStatement
        val varRef = returnStmt.expression as VariableReference
        assertEquals("someVar", varRef.name.value())
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :src:compiler:ir:allTests`
Expected: FAIL — `SanitizationConfig` and `sanitizeNames` not found

- [ ] **Step 3: Write the SanitizationConfig and sanitizeNames implementation**

Create `src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/emit/Sanitization.kt`:

```kotlin
package community.flock.wirespec.ir.emit

import community.flock.wirespec.ir.core.Element
import community.flock.wirespec.ir.core.FieldCall
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.Statement
import community.flock.wirespec.ir.core.Transformer
import community.flock.wirespec.ir.core.transform
import community.flock.wirespec.ir.core.transformChildren

data class SanitizationConfig(
    val reservedKeywords: Set<String>,
    val escapeKeyword: (String) -> String,
    val fieldNameCase: (Name) -> Name,
    val parameterNameCase: (Name) -> Name,
    val sanitizeSymbol: (String) -> String,
    val extraStatementTransforms: ((Statement, Transformer) -> Statement)? = null,
)

fun <T : Element> T.sanitizeNames(config: SanitizationConfig): T = transform {
    fields { field ->
        field.copy(name = config.sanitizeFieldName(field.name))
    }
    parameters { param ->
        val casedName = config.parameterNameCase(param.name)
        val sanitized = config.sanitizeSymbol(casedName.value())
        val escaped = if (sanitized in config.reservedKeywords) config.escapeKeyword(sanitized) else sanitized
        param.copy(name = Name(listOf(escaped)))
    }
    statementAndExpression { stmt, tr ->
        val extra = config.extraStatementTransforms
        when {
            stmt is FieldCall -> FieldCall(
                receiver = stmt.receiver?.let { tr.transformExpression(it) },
                field = config.sanitizeFieldName(stmt.field),
            )
            extra != null -> extra(stmt, tr)
            else -> stmt.transformChildren(tr)
        }
    }
}

private fun SanitizationConfig.sanitizeFieldName(name: Name): Name {
    val cased = fieldNameCase(name)
    val sanitized = sanitizeSymbol(cased.value())
    val escaped = if (sanitized in reservedKeywords) escapeKeyword(sanitized) else sanitized
    return Name(listOf(escaped))
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :src:compiler:ir:allTests`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/emit/Sanitization.kt \
       src/compiler/ir/src/commonTest/kotlin/community/flock/wirespec/ir/emit/SanitizationTest.kt
git commit -m "feat: extract shared SanitizationConfig and sanitizeNames"
```

---

### Task 2: Extract Import and Package Wrapping Helpers

**Files:**
- Create: `src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/emit/EmitHelpers.kt`

- [ ] **Step 1: Write EmitHelpers**

Create `src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/emit/EmitHelpers.kt`:

```kotlin
package community.flock.wirespec.ir.emit

import arrow.core.NonEmptyList
import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.ir.core.Element
import community.flock.wirespec.ir.core.File
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.Package
import community.flock.wirespec.compiler.core.emit.plus

fun File.wrapWithPackage(
    packageName: PackageName,
    definition: Definition,
    wirespecImport: Element,
    needsImport: Boolean,
    nameTransform: (Name) -> String = { it.pascalCase() },
): File {
    val subPackageName = packageName + definition
    return File(
        name = Name.of(subPackageName.toDir() + nameTransform(name)),
        elements = buildList {
            add(Package(subPackageName.value))
            if (needsImport) add(wirespecImport)
            addAll(elements)
        }
    )
}

fun File.wrapWithModuleImport(
    packageName: PackageName,
    definition: Definition,
    imports: List<Element>,
    nameTransform: (Name) -> String = { it.pascalCase() },
): File {
    val subPackageName = packageName + definition
    return File(
        name = Name.of(subPackageName.toDir() + nameTransform(name)),
        elements = imports + elements,
    )
}

fun NonEmptyList<File>.withSharedSource(
    emitShared: EmitShared,
    sharedFile: () -> File,
): NonEmptyList<File> = if (emitShared.value) this + sharedFile() else this
```

- [ ] **Step 2: Run IR tests to verify no compilation errors**

Run: `./gradlew :src:compiler:ir:allTests`
Expected: PASS (no existing tests break)

- [ ] **Step 3: Commit**

```bash
git add src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/emit/EmitHelpers.kt
git commit -m "feat: extract import and package wrapping helpers"
```

---

### Task 3: Extract Shared Client/Server Interface Builder

**Files:**
- Create: `src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/emit/SharedBuilder.kt`

- [ ] **Step 1: Write SharedBuilder**

Create `src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/emit/SharedBuilder.kt`:

```kotlin
package community.flock.wirespec.ir.emit

import community.flock.wirespec.ir.core.Element
import community.flock.wirespec.ir.core.Type
import community.flock.wirespec.ir.core.`interface`

enum class AccessorStyle {
    GETTER_METHODS,
    PROPERTIES,
}

fun buildClientServerInterfaces(style: AccessorStyle): List<Element> {
    val (pathTemplateName, methodName, clientFnName, serverFnName) = when (style) {
        AccessorStyle.GETTER_METHODS -> listOf("getPathTemplate", "getMethod", "getClient", "getServer")
        AccessorStyle.PROPERTIES -> listOf("pathTemplate", "method", "client", "server")
    }
    val useFields = style == AccessorStyle.PROPERTIES

    return listOf(
        `interface`("ServerEdge") {
            typeParam(type("Req"), type("Request", Type.Wildcard))
            typeParam(type("Res"), type("Response", Type.Wildcard))
            function("from") {
                returnType(type("Req"))
                arg("request", type("RawRequest"))
            }
            function("to") {
                returnType(type("RawResponse"))
                arg("response", type("Res"))
            }
        },
        `interface`("ClientEdge") {
            typeParam(type("Req"), type("Request", Type.Wildcard))
            typeParam(type("Res"), type("Response", Type.Wildcard))
            function("to") {
                returnType(type("RawRequest"))
                arg("request", type("Req"))
            }
            function("from") {
                returnType(type("Res"))
                arg("response", type("RawResponse"))
            }
        },
        `interface`("Client") {
            typeParam(type("Req"), type("Request", Type.Wildcard))
            typeParam(type("Res"), type("Response", Type.Wildcard))
            if (useFields) {
                field(pathTemplateName, Type.String)
                field(methodName, Type.String)
            } else {
                function(pathTemplateName) { returnType(Type.String) }
                function(methodName) { returnType(Type.String) }
            }
            function(clientFnName) {
                returnType(type("ClientEdge", type("Req"), type("Res")))
                arg("serialization", type("Serialization"))
            }
        },
        `interface`("Server") {
            typeParam(type("Req"), type("Request", Type.Wildcard))
            typeParam(type("Res"), type("Response", Type.Wildcard))
            if (useFields) {
                field(pathTemplateName, Type.String)
                field(methodName, Type.String)
            } else {
                function(pathTemplateName) { returnType(Type.String) }
                function(methodName) { returnType(Type.String) }
            }
            function(serverFnName) {
                returnType(type("ServerEdge", type("Req"), type("Res")))
                arg("serialization", type("Serialization"))
            }
        },
    )
}
```

- [ ] **Step 2: Run IR tests**

Run: `./gradlew :src:compiler:ir:allTests`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/emit/SharedBuilder.kt
git commit -m "feat: extract shared client/server interface builder"
```

---

### Task 4: Adopt Shared Utilities in JavaIrEmitter

**Files:**
- Modify: `src/compiler/emitters/java/src/commonMain/kotlin/community/flock/wirespec/emitters/java/JavaIrEmitter.kt`
- Test: `src/compiler/emitters/java/src/commonTest/kotlin/community/flock/wirespec/emitters/java/JavaIrEmitterTest.kt`

- [ ] **Step 1: Run Java tests to capture baseline**

Run: `./gradlew :src:compiler:emitters:java:allTests`
Expected: PASS (all existing tests pass)

- [ ] **Step 2: Define Java SanitizationConfig and adopt sanitizeNames(config)**

In `JavaIrEmitter.kt`, add the config and replace the private `sanitizeNames()` method.

Add this val inside the class body (after the `shared` val):

```kotlin
private val sanitizationConfig = SanitizationConfig(
    reservedKeywords = reservedKeywords,
    escapeKeyword = { "_$it" },
    fieldNameCase = { name ->
        val sanitized = if (name.parts.size > 1) name.camelCase() else name.value().sanitizeSymbol()
        Name(listOf(sanitized))
    },
    parameterNameCase = { name -> Name(listOf(name.camelCase().sanitizeSymbol())) },
    sanitizeSymbol = { it.sanitizeSymbol() },
    extraStatementTransforms = { stmt, tr ->
        when (stmt) {
            is FunctionCall -> if (stmt.name.value() == "validate") {
                stmt.copy(typeArguments = emptyList()).transformChildren(tr)
            } else stmt.transformChildren(tr)
            else -> stmt.transformChildren(tr)
        }
    },
)
```

Replace all calls to `.sanitizeNames()` (the private method) with `.sanitizeNames(sanitizationConfig)`.

Remove the old private `fun <T : Element> T.sanitizeNames(): T` method and the private `fun Name.sanitizeName(): Name` method.

Add import: `import community.flock.wirespec.ir.emit.sanitizeNames` and `import community.flock.wirespec.ir.emit.SanitizationConfig`.

- [ ] **Step 3: Adopt wrapWithPackage helper**

Replace the `emit(definition)` override body:

```kotlin
override fun emit(definition: Definition, module: Module, logger: Logger): File {
    val file = super.emit(definition, module, logger)
    return file.wrapWithPackage(
        packageName = packageName,
        definition = definition,
        wirespecImport = wirespecImport,
        needsImport = module.needImports(),
        nameTransform = { it.pascalCase().sanitizeSymbol() },
    )
}
```

Add import: `import community.flock.wirespec.ir.emit.wrapWithPackage`.

- [ ] **Step 4: Adopt withSharedSource helper**

Replace the `emit(module)` override body:

```kotlin
override fun emit(module: Module, logger: Logger): NonEmptyList<File> =
    super.emit(module, logger).withSharedSource(emitShared) {
        File(
            Name.of(PackageName("${DEFAULT_SHARED_PACKAGE_STRING}.java").toDir() + "Wirespec"),
            listOf(RawElement(shared.source))
        )
    }
```

Add import: `import community.flock.wirespec.ir.emit.withSharedSource`.

- [ ] **Step 5: Adopt shared buildClientServerInterfaces in shared source**

Replace the `clientServer` val in the `shared` object:

```kotlin
private val clientServer = buildClientServerInterfaces(AccessorStyle.GETTER_METHODS) + listOf(
    raw(
        """
        |public static Type getType(final Class<?> actualTypeArguments, final Class<?> rawType) {
        |  if(rawType != null) {
        |    return new ParameterizedType() {
        |      public Type getRawType() { return rawType; }
        |      public Type[] getActualTypeArguments() { return new Class<?>[]{actualTypeArguments}; }
        |      public Type getOwnerType() { return null; }
        |    };
        |  }
        |  else { return actualTypeArguments; }
        |}
        """.trimMargin(),
    ),
)
```

Add imports: `import community.flock.wirespec.ir.emit.AccessorStyle` and `import community.flock.wirespec.ir.emit.buildClientServerInterfaces`.

- [ ] **Step 6: Reorder endpoint emission pipeline**

Change `emit(endpoint)` so that enrichment happens before sanitization:

```kotlin
override fun emit(endpoint: Endpoint): File {
    val imports = endpoint.buildImports()
    return endpoint.convert()
        .injectHandleFunction(endpoint)
        .transformTypeDescriptors()
        .sanitizeNames(sanitizationConfig)
        .let { file ->
            if (imports.isNotEmpty()) {
                file.transform {
                    matchingElements { f: File ->
                        f.copy(elements = imports + f.elements)
                    }
                }
            } else {
                file
            }
        }
}
```

Note: `injectHandleFunction` and `transformTypeDescriptors` must now work with unsanitized names. Check that the `injectHandleFunction` code does not depend on already-sanitized field names. If it does, the handler struct construction uses literal strings (like `"getPathTemplate"`) and `Name.of(...)` which are independent of field sanitization — so this reorder is safe.

- [ ] **Step 7: Run Java tests to verify functional equivalence**

Run: `./gradlew :src:compiler:emitters:java:allTests`
Expected: PASS (all tests pass with functionally equivalent output)

- [ ] **Step 8: Commit**

```bash
git add src/compiler/emitters/java/src/commonMain/kotlin/community/flock/wirespec/emitters/java/JavaIrEmitter.kt
git commit -m "refactor: adopt shared utilities in JavaIrEmitter"
```

---

### Task 5: Adopt Shared Utilities in KotlinIrEmitter

**Files:**
- Modify: `src/compiler/emitters/kotlin/src/commonMain/kotlin/community/flock/wirespec/emitters/kotlin/KotlinIrEmitter.kt`
- Test: `src/compiler/emitters/kotlin/src/commonTest/kotlin/community/flock/wirespec/emitters/kotlin/KotlinIrEmitterTest.kt`

- [ ] **Step 1: Run Kotlin tests to capture baseline**

Run: `./gradlew :src:compiler:emitters:kotlin:allTests`
Expected: PASS

- [ ] **Step 2: Define Kotlin SanitizationConfig and adopt sanitizeNames(config)**

Add config inside the class body:

```kotlin
private val sanitizationConfig = SanitizationConfig(
    reservedKeywords = reservedKeywords,
    escapeKeyword = { it.addBackticks() },
    fieldNameCase = { name ->
        val sanitized = if (name.parts.size > 1) name.camelCase() else name.value().sanitizeSymbol()
        Name(listOf(sanitized))
    },
    parameterNameCase = { name -> Name(listOf(name.camelCase().sanitizeSymbol())) },
    sanitizeSymbol = { it.sanitizeSymbol() },
    extraStatementTransforms = { stmt, tr ->
        when (stmt) {
            is FunctionCall -> if (stmt.name.value() == "validate") {
                stmt.copy(typeArguments = emptyList()).transformChildren(tr)
            } else stmt.transformChildren(tr)
            is ConstructorStatement -> ConstructorStatement(
                type = tr.transformType(stmt.type),
                namedArguments = stmt.namedArguments.map { (name, expr) ->
                    sanitizationConfig.sanitizeFieldName(name) to tr.transformExpression(expr)
                }.toMap(),
            )
            else -> stmt.transformChildren(tr)
        }
    },
)
```

Note: The `sanitizationConfig` references itself in `extraStatementTransforms` — use a `lazy` val or extract the `sanitizeFieldName` logic into a standalone function. Since `SanitizationConfig` has a public `sanitizeFieldName` helper, reference it via the config.

Actually, looking at the implementation more carefully: the `sanitizeFieldName` is private in `Sanitization.kt`. We need to make it accessible. Update `Sanitization.kt` to expose it:

In `Sanitization.kt`, change `private fun SanitizationConfig.sanitizeFieldName` to:

```kotlin
fun SanitizationConfig.sanitizeFieldName(name: Name): Name {
    val cased = fieldNameCase(name)
    val sanitized = sanitizeSymbol(cased.value())
    val escaped = if (sanitized in reservedKeywords) escapeKeyword(sanitized) else sanitized
    return Name(listOf(escaped))
}
```

Then in `KotlinIrEmitter.kt`, use `by lazy`:

```kotlin
private val sanitizationConfig: SanitizationConfig by lazy {
    SanitizationConfig(
        reservedKeywords = reservedKeywords,
        escapeKeyword = { it.addBackticks() },
        fieldNameCase = { name ->
            val sanitized = if (name.parts.size > 1) name.camelCase() else name.value().sanitizeSymbol()
            Name(listOf(sanitized))
        },
        parameterNameCase = { name -> Name(listOf(name.camelCase().sanitizeSymbol())) },
        sanitizeSymbol = { it.sanitizeSymbol() },
        extraStatementTransforms = { stmt, tr ->
            when (stmt) {
                is FunctionCall -> if (stmt.name.value() == "validate") {
                    stmt.copy(typeArguments = emptyList()).transformChildren(tr)
                } else stmt.transformChildren(tr)
                is ConstructorStatement -> ConstructorStatement(
                    type = tr.transformType(stmt.type),
                    namedArguments = stmt.namedArguments.map { (name, expr) ->
                        sanitizationConfig.sanitizeFieldName(name) to tr.transformExpression(expr)
                    }.toMap(),
                )
                else -> stmt.transformChildren(tr)
            }
        },
    )
}
```

Replace all `.sanitizeNames()` calls with `.sanitizeNames(sanitizationConfig)`. Remove the old private `sanitizeNames` and `sanitizeName` methods.

Add imports: `import community.flock.wirespec.ir.emit.sanitizeNames`, `import community.flock.wirespec.ir.emit.SanitizationConfig`, `import community.flock.wirespec.ir.emit.sanitizeFieldName`.

- [ ] **Step 3: Adopt wrapWithPackage and withSharedSource helpers**

Replace `emit(definition)`:

```kotlin
override fun emit(definition: Definition, module: Module, logger: Logger): File {
    val file = super.emit(definition, module, logger)
    return file.wrapWithPackage(
        packageName = packageName,
        definition = definition,
        wirespecImport = RawElement(wirespecImport),
        needsImport = module.needImports(),
    )
}
```

Replace `emit(module)`:

```kotlin
override fun emit(module: Module, logger: Logger): NonEmptyList<File> =
    super.emit(module, logger).withSharedSource(emitShared) {
        File(
            Name.of(PackageName("${DEFAULT_SHARED_PACKAGE_STRING}.kotlin").toDir() + "Wirespec"),
            listOf(RawElement(shared.source))
        )
    }
```

Add imports: `import community.flock.wirespec.ir.emit.wrapWithPackage`, `import community.flock.wirespec.ir.emit.withSharedSource`.

- [ ] **Step 4: Adopt shared buildClientServerInterfaces**

Replace the `clientServer` val in the `shared` object:

```kotlin
private val clientServer = buildClientServerInterfaces(AccessorStyle.PROPERTIES)
```

Add imports: `import community.flock.wirespec.ir.emit.AccessorStyle`, `import community.flock.wirespec.ir.emit.buildClientServerInterfaces`.

- [ ] **Step 5: Replace raw companion object with IR DSL**

Replace the `companionObject` method (lines 333-354) with an IR DSL version. Create a `buildCompanionObject` method:

```kotlin
private fun buildCompanionObject(endpoint: Endpoint): Namespace {
    val pathTemplate = "/" + endpoint.path.joinToString("/") {
        when (it) {
            is Endpoint.Segment.Literal -> it.value
            is Endpoint.Segment.Param -> "{${it.identifier.value}}"
        }
    }
    return namespace("companion") {
        struct("Companion") {
            implements(type("Wirespec.Server", type("Request"), type("Response", LanguageType.Wildcard)))
            implements(type("Wirespec.Client", type("Request"), type("Response", LanguageType.Wildcard)))
            field("pathTemplate", LanguageType.String, isOverride = true)
            field("method", LanguageType.String, isOverride = true)
            function("server", isOverride = true) {
                returnType(type("Wirespec.ServerEdge", type("Request"), type("Response", LanguageType.Wildcard)))
                arg("serialization", type("Wirespec.Serialization"))
            }
            function("client", isOverride = true) {
                returnType(type("Wirespec.ClientEdge", type("Request"), type("Response", LanguageType.Wildcard)))
                arg("serialization", type("Wirespec.Serialization"))
            }
        }
    }
}
```

**Important:** If the Kotlin generator cannot produce the exact companion object syntax from this IR, keep the existing raw string approach but rename the method to `buildCompanionObject` for naming consistency. The goal is naming alignment — the raw string is acceptable here if the IR DSL can't express Kotlin companion objects.

Verify by checking the test output. If the generated output differs in a way that breaks functional equivalence, revert to the raw approach with the renamed method.

- [ ] **Step 6: Reorder endpoint emission pipeline**

Change `emit(endpoint)` so enrichment happens before sanitization:

```kotlin
override fun emit(endpoint: Endpoint): File {
    val imports = endpoint.buildImports()
    val file = endpoint.convert()
    val endpointNamespace = file.findElement<Namespace>()!!
    val body = endpointNamespace.injectCompanionObject(endpoint)
    val sanitized = LanguageFile(Name.of(endpoint.identifier.sanitize()), listOf(body))
        .sanitizeNames(sanitizationConfig)
    return if (imports.isNotEmpty()) {
        sanitized.copy(elements = listOf(RawElement(imports)) + sanitized.elements)
    } else {
        sanitized
    }
}
```

- [ ] **Step 7: Run Kotlin tests to verify functional equivalence**

Run: `./gradlew :src:compiler:emitters:kotlin:allTests`
Expected: PASS

- [ ] **Step 8: Commit**

```bash
git add src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/emit/Sanitization.kt \
       src/compiler/emitters/kotlin/src/commonMain/kotlin/community/flock/wirespec/emitters/kotlin/KotlinIrEmitter.kt
git commit -m "refactor: adopt shared utilities in KotlinIrEmitter"
```

---

### Task 6: Adopt Shared Utilities in ScalaIrEmitter

**Files:**
- Modify: `src/compiler/emitters/scala/src/commonMain/kotlin/community/flock/wirespec/emitters/scala/ScalaIrEmitter.kt`
- Test: `src/compiler/emitters/scala/src/commonTest/kotlin/community/flock/wirespec/emitters/scala/ScalaIrEmitterTest.kt`

- [ ] **Step 1: Run Scala tests to capture baseline**

Run: `./gradlew :src:compiler:emitters:scala:allTests`
Expected: PASS

- [ ] **Step 2: Define Scala SanitizationConfig and adopt sanitizeNames(config)**

Add config inside the class body:

```kotlin
private val sanitizationConfig: SanitizationConfig by lazy {
    SanitizationConfig(
        reservedKeywords = reservedKeywords,
        escapeKeyword = { it.addBackticks() },
        fieldNameCase = { name ->
            val sanitized = if (name.parts.size > 1) name.camelCase() else name.value().sanitizeSymbol()
            Name(listOf(sanitized))
        },
        parameterNameCase = { name -> Name(listOf(name.camelCase().sanitizeSymbol())) },
        sanitizeSymbol = { it.sanitizeSymbol() },
        extraStatementTransforms = { stmt, tr ->
            when (stmt) {
                is FunctionCall -> if (stmt.name.value() == "validate") {
                    stmt.copy(typeArguments = emptyList()).transformChildren(tr)
                } else stmt.transformChildren(tr)
                is ConstructorStatement -> ConstructorStatement(
                    type = tr.transformType(stmt.type),
                    namedArguments = stmt.namedArguments.map { (name, expr) ->
                        sanitizationConfig.sanitizeFieldName(name) to tr.transformExpression(expr)
                    }.toMap(),
                )
                else -> stmt.transformChildren(tr)
            }
        },
    )
}
```

Replace all `.sanitizeNames()` calls with `.sanitizeNames(sanitizationConfig)`. Remove old private `sanitizeNames` and `sanitizeName` methods.

- [ ] **Step 3: Adopt wrapWithPackage, withSharedSource, and buildClientServerInterfaces**

Same pattern as Kotlin (Task 5 steps 3-4). Replace `emit(definition)`, `emit(module)`, and `clientServer` val.

- [ ] **Step 4: Replace raw Client/Server objects with IR DSL**

Replace the `withClientServerObjects` method with a `buildClientServerObjects` that uses IR DSL:

```kotlin
private fun buildClientServerObjects(endpoint: Endpoint, requestIsObject: Boolean): List<Element> {
    val reqType = if (requestIsObject) "Request.type" else "Request"
    val pathTemplate = "/" + endpoint.path.joinToString("/") {
        when (it) {
            is Endpoint.Segment.Literal -> it.value
            is Endpoint.Segment.Param -> "{${it.identifier.value}}"
        }
    }

    val clientObject = struct("Client") {
        implements(type("Wirespec.Client", type(reqType), type("Response", LanguageType.Wildcard)))
        field("pathTemplate", LanguageType.String, isOverride = true)
        field("method", LanguageType.String, isOverride = true)
        function("client", isOverride = true) {
            returnType(type("Wirespec.ClientEdge", type(reqType), type("Response", LanguageType.Wildcard)))
            arg("serialization", type("Wirespec.Serialization"))
        }
    }

    val serverObject = struct("Server") {
        implements(type("Wirespec.Server", type(reqType), type("Response", LanguageType.Wildcard)))
        field("pathTemplate", LanguageType.String, isOverride = true)
        field("method", LanguageType.String, isOverride = true)
        function("server", isOverride = true) {
            returnType(type("Wirespec.ServerEdge", type(reqType), type("Response", LanguageType.Wildcard)))
            arg("serialization", type("Wirespec.Serialization"))
        }
    }

    return listOf(clientObject, serverObject)
}
```

**Same caveat as Kotlin:** If the Scala generator cannot produce the exact `object Client extends...` syntax from struct IR, keep the raw strings but rename to `buildClientServerObjects`. Check test output.

Update `emit(endpoint)` to use the new method and reorder pipeline:

```kotlin
override fun emit(endpoint: Endpoint): File {
    val imports = endpoint.buildImports()
    val file = endpoint.convert()
    val endpointNamespace = file.findElement<Namespace>()!!
    val flattened = endpointNamespace.flattenNestedStructs()
    val requestIsObject = isRequestObject(flattened)
    val body = flattened
        .injectHandleFunction()
        .let { ns -> ns.copy(elements = ns.elements + buildClientServerObjects(endpoint, requestIsObject)) }
    val sanitized = LanguageFile(Name.of(endpoint.identifier.sanitize()), listOf(body))
        .sanitizeNames(sanitizationConfig)
    return if (imports.isNotEmpty()) sanitized.copy(elements = listOf(RawElement(imports)) + sanitized.elements)
    else sanitized
}
```

- [ ] **Step 5: Run Scala tests to verify functional equivalence**

Run: `./gradlew :src:compiler:emitters:scala:allTests`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add src/compiler/emitters/scala/src/commonMain/kotlin/community/flock/wirespec/emitters/scala/ScalaIrEmitter.kt
git commit -m "refactor: adopt shared utilities in ScalaIrEmitter"
```

---

### Task 7: Adopt Shared Utilities in PythonIrEmitter

**Files:**
- Modify: `src/compiler/emitters/python/src/commonMain/kotlin/community/flock/wirespec/emitters/python/PythonIrEmitter.kt`
- Test: `src/compiler/emitters/python/src/commonTest/kotlin/community/flock/wirespec/emitters/python/PythonIrEmitterTest.kt`

- [ ] **Step 1: Run Python tests to capture baseline**

Run: `./gradlew :src:compiler:emitters:python:allTests`
Expected: PASS

- [ ] **Step 2: Define Python SanitizationConfig and adopt sanitizeNames(config)**

```kotlin
private val sanitizationConfig = SanitizationConfig(
    reservedKeywords = reservedKeywords,
    escapeKeyword = { "_$it" },
    fieldNameCase = { name ->
        val sanitized = if (name.parts.size > 1) name.camelCase() else name.value()
        Name(listOf(sanitized))
    },
    parameterNameCase = { name -> Name(listOf(name.camelCase())) },
    sanitizeSymbol = { it },
    extraStatementTransforms = { stmt, tr ->
        when (stmt) {
            is ConstructorStatement -> ConstructorStatement(
                type = tr.transformType(stmt.type),
                namedArguments = stmt.namedArguments
                    .map { (k, v) -> sanitizationConfig.sanitizeFieldName(k) to tr.transformExpression(v) }
                    .toMap(),
            )
            else -> stmt.transformChildren(tr)
        }
    },
)
```

Note: Python's `sanitizeSymbol` is identity (no symbol stripping needed). Python uses `_keyword` escaping.

Replace all `.sanitizeNames()` calls with `.sanitizeNames(sanitizationConfig)`. Remove old private methods.

- [ ] **Step 3: Adopt wrapWithModuleImport helper**

Replace `emit(definition)`:

```kotlin
override fun emit(definition: Definition, module: Module, logger: Logger): File {
    val file = super.emit(definition, module, logger)
    return file.wrapWithModuleImport(
        packageName = packageName,
        definition = definition,
        imports = buildImports("..wirespec"),
    )
}
```

- [ ] **Step 4: Reorder endpoint emission pipeline**

```kotlin
override fun emit(endpoint: Endpoint): File {
    val endpointImports = endpoint.importReferences().distinctBy { it.value }
        .map { Import("..model.${it.value}", LanguageType.Custom(it.value)) }
    val converted = endpoint.convert().findElement<Namespace>()!!
    val flattened = converted.flattenNestedStructs()
    val (moduleElements, classElements) = flattened.elements.partition { it is Struct || it is LanguageUnion }
    val endpointClass = Namespace(
        name = converted.name,
        elements = classElements,
        extends = converted.extends,
    )
    return LanguageFile(converted.name, buildList {
        addAll(endpointImports)
        addAll(moduleElements)
        add(endpointClass)
    })
        .snakeCaseHandlerAndCallMethods()
        .sanitizeNames(sanitizationConfig)
}
```

- [ ] **Step 5: Run Python tests to verify functional equivalence**

Run: `./gradlew :src:compiler:emitters:python:allTests`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add src/compiler/emitters/python/src/commonMain/kotlin/community/flock/wirespec/emitters/python/PythonIrEmitter.kt
git commit -m "refactor: adopt shared utilities in PythonIrEmitter"
```

---

### Task 8: Adopt Shared Utilities in TypeScriptIrEmitter

**Files:**
- Modify: `src/compiler/emitters/typescript/src/commonMain/kotlin/community/flock/wirespec/emitters/typescript/TypeScriptIrEmitter.kt`
- Test: `src/compiler/emitters/typescript/src/commonTest/kotlin/community/flock/wirespec/emitters/typescript/TypeScriptIrEmitterTest.kt`

- [ ] **Step 1: Run TypeScript tests to capture baseline**

Run: `./gradlew :src:compiler:emitters:typescript:allTests`
Expected: PASS

- [ ] **Step 2: Define TypeScript SanitizationConfig and adopt sanitizeNames(config)**

```kotlin
private val sanitizationConfig = SanitizationConfig(
    reservedKeywords = reservedKeywords,
    escapeKeyword = { "_$it" },
    fieldNameCase = { name ->
        val sanitized = if (name.parts.size > 1) name.camelCase() else name.value().sanitizeSymbol()
        Name(listOf(sanitized))
    },
    parameterNameCase = { name ->
        val sanitized = if (name.parts.size > 1) name.camelCase() else name.value().sanitizeSymbol()
        Name(listOf(sanitized))
    },
    sanitizeSymbol = { it.filter { ch -> ch.isLetterOrDigit() || ch == '_' } },
    extraStatementTransforms = { stmt, tr ->
        when (stmt) {
            is VariableReference -> VariableReference(
                name = sanitizationConfig.sanitizeFieldName(stmt.name),
            )
            is ConstructorStatement -> ConstructorStatement(
                type = tr.transformType(stmt.type),
                namedArguments = stmt.namedArguments.map { (key, value) ->
                    sanitizationConfig.sanitizeFieldName(key) to tr.transformExpression(value)
                }.toMap(),
            )
            is Assignment -> Assignment(
                name = sanitizationConfig.sanitizeFieldName(stmt.name),
                value = tr.transformExpression(stmt.value),
                isProperty = stmt.isProperty,
            )
            else -> stmt.transformChildren(tr)
        }
    },
)
```

Replace all `.sanitizeNames()` calls with `.sanitizeNames(sanitizationConfig)`. Remove old private methods.

- [ ] **Step 3: Extract buildApiConst for endpoint**

Extract the raw api string into a named method:

```kotlin
private fun buildApiConst(endpoint: Endpoint): RawElement {
    val apiName = endpoint.identifier.value.firstToLower()
    val method = endpoint.method.name
    val pathString = endpoint.path.joinToString("/") {
        when (it) {
            is Endpoint.Segment.Literal -> it.value
            is Endpoint.Segment.Param -> "{${it.identifier.value}}"
        }
    }
    return raw("""
        |export const client:Wirespec.Client<Request, Response> = (serialization: Wirespec.Serialization) => ({
        |  from: (it) => fromRawResponse(serialization, it),
        |  to: (it) => toRawRequest(serialization, it)
        |})
        |export const server:Wirespec.Server<Request, Response> = (serialization: Wirespec.Serialization) => ({
        |  from: (it) => fromRawRequest(serialization, it),
        |  to: (it) => toRawResponse(serialization, it)
        |})
        |export const api = {
        |  name: "$apiName",
        |  method: "$method",
        |  path: "$pathString",
        |  server,
        |  client
        |} as const
    """.trimMargin())
}
```

- [ ] **Step 4: Reorder endpoint emission pipeline**

```kotlin
override fun emit(endpoint: Endpoint): File {
    val imports = endpoint.importReferences().distinctBy { it.value }
        .joinToString("\n") { "import {type ${it.value}} from '../model'" }

    val hasRequestParams = endpoint.requestParameters().isNotEmpty()
    val endpointNamespace = endpoint.convert()
        .transform {
            statement { stmt, transformer ->
                // error message trimming
                when (stmt) {
                    is Switch -> stmt.copy(
                        default = stmt.default?.map { s ->
                            if (s is ErrorStatement && s.message is BinaryOp) {
                                val binary = s.message as BinaryOp
                                val literal = binary.left as? Literal
                                if (literal != null) ErrorStatement(Literal(literal.value.toString().trimEnd(' '), literal.type))
                                else s
                            } else s
                        }
                    ).transformChildren(transformer)
                    else -> stmt.transformChildren(transformer)
                }
            }
        }
        .transform { apply(transformPatternSwitchToValueSwitch()) }
        .transform {
            if (hasRequestParams) {
                matchingElements { iface: community.flock.wirespec.ir.core.Interface ->
                    if (iface.name == Name.of("Call")) {
                        iface.copy(
                            elements = iface.elements.map { element ->
                                if (element is community.flock.wirespec.ir.core.Function) {
                                    element.copy(
                                        parameters = listOf(
                                            Parameter(Name.of("params"), LanguageType.Custom("RequestParams"))
                                        )
                                    )
                                } else element
                            }
                        )
                    } else iface
                }
            }
        }
        .findElement<Namespace>()!!
    val body = endpointNamespace
        .transform { injectAfter { _: Namespace -> listOf(buildApiConst(endpoint)) } }
    val sanitized = File(Name.of(endpoint.identifier.sanitize()), listOf(body))
        .sanitizeNames(sanitizationConfig)
    return if (imports.isNotEmpty()) sanitized.copy(elements = listOf(RawElement(imports)) + sanitized.elements)
    else sanitized
}
```

- [ ] **Step 5: Run TypeScript tests to verify functional equivalence**

Run: `./gradlew :src:compiler:emitters:typescript:allTests`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add src/compiler/emitters/typescript/src/commonMain/kotlin/community/flock/wirespec/emitters/typescript/TypeScriptIrEmitter.kt
git commit -m "refactor: adopt shared utilities in TypeScriptIrEmitter"
```

---

### Task 9: Adopt Shared Utilities in RustIrEmitter

**Files:**
- Modify: `src/compiler/emitters/rust/src/commonMain/kotlin/community/flock/wirespec/emitters/rust/RustIrEmitter.kt`
- Test: `src/compiler/emitters/rust/src/commonTest/kotlin/community/flock/wirespec/emitters/rust/RustIrEmitterTest.kt`

- [ ] **Step 1: Run Rust tests to capture baseline**

Run: `./gradlew :src:compiler:emitters:rust:allTests`
Expected: PASS

- [ ] **Step 2: Define Rust SanitizationConfig and adopt sanitizeNames(config)**

```kotlin
private val sanitizationConfig = SanitizationConfig(
    reservedKeywords = reservedKeywords,
    escapeKeyword = { "r#$it" },
    fieldNameCase = { name -> Name.of(Name(name.parts).snakeCase()) },
    parameterNameCase = { name ->
        val value = name.value()
        if (value == "self" || value == "&self") name
        else Name.of(Name(name.parts).snakeCase())
    },
    sanitizeSymbol = { it },
    extraStatementTransforms = { stmt, tr ->
        when (stmt) {
            is ConstructorStatement -> ConstructorStatement(
                type = tr.transformType(stmt.type),
                namedArguments = stmt.namedArguments
                    .map { (k, v) -> sanitizationConfig.sanitizeFieldName(k) to tr.transformExpression(v) }
                    .toMap(),
            )
            else -> stmt.transformChildren(tr)
        }
    },
)
```

Note: Rust's parameter sanitization must skip `self`/`&self` params. Handle this in `parameterNameCase`.

Replace all `.sanitizeNames()` calls with `.sanitizeNames(sanitizationConfig)`. Remove old private methods.

- [ ] **Step 3: Adopt wrapWithModuleImport helper**

Replace `emit(definition)`:

```kotlin
override fun emit(definition: Definition, module: Module, logger: Logger): File {
    val importHeader = when (definition) {
        is Endpoint -> endpointImport
        else -> modelImport
    }
    val file = super.emit(definition, module, logger)
    val subPackageName = packageName + definition
    return File(
        name = Name.of(subPackageName.toDir() + file.name.pascalCase().toSnakeCase()),
        elements = listOf(RawElement(importHeader)) + file.elements.flatMap { element ->
            if (element is Struct) listOf(RawElement("#[derive(Debug, Clone, Default, PartialEq)]"), element)
            else listOf(element)
        }
    )
}
```

Note: Rust's `emit(definition)` has extra logic (derive injection, snake_case naming) that doesn't fit the generic helper cleanly. Keep the Rust-specific implementation but align the naming/structure.

- [ ] **Step 4: Break apart rustifyEndpoint into named steps**

Extract the monolithic `rustifyEndpoint` into named methods:

```kotlin
private fun File.stripHandlerExtends(): File = transform {
    matchingElements<Interface> { iface ->
        if (iface.name == Name.of("Handler") || iface.name == Name.of("Call")) iface.copy(extends = emptyList()) else iface
    }
}

private fun File.stripResponseGenerics(): File = transform {
    matching<LanguageType.Custom> { type ->
        if (type.name.startsWith("Response") && type.generics.isNotEmpty()) type.copy(generics = emptyList()) else type
    }
}

private fun File.injectSelfToHandlerMethods(): File = transform {
    matchingElements<Interface> { iface ->
        if (iface.name == Name.of("Handler") || iface.name == Name.of("Call")) {
            iface.transform {
                matchingElements { fn: LanguageFunction ->
                    fn.copy(
                        name = Name.of(fn.name.snakeCase()),
                        parameters = listOf(selfParam) + fn.parameters,
                    )
                }
            }
        } else iface
    }
}

private fun File.injectHandlerImplForClient(endpoint: Endpoint): File = transform {
    matchingElements<Namespace> { ns ->
        val handler = ns.elements.filterIsInstance<Interface>().firstOrNull { it.name == Name.of("Handler") }
        if (handler != null) {
            val method = handler.elements.filterIsInstance<LanguageFunction>().firstOrNull()
            if (method != null) {
                val methodName = method.name.snakeCase()
                ns.copy(elements = ns.elements + listOf(buildHandlerImpl(methodName)))
            } else ns
        } else ns
    }
}

private fun buildHandlerImpl(methodName: String): RawElement = RawElement("""
    impl<C: Client> Handler for C {
        async fn $methodName(&self, request: Request) -> Response {
            let raw = to_raw_request(self.serialization(), request);
            let resp = self.transport().transport(&raw).await;
            from_raw_response(self.serialization(), resp)
        }
    }
""".trimIndent())

private fun buildApiStruct(endpoint: Endpoint): RawElement = RawElement(endpoint.generateApiStruct())

private fun File.injectResponseFromImpls(): File = transform {
    matchingElements<LanguageFile> { file ->
        file.copy(elements = file.elements.flatMap { element ->
            if (element is LanguageUnion && element.name.pascalCase() == "Response" && element.members.isNotEmpty()) {
                listOf(element) + element.members.map { member ->
                    RawElement("impl From<${member.name}> for Response { fn from(value: ${member.name}) -> Self { Response::${member.name}(value) } }\n")
                }
            } else listOf(element)
        })
    }
}
```

Then rewrite `emit(endpoint)`:

```kotlin
override fun emit(endpoint: Endpoint): File =
    endpoint.convert()
        .flattenForRust()
        .stripWirespecPrefix()
        .stripHandlerExtends()
        .stripResponseGenerics()
        .transform { apply(fixResponseSwitchPatterns()) }
        .transform { apply(fixConstructorCalls()) }
        .transform { apply(borrowSerializationArgs()) }
        .transform {
            parametersWhere(
                predicate = { (it.type as? LanguageType.Custom)?.name in setOf("Serializer", "Deserializer") },
                transform = { it.copy(type = (it.type as LanguageType.Custom).borrowImpl()) },
            )
        }
        .injectSelfToHandlerMethods()
        .injectHandlerImplForClient(endpoint)
        .injectResponseFromImpls()
        .transform {
            matchingElements<Namespace> { ns ->
                ns.copy(elements = ns.elements + listOf(buildApiStruct(endpoint)))
            }
        }
        .sanitizeNames(sanitizationConfig)
        .prependImports(endpoint.buildEndpointImports())
```

- [ ] **Step 5: Run Rust tests to verify functional equivalence**

Run: `./gradlew :src:compiler:emitters:rust:allTests`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add src/compiler/emitters/rust/src/commonMain/kotlin/community/flock/wirespec/emitters/rust/RustIrEmitter.kt
git commit -m "refactor: adopt shared utilities in RustIrEmitter"
```

---

### Task 10: Clean Up — Remove Dead Code and Run Full Test Suite

**Files:**
- All 6 emitter files (remove any remaining dead private methods)

- [ ] **Step 1: Scan for dead private methods in each emitter**

In each emitter file, check for private methods that were replaced by shared utilities but not yet removed:
- Old `sanitizeNames()` methods
- Old `sanitizeName()` methods
- Any now-unused helper methods

Remove them.

- [ ] **Step 2: Run full test suite**

Run: `./gradlew :src:compiler:emitters:java:allTests :src:compiler:emitters:kotlin:allTests :src:compiler:emitters:scala:allTests :src:compiler:emitters:python:allTests :src:compiler:emitters:typescript:allTests :src:compiler:emitters:rust:allTests :src:compiler:ir:allTests`
Expected: ALL PASS

- [ ] **Step 3: Commit**

```bash
git add -A src/compiler/emitters/ src/compiler/ir/
git commit -m "refactor: clean up dead code after harmonization"
```
