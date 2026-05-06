package community.flock.wirespec.verify

import community.flock.wirespec.compiler.test.CompileGeneratorTest
import community.flock.wirespec.emitters.java.JavaIrEmitter
import community.flock.wirespec.emitters.kotlin.KotlinIrEmitter
import community.flock.wirespec.ir.core.ArrayIndexCall
import community.flock.wirespec.ir.core.BinaryOp
import community.flock.wirespec.ir.core.Case
import community.flock.wirespec.ir.core.Cast
import community.flock.wirespec.ir.core.Expression
import community.flock.wirespec.ir.core.FieldCall
import community.flock.wirespec.ir.core.FunctionCall
import community.flock.wirespec.ir.core.Literal
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.NullableGet
import community.flock.wirespec.ir.core.Precision
import community.flock.wirespec.ir.core.RawExpression
import community.flock.wirespec.ir.core.Switch
import community.flock.wirespec.ir.core.Type
import community.flock.wirespec.ir.core.VariableReference
import community.flock.wirespec.ir.core.file
import io.kotest.core.spec.style.FunSpec

/**
 * Verifies that the generated `PersonGenerator.generate(generator, path)` function
 * produces a populated Person when invoked with a deterministic Generator callback.
 *
 * SCOPE: Kotlin and Java. Rust/Scala/Python/TypeScript are deferred because their
 * Generator converters have known output issues (Rust dotted names, Scala
 * unreachable-throw in unions, etc.).
 */
class VerifyGeneratorTest : FunSpec({

    languages.values
        .filter { it.emitter is KotlinIrEmitter || it.emitter is JavaIrEmitter }
        .forEach { lang ->
            test("generator produces populated Person - $lang") {
                val isJava = lang.emitter is JavaIrEmitter
                // Language-specific hooks the IR can't express:
                // - Wirespec runtime lives in different packages
                // - `object Generator` in Kotlin is referenced as `Generator`;
                //   Java emits `record Generator()` and must be instantiated
                // - empty byte array: Kotlin `ByteArray(0)` vs Java `new byte[0]`
                val wirespecPkg = if (isJava) "community.flock.wirespec.java" else "community.flock.wirespec.kotlin"
                val generatorRef: Expression = RawExpression(if (isJava) "new Generator()" else "Generator")
                val emptyBytes: Expression = if (isJava) {
                    RawExpression("new byte[0]")
                } else {
                    FunctionCall(
                        name = Name.of("ByteArray"),
                        arguments = mapOf(Name.of("size") to Literal(0, Type.Integer())),
                    )
                }

                val testFile = file("GeneratorSmokeTest") {
                    import(wirespecPkg, "Wirespec")
                    import("community.flock.wirespec.generated.model", "Person")
                    import("community.flock.wirespec.generated.generator", "PersonGenerator")
                    if (isJava) {
                        import("java.lang.reflect", "Type")
                    } else {
                        import("kotlin.reflect", "KType")
                    }

                    main(statics = {
                        // A deterministic Generator that returns a predictable value
                        // for every GeneratorField variant. The generated
                        // PersonGenerator calls into this for each primitive field;
                        // for nested models (Address, Color, UUID) it delegates to
                        // the corresponding *Generator.generate(generator, path).
                        //
                        // Emits as `object Generator : Wirespec.Generator` (Kotlin)
                        // / `record Generator() implements Wirespec.Generator` (Java).
                        // Kotlin's `-nowarn` hides UNCHECKED_CAST; Java doesn't care.
                        struct("Generator") {
                            implements(Type.Custom("Wirespec.Generator"))
                            function("generate", isOverride = true) {
                                typeParam(Type.Custom("T"), Type.Nullable(Type.Any))
                                arg("path", Type.Array(Type.String))
                                // Type.Reflect emits `KType` in Kotlin, `Type` in Java.
                                arg("type", Type.Reflect)
                                arg("field", Type.Custom("Wirespec.GeneratorField", listOf(Type.Custom("T"))))
                                returnType(Type.Custom("T"))
                                // `variable = f` introduces a narrowed pattern
                                // binding: Kotlin emits `when(val f = field) { is X -> ... }`
                                // (smart-cast on `f`), Java emits `if (field instanceof X f)`
                                // (pattern-variable bind). Cases that need subtype-specific
                                // access (Enum/Union) reference `f`.
                                returns(
                                    Switch(
                                        expression = VariableReference(Name.of("field")),
                                        variable = Name.of("f"),
                                        cases = listOf(
                                            typeCase("Wirespec.GeneratorFieldString", Literal("test-string", Type.String)),
                                            typeCase("Wirespec.GeneratorFieldInteger", Literal(42L, Type.Integer(Precision.P64))),
                                            typeCase("Wirespec.GeneratorFieldNumber", Literal(1.0, Type.Number(Precision.P64))),
                                            typeCase("Wirespec.GeneratorFieldBoolean", Literal(true, Type.Boolean)),
                                            typeCase("Wirespec.GeneratorFieldBytes", emptyBytes),
                                            // `f.values[0]` / `f.values().get(0)`. Kotlin List has
                                            // `first()` but Java List doesn't until 21; ArrayIndexCall
                                            // emits `[0]` in Kotlin and `.get(0)` in Java.
                                            typeCase(
                                                "Wirespec.GeneratorFieldEnum",
                                                ArrayIndexCall(
                                                    receiver = FieldCall(VariableReference(Name.of("f")), Name.of("values")),
                                                    index = Literal(0, Type.Integer()),
                                                ),
                                            ),
                                            typeCase(
                                                "Wirespec.GeneratorFieldUnion",
                                                ArrayIndexCall(
                                                    receiver = FieldCall(VariableReference(Name.of("f")), Name.of("variants")),
                                                    index = Literal(0, Type.Integer()),
                                                ),
                                            ),
                                            // Array/Dict/Nullable/Shape all carry a `(path) -> T` thunk;
                                            // Kotlin invokes the function value with `f.generate(path)`,
                                            // Java does `f.generate().apply(path)` (Function<List<String>, T>).
                                            typeCase(
                                                "Wirespec.GeneratorFieldArray",
                                                RawExpression(if (isJava) "java.util.List.of(f.generate().apply(path))" else "listOf(f.generate(path))"),
                                                generics = listOf(Type.Wildcard),
                                            ),
                                            typeCase(
                                                "Wirespec.GeneratorFieldNullable",
                                                RawExpression(if (isJava) "f.generate().apply(path)" else "f.generate(path)"),
                                                generics = listOf(Type.Wildcard),
                                            ),
                                            typeCase(
                                                "Wirespec.GeneratorFieldShape",
                                                RawExpression(if (isJava) "f.generate().apply(path)" else "f.generate(path)"),
                                                generics = listOf(Type.Wildcard),
                                            ),
                                            typeCase(
                                                "Wirespec.GeneratorFieldDict",
                                                RawExpression(if (isJava) "java.util.Map.of(\"a\", f.generate().apply(path))" else "mapOf(\"a\" to f.generate(path))"),
                                                generics = listOf(Type.Wildcard),
                                            ),
                                        ),
                                    ),
                                )
                            }
                        }
                    }) {
                        // Call PersonGenerator.generate and capture the result.
                        // Use RawExpression for the object references because
                        // `VariableReference` normalizes names to camelCase, but
                        // both generators need the type identifier preserved.
                        assign(
                            "person",
                            functionCall("generate", receiver = RawExpression("PersonGenerator")) {
                                arg("generator", generatorRef)
                                arg("path", listOf(listOf(literal("Person")), string))
                            },
                        )

                        // The callback always returns "test-string" for string fields, so
                        // Person.name should be "test-string".
                        assertThat(
                            BinaryOp(
                                FieldCall(VariableReference(Name.of("person")), Name.of("name")),
                                BinaryOp.Operator.EQUALS,
                                Literal("test-string", Type.String),
                            ),
                            "Person.name should be populated by the generator",
                        )

                        // Person.age is typed as Long, so use a P64 integer literal — the
                        // Kotlin generator emits it with the `L` suffix. Type.Integer()
                        // defaults to P32 and would emit plain `42`, which wouldn't equal
                        // a Long-typed field at runtime.
                        assertThat(
                            BinaryOp(
                                FieldCall(VariableReference(Name.of("person")), Name.of("age")),
                                BinaryOp.Operator.EQUALS,
                                Literal(42L, Type.Integer(Precision.P64)),
                            ),
                            "Person.age should be populated by the generator",
                        )

                        // Array fields produce 1 element by default — the IR builds a
                        // 1-element list inside the GeneratorFieldArray thunk, and the
                        // stub Generator just invokes that thunk.
                        assertThat(
                            BinaryOp(
                                FieldCall(
                                    FieldCall(VariableReference(Name.of("person")), Name.of("addresses")),
                                    Name.of("size"),
                                ),
                                BinaryOp.Operator.EQUALS,
                                Literal(1, Type.Integer()),
                            ),
                            "Person.addresses should contain 1 generated Address element",
                        )

                        // Nullable fields: the callback invokes `f.generate()` for
                        // GeneratorFieldNullable, which in turn calls the underlying
                        // GeneratorFieldString case and returns "test-string".
                        // Kotlin: `String?`, Java: `Optional<String>` — NullableGet
                        // emits `!!` / `.get()` to extract the underlying value.
                        assertThat(
                            BinaryOp(
                                NullableGet(FieldCall(VariableReference(Name.of("person")), Name.of("nickname"))),
                                BinaryOp.Operator.EQUALS,
                                Literal("test-string", Type.String),
                            ),
                            "Person.nickname should be the non-null generated string",
                        )
                    }
                }

                lang.start(name = "generator-smoke", fixture = CompileGeneratorTest)
                lang.run(testFile)
            }
        }
})

// A pattern-match case body for the Generator.generate switch. Wraps `bodyExpr`
// in `Cast(..., T)` so each branch emits `<value> as T`, satisfying the
// callback's generic return type. `value = RawExpression("")` is ignored for
// pattern-match cases (only `type` and `body` are consumed).
private fun typeCase(typeName: String, bodyExpr: Expression, generics: List<Type> = emptyList()): Case = Case(
    value = RawExpression(""),
    body = listOf(Cast(bodyExpr, Type.Custom("T"))),
    type = Type.Custom(typeName, generics),
)
