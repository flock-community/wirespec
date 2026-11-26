package community.flock.wirespec.verify

import community.flock.wirespec.compiler.core.ir.BinaryOp
import community.flock.wirespec.compiler.core.ir.VariableReference
import community.flock.wirespec.compiler.core.ir.file
import community.flock.wirespec.compiler.test.CompileLiteralTest
import io.kotest.core.spec.style.FunSpec

class VerifyLiteralTest : FunSpec({

    // TypeScript has literal types of its own. It reads the `kind` of an object without a type as a string, which is
    // not a Dog, and a Dog with another kind does not compile: there is nothing to validate, so only compile the output.
    languages.filterKeys { it == "typescript" }.values.forEach { lang ->
        test("literal compiles - $lang") {
            lang.start(name = "literal-compiles", fixture = CompileLiteralTest)
            lang.compile()
        }
    }

    languages.filterKeys { it != "typescript" }.values.forEach { lang ->
        test("literal valid - $lang") {
            val testFile = file("LiteralValid") {
                import("community.flock.wirespec.generated.model", "Dog")
                main {
                    assign("dog", construct(type("Dog")) {
                        arg("kind", literal("dog"))
                        arg("name", literal("Rex"))
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

    languages.filterKeys { it != "typescript" }.values.forEach { lang ->
        test("literal invalid - $lang") {
            val testFile = file("LiteralInvalid") {
                import("community.flock.wirespec.generated.model", "Dog")
                main {
                    assign("dog", construct(type("Dog")) {
                        arg("kind", literal("cat"))
                        arg("name", literal("Rex"))
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
