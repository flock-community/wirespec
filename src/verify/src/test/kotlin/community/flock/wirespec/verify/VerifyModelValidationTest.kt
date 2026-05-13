package community.flock.wirespec.verify

import community.flock.wirespec.compiler.test.CompileNestedTypeTest
import community.flock.wirespec.ir.core.BinaryOp
import community.flock.wirespec.ir.core.VariableReference
import community.flock.wirespec.ir.core.file
import io.kotest.core.spec.style.FunSpec

class VerifyModelValidationTest : FunSpec({

    languages.values.forEach { lang ->
        test("model validation valid - $lang") {
            val testFile = file("ModelValidationValid") {
                import("community.flock.wirespec.generated.model", "DutchPostalCode")
                import("community.flock.wirespec.generated.model", "Address")
                import("community.flock.wirespec.generated.model", "Person")
                main {
                    assign("postalCode", construct(type("DutchPostalCode")) {
                        arg("value", literal("1234AB"))
                    })
                    assign("address", construct(type("Address")) {
                        arg("street", literal("Main St"))
                        arg("houseNumber", literal(42L))
                        arg("postalCode", VariableReference("postalCode"))
                    })
                    assign("person", construct(type("Person")) {
                        arg("name", literal("John"))
                        arg("address", VariableReference("address"))
                        arg("tags", emptyList(string))
                    })
                    assign("errors", functionCall("validate", receiver = VariableReference("person")))
                    assign("expected", literalList(string))
                    assertThat(BinaryOp(VariableReference("errors"), BinaryOp.Operator.EQUALS, VariableReference("expected")), "Valid person should have no validation errors")
                }
            }

            lang.start(name = "model-validation-valid", fixture = CompileNestedTypeTest)
            lang.run(testFile)
        }

        test("model validation - $lang") {
            val testFile = file("ModelValidation") {
                import("community.flock.wirespec.generated.model", "DutchPostalCode")
                import("community.flock.wirespec.generated.model", "Address")
                import("community.flock.wirespec.generated.model", "Person")
                main {
                    assign("postalCode", construct(type("DutchPostalCode")) {
                        arg("value", literal("invalid"))
                    })
                    assign("address", construct(type("Address")) {
                        arg("street", literal("Main St"))
                        arg("houseNumber", literal(42L))
                        arg("postalCode", VariableReference("postalCode"))
                    })
                    assign("person", construct(type("Person")) {
                        arg("name", literal("John"))
                        arg("address", VariableReference("address"))
                        arg("tags", emptyList(string))
                    })
                    assign("errors", functionCall("validate", receiver = VariableReference("person")))
                    assign("expected", literalList(listOf(literal("address.postalCode")), string))
                    assertThat(BinaryOp(VariableReference("errors"), BinaryOp.Operator.EQUALS, VariableReference("expected")), "Refined type is not valid")
                }
            }

            lang.start(name = "model-validation", fixture = CompileNestedTypeTest)
            lang.run(testFile)
        }
    }
})
