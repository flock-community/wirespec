package community.flock.wirespec.verify

import community.flock.wirespec.compiler.test.CompileNestedTypeTest
import community.flock.wirespec.emitters.rust.RustEmitter
import community.flock.wirespec.emitters.typescript.TypeScriptEmitter
import community.flock.wirespec.compiler.core.ir.ArrayIndexCall
import community.flock.wirespec.compiler.core.ir.BinaryOp
import community.flock.wirespec.compiler.core.ir.FieldCall
import community.flock.wirespec.compiler.core.ir.Literal
import community.flock.wirespec.compiler.core.ir.Name
import community.flock.wirespec.compiler.core.ir.Precision
import community.flock.wirespec.compiler.core.ir.RawExpression
import community.flock.wirespec.compiler.core.ir.Type
import community.flock.wirespec.compiler.core.ir.VariableReference
import community.flock.wirespec.compiler.core.ir.file
import io.kotest.core.spec.style.FunSpec

class VerifyGeneratorTest : FunSpec({

    // Rust is excluded: the Rust emitter's generator output is not wired into the generated
    // module tree (lib.rs lacks `pub mod generator`) and does not compile yet.
    languages.values.filterNot { it.emitter is RustEmitter }.forEach { lang ->
        test("generated test data - $lang") {
            val isTypeScript = lang.emitter is TypeScriptEmitter

            val testFile = file("GeneratorDataTest") {
                generatorImports(lang, listOf("Person"))

                main(statics = { raw(generatorCode(lang)) }) {
                    assign("person", functionCall("generate", receiver = RawExpression("PersonGenerator")) {
                        arg("generator", VariableReference("generator"))
                        arg("path", emptyList(string))
                    })
                    assign("address", FieldCall(VariableReference("person"), Name.of("address")))

                    assertThat(
                        BinaryOp(
                            FieldCall(VariableReference("person"), Name.of("name")),
                            BinaryOp.Operator.EQUALS,
                            Literal("string", Type.String),
                        ),
                        "Person name should be the deterministic string",
                    )
                    assertThat(
                        BinaryOp(
                            FieldCall(VariableReference("address"), Name.of("houseNumber")),
                            BinaryOp.Operator.EQUALS,
                            Literal(42L, Type.Integer(Precision.P64)),
                        ),
                        "House number should be the deterministic integer",
                    )
                    // TypeScript inlines refined types to their primitive, so there is no .value wrapper
                    val postalCode = FieldCall(VariableReference("address"), Name.of("postalCode"))
                    val postalValue = if (isTypeScript) postalCode else FieldCall(postalCode, Name.of("value"))
                    assertThat(
                        BinaryOp(postalValue, BinaryOp.Operator.EQUALS, Literal("1234AB", Type.String)),
                        "Postal code should satisfy the refined regex",
                    )
                    assertThat(
                        BinaryOp(
                            ArrayIndexCall(
                                FieldCall(VariableReference("person"), Name.of("tags")),
                                Literal(0, Type.Integer()),
                            ),
                            BinaryOp.Operator.EQUALS,
                            Literal("string", Type.String),
                        ),
                        "Tags should contain the deterministic string",
                    )
                }
            }

            lang.start(name = "generator-test", fixture = CompileNestedTypeTest)
            lang.run(testFile)
        }
    }
})
