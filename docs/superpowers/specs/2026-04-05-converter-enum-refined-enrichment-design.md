# Enrich IrConverter: Enum Entry Values and Refined toString

## Goal

Move two universal patterns from language emitters into `IrConverter.kt`, reducing duplicated transforms across Java, Kotlin, Scala, Python, and Rust emitters.

Scope is limited to patterns where all emitters (except TypeScript, which architecturally diverges) perform the same structural IR modification.

## Change 1: Enum Entry String Values

### Current state

`EnumWirespec.convert()` in `IrConverter.kt:409-413` produces entries with no values:

```kotlin
fun EnumWirespec.convert() = file(identifier.toName()) {
    enum(identifier.toName(), Type.Custom("Wirespec.Enum")) {
        entries.forEach { entry(it) }  // Entry(name, emptyList())
    }
}
```

Then 5/6 emitters add `listOf("\"${original}\"")` as the entry value:
- Java, Kotlin, Scala: via `withLabelField()` which overwrites entries
- Python, Rust: via `matchingElements<Enum>` transform that maps entries

TypeScript ignores entry values entirely (its generator uses `it.name.value()` directly in `TypeScriptGenerator.kt:161`).

### Change

Add the quoted-string value in the converter:

```kotlin
fun EnumWirespec.convert() = file(identifier.toName()) {
    enum(identifier.toName(), Type.Custom("Wirespec.Enum")) {
        entries.forEach { entry(it, "\"$it\"") }
    }
}
```

### Impact per emitter

| Emitter | Impact |
|---------|--------|
| Java | `withLabelField()` no longer needs to rewrite entry values (it overwrites anyway, so this is safe even without changes) |
| Kotlin | Same as Java |
| Scala | Same as Java |
| Python | **Remove the `matchingElements<Enum>` transform body** that maps entries to add values. Entry name sanitization (`sanitizeEnum()`, `sanitizeKeywords()`) must still happen, either in `sanitizeNames()` or a simpler entry-name-only transform. |
| Rust | **Remove the `matchingElements<Enum>` transform body** that maps entries. Same caveat about entry name sanitization. |
| TypeScript | No change needed. Generator ignores entry values. |

### Simplification of `withLabelField()`

The `withLabelField()` function in `Dsl.kt:768-790` currently rewrites entry values:
```kotlin
entries = entries.map {
    Enum.Entry(Name.of(sanitizeEntry(it.name.value())), listOf("\"${it.name.value()}\""))
}
```

After this change, entry values are already present. `withLabelField()` can be simplified to only handle entry name sanitization:
```kotlin
entries = entries.map {
    it.copy(name = Name.of(sanitizeEntry(it.name.value())))
}
```

## Change 2: Refined toString Function

### Current state

`RefinedWirespec.convert()` in `IrConverter.kt:421-430` produces a struct with only `validate()`:

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

Then 4/5 emitters add a toString function from scratch:
- Java: `value.toString()` (FunctionCall)
- Kotlin: `value` for String refs, `value.toString()` for others (RawExpression)
- Python: `self.value` for String, `str(self.value)` for others (RawExpression with self receiver)
- Rust: `self.value.clone()` for String, `format!("{}", self.value)` for others (RawExpression with &self)

TypeScript ignores the struct entirely (uses type alias + standalone validator function).

### Change

Add a toString function to the converter output. Use `VariableReference("value")` for String references, `FunctionCall(value, "toString")` for others:

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
                if (reference.isStringPrimitive())
                    VariableReference(Name.of("value"))
                else
                    FunctionCall(
                        receiver = VariableReference(Name.of("value")),
                        name = Name.of("toString")
                    )
            )
        }
    }
}
```

A helper `Reference.isStringPrimitive()` checks if the reference type is `Primitive(Type.String)`.

### Impact per emitter

| Emitter | Impact |
|---------|--------|
| Java | Remove manual toString injection in `emit(refined)`. Keep `isOverride` marking and `value()` accessor addition. |
| Kotlin | Remove manual toString construction. Keep `isOverride` field marking. May still transform expression to `RawExpression`. |
| Scala | Similar to Kotlin. |
| Python | Remove `__str__` construction. Still needs to: rename to `__str__`, add `self` parameter, transform `VariableReference("value")` to `FieldCall(self, "value")`. |
| Rust | Remove `buildToStringFunction()`. Still needs to: rename to `to_string`, add `&self`, transform expression for Rust semantics. |
| TypeScript | No change. Ignores the struct entirely. |

## What does NOT change

- `withLabelField()` continues to exist for Java/Kotlin/Scala (they need label field, constructor, override semantics)
- `sanitizeNames()` stays in each emitter (language-specific keywords and case conventions)
- TypeScript remains independent (type aliases and standalone functions)
- Endpoint, Type, Union, Channel conversion unchanged

## Risk assessment

- **Low risk**: Both changes add data that was previously absent. No existing data is removed.
- **TypeScript safety**: Confirmed TypeScript enum generator uses `it.name.value()`, not entry values.
- **withLabelField safety**: It overwrites entry values anyway, so Java/Kotlin/Scala work correctly even before they're updated to use the new values.
- **Refined safety**: Emitters that replace struct elements (Kotlin, Python, Rust) overwrite the converter's output, so they work correctly even before simplification.

## Files to modify

| File | Change |
|------|--------|
| `src/compiler/ir/.../converter/IrConverter.kt` | Add entry values to enum, add toString to refined |
| `src/compiler/ir/.../core/Dsl.kt` | Simplify `withLabelField()` entry value handling |
| `src/compiler/emitters/python/.../PythonIrEmitter.kt` | Remove enum entry value transform |
| `src/compiler/emitters/rust/.../RustIrEmitter.kt` | Remove enum entry value transform |
| `src/compiler/emitters/java/.../JavaIrEmitter.kt` | Remove refined toString injection |
| `src/compiler/emitters/kotlin/.../KotlinIrEmitter.kt` | Remove refined toString construction |
| `src/compiler/emitters/scala/.../ScalaIrEmitter.kt` | Remove refined toString construction |
| `src/compiler/emitters/python/.../PythonIrEmitter.kt` | Remove `__str__` construction, adapt converter's toString |
| `src/compiler/emitters/rust/.../RustIrEmitter.kt` | Remove `buildToStringFunction()`, adapt converter's toString |
| Emitter test files | Update expected IR snapshots if any IR-level tests exist |
