package community.flock.wirespec.verify

import community.flock.wirespec.compiler.test.CompileComplexModelTest
import community.flock.wirespec.ir.core.BinaryOp
import community.flock.wirespec.ir.core.NullableEmpty
import community.flock.wirespec.ir.core.NullableOf
import community.flock.wirespec.ir.core.VariableReference
import community.flock.wirespec.ir.core.file
import io.kotest.core.spec.style.FunSpec

class VerifyComplexModelTest : FunSpec({

    languages.values.forEach { lang ->
        test("complex model validation valid - $lang") {
            val testFile = file("ComplexModelValidationValid") {
                import("community.flock.wirespec.generated.model", "Email")
                import("community.flock.wirespec.generated.model", "PhoneNumber")
                import("community.flock.wirespec.generated.model", "Tag")
                import("community.flock.wirespec.generated.model", "EmployeeAge")
                import("community.flock.wirespec.generated.model", "ContactInfo")
                import("community.flock.wirespec.generated.model", "Employee")
                import("community.flock.wirespec.generated.model", "Department")
                import("community.flock.wirespec.generated.model", "Company")
                main {
                    assign("email", construct(type("Email")) {
                        arg("value", literal("test@example.com"))
                    })
                    assign("phone", construct(type("PhoneNumber")) {
                        arg("value", literal("+1234567890"))
                    })
                    assign("tag1", construct(type("Tag")) {
                        arg("value", literal("developer"))
                    })
                    assign("tag2", construct(type("Tag")) {
                        arg("value", literal("senior"))
                    })
                    assign("age", construct(type("EmployeeAge")) {
                        arg("value", literal(30L))
                    })
                    assign("contactInfo", construct(type("ContactInfo")) {
                        arg("email", VariableReference("email"))
                        arg("phone", NullableOf(VariableReference("phone")))
                    })
                    assign("employee", construct(type("Employee")) {
                        arg("name", literal("John"))
                        arg("age", VariableReference("age"))
                        arg("contactInfo", VariableReference("contactInfo"))
                        arg("tags", listOf(listOf(VariableReference("tag1"), VariableReference("tag2")), type("Tag")))
                    })
                    assign("department", construct(type("Department")) {
                        arg("name", literal("Engineering"))
                        arg("employees", listOf(listOf(VariableReference("employee")), type("Employee")))
                    })
                    assign("company", construct(type("Company")) {
                        arg("name", literal("Acme"))
                        arg("departments", listOf(listOf(VariableReference("department")), type("Department")))
                    })
                    assign("errors", functionCall("validate", receiver = VariableReference("company")))
                    assign("expected", literalList(string))
                    assertThat(BinaryOp(VariableReference("errors"), BinaryOp.Operator.EQUALS, VariableReference("expected")), "Valid company should have no validation errors")
                }
            }

            lang.start(name = "complex-model-valid", fixture = CompileComplexModelTest)
            lang.run(testFile)
        }

        test("complex model validation invalid - $lang") {
            val testFile = file("ComplexModelValidationInvalid") {
                import("community.flock.wirespec.generated.model", "Email")
                import("community.flock.wirespec.generated.model", "PhoneNumber")
                import("community.flock.wirespec.generated.model", "Tag")
                import("community.flock.wirespec.generated.model", "EmployeeAge")
                import("community.flock.wirespec.generated.model", "ContactInfo")
                import("community.flock.wirespec.generated.model", "Employee")
                import("community.flock.wirespec.generated.model", "Department")
                import("community.flock.wirespec.generated.model", "Company")
                main {
                    assign("email", construct(type("Email")) {
                        arg("value", literal("not-an-email"))
                    })
                    assign("tag1", construct(type("Tag")) {
                        arg("value", literal("valid"))
                    })
                    assign("tag2", construct(type("Tag")) {
                        arg("value", literal("INVALID TAG!"))
                    })
                    assign("age", construct(type("EmployeeAge")) {
                        arg("value", literal(10L))
                    })
                    assign("contactInfo", construct(type("ContactInfo")) {
                        arg("email", VariableReference("email"))
                        arg("phone", NullableEmpty)
                    })
                    assign("employee", construct(type("Employee")) {
                        arg("name", literal("John"))
                        arg("age", VariableReference("age"))
                        arg("contactInfo", VariableReference("contactInfo"))
                        arg("tags", listOf(listOf(VariableReference("tag1"), VariableReference("tag2")), type("Tag")))
                    })
                    assign("department", construct(type("Department")) {
                        arg("name", literal("Engineering"))
                        arg("employees", listOf(listOf(VariableReference("employee")), type("Employee")))
                    })
                    assign("company", construct(type("Company")) {
                        arg("name", literal("Acme"))
                        arg("departments", listOf(listOf(VariableReference("department")), type("Department")))
                    })
                    assign("errors", functionCall("validate", receiver = VariableReference("company")))
                    assign("expected", literalList(listOf(literal("departments[0].employees[0].age"), literal("departments[0].employees[0].contactInfo.email"), literal("departments[0].employees[0].tags[1]")), string))
                    assertThat(BinaryOp(VariableReference("errors"), BinaryOp.Operator.EQUALS, VariableReference("expected")), "Invalid company should have validation errors for age, email, and tags[1]")
                }
            }

            lang.start(name = "complex-model-invalid", fixture = CompileComplexModelTest)
            lang.run(testFile)
        }
    }
})
