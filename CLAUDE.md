# Wirespec - Claude Code Instructions

## IR Emitter Pipeline

The Wirespec compiler uses a four-stage IR pipeline to generate idiomatic code for Java, Kotlin, TypeScript, Python, and Rust:

```
Wirespec Source
    │
    ▼
Parser ──► AST (Root / Module / Definition)
    │
    ▼
IrConverter ──► IR File (language-neutral tree)
    │
    ▼
Language Emitter ──► Transformed IR File (via Transform DSL)
    │
    ▼
CodeGenerator ──► String (target-language source code)
```

### Stage 1: Parser AST

The parser produces a tree of `Definition` nodes grouped into `Module`s inside a `Root`:

```
Root ─► Module[] ─► Definition[]
```

**Definition types** (sealed hierarchy):
- `Type` — record/struct with a `Shape` (list of `Field`s) and optional `extends`
- `Enum` — set of string entries
- `Union` — set of `Reference` entries
- `Refined` — primitive wrapper with a regex/bound constraint
- `Endpoint` — HTTP endpoint (method, path, queries, headers, requests, responses)
- `Channel` — async messaging channel with a reference type

**Reference type system** (`sealed interface Reference`):
`Any`, `Unit`, `Custom(name)`, `Primitive(type)`, `Iterable(reference)`, `Dict(reference)`. Each carries `isNullable`. `Primitive.Type` variants: `String`, `Integer`, `Number`, `Boolean`, `Bytes` (with optional precision and constraints).

Key files:
- `src/compiler/core/.../parse/ast/Definition.kt`
- `src/compiler/core/.../parse/ast/Reference.kt`
- `src/compiler/core/.../parse/ast/Root.kt`

### Stage 2: Convert (Parser AST → IR)

`IrConverter.kt` maps each parser `Definition` to an IR `File` tree. The entry point dispatches by definition type:

```kotlin
fun DefinitionWirespec.convert(): File = when (this) {
    is TypeWirespec     -> convert()
    is EnumWirespec     -> convert()
    is UnionWirespec    -> convert()
    is RefinedWirespec  -> convert()
    is ChannelWirespec  -> convert()
    is EndpointWirespec -> convert()
}
```

Each per-definition converter produces a complete IR `File` with the appropriate structs, interfaces, functions, and validation logic. `EndpointWirespec.convert()` is the most complex — it generates Path/Queries/RequestHeaders/Request structs, a Response union hierarchy, serialization functions, and a Handler interface.

Reference conversion maps parser references to IR types: `Custom → Type.Custom`, `Iterable → Type.Array`, `Dict → Type.Dict`, `Primitive → Type.String/Integer/Number/Boolean/Bytes`, wrapping with `Type.Nullable` when `isNullable`.

Key file: `src/compiler/ir/.../converter/IrConverter.kt`

### Stage 3: IR AST

The IR is a language-neutral tree with the following node types:

**Elements** (AST nodes):
- `File(name, elements)` — top-level container
- `Struct(name, fields, constructors, interfaces, elements)` — record/class
- `Interface(name, elements, extends, isSealed, typeParameters, fields)` — interface/protocol
- `Namespace(name, elements, extends)` — grouping container
- `Union(name, members, typeParameters)` — tagged union
- `Enum(name, entries, fields, constructors, elements)` — enumeration (entries have name + values)
- `Function(name, typeParameters, parameters, returnType, body, isAsync, isStatic, isOverride)`
- `Package(path)`, `Import(path, type)`, `RawElement(code)`

**Type system** (`sealed interface Type`):
`Integer(precision)`, `Number(precision)`, `String`, `Boolean`, `Bytes`, `Unit`, `Any`, `Wildcard`, `Array(elementType)`, `Dict(keyType, valueType)`, `Custom(name, generics)`, `Nullable(type)`

**Statement / Expression hierarchy**: `RawExpression`, `VariableReference`, `FieldCall`, `FunctionCall`, `BinaryOp`, `ConstructorStatement`, `Literal`, `Switch/Case`, `IfExpression`, `StringTemplate`, `MapExpression`, `ReturnStatement`, `Assignment`, constraints (`RegexMatch`, `BoundCheck`), null-handling (`NullCheck`, `NullableMap`, `NullableOf`), and more.

Key file: `src/compiler/ir/.../core/Ast.kt`

### Stage 4: Transform (detailed)

The transform layer is the heart of language-specific adaptation. It lets each emitter reshape the language-neutral IR into a form that generates idiomatic target code.

Key file: `src/compiler/ir/.../core/Transform.kt`

#### Transformer interface

Eight override points, each defaulting to recursive `transformChildren()`:

```kotlin
interface Transformer {
    fun transformType(type: Type): Type
    fun transformElement(element: Element): Element
    fun transformStatement(statement: Statement): Statement
    fun transformExpression(expression: Expression): Expression
    fun transformField(field: Field): Field
    fun transformParameter(parameter: Parameter): Parameter
    fun transformConstructor(constructor: Constructor): Constructor
    fun transformCase(case: Case): Case
}
```

#### `transformer()` DSL factory

Creates a `Transformer` from lambdas, each receiving the node and the transformer (for recursive delegation):

```kotlin
fun transformer(
    transformType: (Type, Transformer) -> Type = { t, tr -> t.transformChildren(tr) },
    transformElement: (Element, Transformer) -> Element = { e, tr -> e.transformChildren(tr) },
    transformStatement: (Statement, Transformer) -> Statement = { s, tr -> s.transformChildren(tr) },
    transformExpression: (Expression, Transformer) -> Expression = { e, tr -> e.transformChildren(tr) },
    transformField: (Field, Transformer) -> Field = { f, tr -> f.transformChildren(tr) },
    transformParameter: (Parameter, Transformer) -> Parameter = { p, tr -> p.transformChildren(tr) },
    transformConstructor: (Constructor, Transformer) -> Constructor = { c, tr -> c.transformChildren(tr) },
    transformCase: (Case, Transformer) -> Case = { c, tr -> c.transformChildren(tr) },
): Transformer
```

#### `transformChildren()` recursive traversal

Each node type has a `transformChildren(Transformer)` extension that walks into its children. For example, a `Struct` transforms its fields, constructors, and child elements; a `FunctionCall` transforms its receiver, type arguments, and argument expressions. This ensures transforms propagate through the entire tree.

Apply a transformer to any element: `fun <T : Element> T.transform(transformer: Transformer): T`

#### High-level DSL

| Function | Purpose |
|---|---|
| `transformMatching<M : Type>` | Transform all types matching a Kotlin class |
| `transformMatchingElements<M : Element>` | Transform all elements matching a Kotlin class |
| `transformFieldsWhere(predicate, transform)` | Transform fields matching a predicate |
| `transformParametersWhere(predicate, transform)` | Transform parameters matching a predicate |
| `renameType(oldName, newName)` | Rename a `Type.Custom` throughout the tree |
| `renameField(oldName, newName)` | Rename a field throughout the tree |
| `transformTypeByName(name, transform)` | Transform types matching a custom name |
| `injectBefore<T>(produce)` | Insert elements before a matching container |
| `injectAfter<T>(produce)` | Insert elements after a matching container |

#### Visitor (read-only traversal)

```kotlin
interface Visitor {
    fun visitType(type: Type)
    fun visitElement(element: Element)
    fun visitStatement(statement: Statement)
    // + visitExpression, visitField, visitParameter, visitConstructor, visitCase
}
```

Convenience extensions: `forEachType()`, `forEachElement()`, `forEachField()`, `collectTypes()`, `collectCustomTypeNames()`, `findAll<T>()`, `findAllTypes<T>()`, `findElement<T>()`.

### Stage 5: Generate

Each language emitter implements `File.generate()` by delegating to a `CodeGenerator` singleton:

```kotlin
interface CodeGenerator {
    fun generate(element: Element): String
}
```

Generators: `JavaGenerator`, `KotlinGenerator`, `TypeScriptGenerator`, `PythonGenerator`, `RustGenerator`. Each recursively walks the IR tree and emits the corresponding target-language syntax as a string.

Top-level entry: `fun Element.generateJava()`, `fun Element.generateKotlin()`, etc.

Key files:
- `src/compiler/ir/.../emit/IrEmitter.kt`
- `src/compiler/ir/.../generator/CodeGenerator.kt`
- `src/compiler/ir/.../generator/{Java,Kotlin,TypeScript,Python,Rust}Generator.kt`

### Key File Reference

| File | Purpose |
|---|---|
| `src/compiler/core/.../parse/ast/Definition.kt` | Parser AST definition types |
| `src/compiler/core/.../parse/ast/Reference.kt` | Parser AST reference/type system |
| `src/compiler/ir/.../converter/IrConverter.kt` | Parser AST → IR conversion |
| `src/compiler/ir/.../core/Ast.kt` | IR node types (Element, Type, Statement, Expression) |
| `src/compiler/ir/.../core/Transform.kt` | Transform DSL + Visitor |
| `src/compiler/ir/.../emit/IrEmitter.kt` | Emitter interface and orchestration |
| `src/compiler/ir/.../generator/CodeGenerator.kt` | Generator interface + top-level functions |
| `src/compiler/emitters/java/.../JavaIrEmitter.kt` | Java-specific transforms + emit |
| `src/compiler/emitters/kotlin/.../KotlinIrEmitter.kt` | Kotlin-specific transforms + emit |
| `src/compiler/emitters/typescript/.../TypeScriptIrEmitter.kt` | TypeScript-specific transforms + emit |
| `src/compiler/emitters/python/.../PythonIrEmitter.kt` | Python-specific transforms + emit |
| `src/compiler/emitters/rust/.../RustIrEmitter.kt` | Rust-specific transforms + emit |
