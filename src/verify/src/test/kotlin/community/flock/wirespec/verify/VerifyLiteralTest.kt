package community.flock.wirespec.verify

import community.flock.wirespec.compiler.core.ir.BinaryOp
import community.flock.wirespec.compiler.core.ir.LiteralMap
import community.flock.wirespec.compiler.core.ir.NullableOf
import community.flock.wirespec.compiler.core.ir.VariableReference
import community.flock.wirespec.compiler.core.ir.file
import community.flock.wirespec.compiler.test.CompileLiteralTest
import io.kotest.core.spec.style.FunSpec

class VerifyLiteralTest : FunSpec({

    languages.values.forEach { lang ->
        test("literal valid - $lang") {
            val testFile = file("LiteralValid") {
                import("community.flock.wirespec.generated.model", "Dog")
                main {
                    assign("dog", construct(type("Dog")) {
                        arg("kind", literal("dog"))
                        arg("nickname", NullableOf(literal("good \"boy\"")))
                        arg("tags", listOf(listOf(literal("pet")), string))
                        arg("tagsByKey", LiteralMap(mapOf("a" to literal("pet")), string, string))
                    })
                    assign("errors", functionCall("validate", receiver = VariableReference("dog")))
                    assign("expected", literalList(string))
                    assertThat(BinaryOp(VariableReference("errors"), BinaryOp.Operator.EQUALS, VariableReference("expected")), "A dog with its literals should have no validation errors")
                }
            }

            lang.start(name = "literal-valid", fixture = CompileLiteralTest)
            lang.run(testFile)
        }
    }

    // TypeScript has literal types of its own: a value with another string does not compile, so there is nothing to validate.
    languages.filterKeys { it != "typescript" }.values.forEach { lang ->
        test("literal invalid - $lang") {
            val testFile = file("LiteralInvalid") {
                import("community.flock.wirespec.generated.model", "Dog")
                main {
                    assign("dog", construct(type("Dog")) {
                        arg("kind", literal("cat"))
                        arg("nickname", NullableOf(literal("bad boy")))
                        arg("tags", listOf(listOf(literal("pet"), literal("wild")), string))
                        arg("tagsByKey", LiteralMap(mapOf("a" to literal("wild")), string, string))
                    })
                    assign("errors", functionCall("validate", receiver = VariableReference("dog")))
                    assign(
                        "expected",
                        literalList(listOf(literal("kind"), literal("nickname"), literal("tags[1]"), literal("tagsByKey[a]")), string),
                    )
                    assertThat(BinaryOp(VariableReference("errors"), BinaryOp.Operator.EQUALS, VariableReference("expected")), "Every value that is not its literal should be reported by its path")
                }
            }

            lang.start(name = "literal-invalid", fixture = CompileLiteralTest)
            lang.run(testFile)
        }
    }
})
