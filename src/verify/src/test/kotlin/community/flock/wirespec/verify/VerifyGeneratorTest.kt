package community.flock.wirespec.verify

import community.flock.wirespec.compiler.test.CompileGeneratorTest
import community.flock.wirespec.emitters.kotlin.KotlinIrEmitter
import community.flock.wirespec.ir.core.BinaryOp
import community.flock.wirespec.ir.core.FieldCall
import community.flock.wirespec.ir.core.Literal
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.RawExpression
import community.flock.wirespec.ir.core.Type
import community.flock.wirespec.ir.core.VariableReference
import community.flock.wirespec.ir.core.file
import io.kotest.core.spec.style.FunSpec

/**
 * Verifies that the generated `PersonGenerator.generate(path, generator)` function
 * produces a populated Person when invoked with a deterministic Generator callback.
 *
 * SCOPE: Kotlin only. Other languages are deferred because their Generator converters
 * have known output issues (Rust dotted names, Scala unreachable-throw in unions, etc.).
 */
class VerifyGeneratorTest : FunSpec({

    // Kotlin-only: filter the shared `languages` map to the two Kotlin images.
    languages.values
        .filter { it.emitter is KotlinIrEmitter }
        .forEach { lang ->
            test("generator produces populated Person - $lang") {
                val testFile = file("GeneratorSmokeTest") {
                    import("community.flock.wirespec.kotlin", "Wirespec")
                    import("community.flock.wirespec.generated.model", "Person")
                    import("community.flock.wirespec.generated.generator", "PersonGenerator")
                    import("kotlin.reflect", "KType")

                    main(statics = {
                        // A deterministic Generator that returns a predictable value
                        // for every GeneratorField variant. The generated
                        // PersonGenerator calls into this for each primitive field;
                        // for nested models (Address, Color, UUID) it delegates to
                        // the corresponding *Generator.generate(path, generator).
                        raw(
                            """
                            |val generator = object : Wirespec.Generator {
                            |    @Suppress("UNCHECKED_CAST")
                            |    override fun <T : Any> generate(path: List<String>, type: KType, field: Wirespec.GeneratorField<T>): T {
                            |        return when (field) {
                            |            is Wirespec.GeneratorFieldString -> "test-string" as T
                            |            is Wirespec.GeneratorFieldInteger -> 42L as T
                            |            is Wirespec.GeneratorFieldNumber -> 1.0 as T
                            |            is Wirespec.GeneratorFieldBoolean -> true as T
                            |            is Wirespec.GeneratorFieldBytes -> ByteArray(0) as T
                            |            is Wirespec.GeneratorFieldEnum -> field.values.first() as T
                            |            is Wirespec.GeneratorFieldUnion -> field.variants.first() as T
                            |            is Wirespec.GeneratorFieldArray -> 2 as T
                            |            is Wirespec.GeneratorFieldNullable -> false as T
                            |            is Wirespec.GeneratorFieldDict -> 1 as T
                            |        }
                            |    }
                            |}
                            """.trimMargin(),
                        )
                    }) {
                        // Call PersonGenerator.generate and capture the result.
                        // Use RawExpression("PersonGenerator") because `VariableReference`
                        // normalizes the name and would emit `personGenerator` (camelCase),
                        // but Kotlin objects are referenced by their PascalCase identifier.
                        assign(
                            "person",
                            functionCall("generate", receiver = RawExpression("PersonGenerator")) {
                                arg("path", listOf(listOf(literal("Person")), string))
                                arg("generator", VariableReference(Name.of("generator")))
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

                        // Integer fields return 42L. Use a raw "42L" literal so the Kotlin
                        // generator emits the Long suffix (Type.Integer() defaults to P32
                        // and would emit plain `42`, which fails `person.age == 42` when
                        // `age` is typed as Long).
                        assertThat(
                            BinaryOp(
                                FieldCall(VariableReference(Name.of("person")), Name.of("age")),
                                BinaryOp.Operator.EQUALS,
                                RawExpression("42L"),
                            ),
                            "Person.age should be populated by the generator",
                        )

                        // Array fields produce 2 elements (the callback returns 2 for
                        // GeneratorFieldArray, and PersonGenerator iterates that count).
                        assertThat(
                            BinaryOp(
                                FieldCall(
                                    FieldCall(VariableReference(Name.of("person")), Name.of("addresses")),
                                    Name.of("size"),
                                ),
                                BinaryOp.Operator.EQUALS,
                                Literal(2, Type.Integer()),
                            ),
                            "Person.addresses should contain 2 generated Address elements",
                        )

                        // Nullable fields: callback returns false for GeneratorFieldNullable,
                        // meaning "not null", so nickname should be "test-string".
                        assertThat(
                            BinaryOp(
                                FieldCall(VariableReference(Name.of("person")), Name.of("nickname")),
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
