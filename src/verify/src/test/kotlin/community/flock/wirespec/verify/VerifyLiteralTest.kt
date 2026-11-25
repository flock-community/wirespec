package community.flock.wirespec.verify

import community.flock.wirespec.compiler.core.ir.BinaryOp
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
                    })
                    assign("errors", functionCall("validate", receiver = VariableReference("dog")))
                    assign("expected", literalList(string))
                    assertThat(BinaryOp(VariableReference("errors"), BinaryOp.Operator.EQUALS, VariableReference("expected")), "A dog of kind dog should have no validation errors")
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
                    })
                    assign("errors", functionCall("validate", receiver = VariableReference("dog")))
                    assign(
                        "expected",
                        literalList(listOf(literal("kind")), string),
                    )
                    assertThat(BinaryOp(VariableReference("errors"), BinaryOp.Operator.EQUALS, VariableReference("expected")), "A field that does not hold its literal should be reported")
                }
            }

            lang.start(name = "literal-invalid", fixture = CompileLiteralTest)
            lang.run(testFile)
        }
    }
})
