package community.flock.wirespec.verify

import community.flock.wirespec.compiler.test.CompileRefinedTest
import community.flock.wirespec.ir.core.VariableReference
import community.flock.wirespec.ir.core.file
import io.kotest.core.spec.style.FunSpec

class VerifyRefinedTest : FunSpec({

    languages.values.forEach { lang ->
        test("refined types - $lang") {
            val testFile = file("RefinedValidation") {
                import("community.flock.wirespec.generated.model", "TestInt2")
                main {
                    assign("refined", construct(type("TestInt2")) {
                        arg("value", literal(2L))
                    })
                    assign("result", functionCall("validate", receiver = VariableReference("refined")))
                    assertThat(VariableReference("result"), "Refined type is not valid")
                }
            }

            lang.start(name = "refined-types", fixture = CompileRefinedTest)
            lang.run(testFile)
        }

        test("refined type boundary validation - $lang") {
            val testFile = file("RefinedBoundaryValidation") {
                import("community.flock.wirespec.generated.model", "TestInt2")
                import("community.flock.wirespec.generated.model", "TodoId")
                import("community.flock.wirespec.generated.model", "TestNum2")
                import("community.flock.wirespec.generated.model", "TestInt1")
                import("community.flock.wirespec.generated.model", "TestNum1")
                main {
                    assign("int2Min", construct(type("TestInt2")) {
                        arg("value", literal(1L))
                    })
                    assign("int2MinResult", functionCall("validate", receiver = VariableReference("int2Min")))
                    assertThat(VariableReference("int2MinResult"), "TestInt2(1) should be valid")

                    assign("int2Max", construct(type("TestInt2")) {
                        arg("value", literal(3L))
                    })
                    assign("int2MaxResult", functionCall("validate", receiver = VariableReference("int2Max")))
                    assertThat(VariableReference("int2MaxResult"), "TestInt2(3) should be valid")

                    assign("todoId", construct(type("TodoId")) {
                        arg("value", literal("550e8400-e29b-41d4-a716-446655440000"))
                    })
                    assign("todoIdResult", functionCall("validate", receiver = VariableReference("todoId")))
                    assertThat(VariableReference("todoIdResult"), "TodoId with valid UUID should be valid")

                    assign("num2", construct(type("TestNum2")) {
                        arg("value", literal(0.3))
                    })
                    assign("num2Result", functionCall("validate", receiver = VariableReference("num2")))
                    assertThat(VariableReference("num2Result"), "TestNum2(0.3) should be valid")

                    assign("int1", construct(type("TestInt1")) {
                        arg("value", literal(0L))
                    })
                    assign("int1Result", functionCall("validate", receiver = VariableReference("int1")))
                    assertThat(VariableReference("int1Result"), "TestInt1(0) should be valid")

                    assign("num1", construct(type("TestNum1")) {
                        arg("value", literal(0.5))
                    })
                    assign("num1Result", functionCall("validate", receiver = VariableReference("num1")))
                    assertThat(VariableReference("num1Result"), "TestNum1(0.5) should be valid")
                }
            }

            lang.start(name = "refined-boundary", fixture = CompileRefinedTest)
            lang.run(testFile)
        }
    }
})
