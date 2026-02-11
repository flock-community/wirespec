package community.flock.wirespec.verify

import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.test.CompileComplexModelTest
import community.flock.wirespec.compiler.test.CompileFullEndpointTest
import community.flock.wirespec.compiler.test.CompileNestedTypeTest
import community.flock.wirespec.compiler.test.CompileRefinedTest
import community.flock.wirespec.emitters.java.JavaIrEmitter
import community.flock.wirespec.emitters.kotlin.KotlinIrEmitter
import community.flock.wirespec.emitters.python.PythonIrEmitter
import community.flock.wirespec.emitters.typescript.TypeScriptIrEmitter
import community.flock.wirespec.language.core.BinaryOp
import community.flock.wirespec.language.core.NullableEmpty
import community.flock.wirespec.language.core.NullableOf
import community.flock.wirespec.language.core.VariableReference
import community.flock.wirespec.language.core.file
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class VerifyTest {

    companion object {
        @JvmStatic
        fun languages() = listOf(
            Language("java-17", JavaIrEmitter(emitShared = EmitShared(true)), { "eclipse-temurin:17-jdk" }),
            Language("java-21", JavaIrEmitter(emitShared = EmitShared(true)), { "eclipse-temurin:21-jdk" }),
            Language("kotlin-1", KotlinIrEmitter(emitShared = EmitShared(true)), { VerifyImage.KOTLIN_1.image }),
            Language("kotlin-2", KotlinIrEmitter(emitShared = EmitShared(true)), { VerifyImage.KOTLIN_2.image }),
            Language("python", PythonIrEmitter(emitShared = EmitShared(true)), { VerifyImage.PYTHON.image }),
            Language("typescript", TypeScriptIrEmitter(), { "node:20-slim" }),
        )
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("languages")
    fun `full endpoint`(lang: Language) {
        lang.start(
            name = "full-endpoint",
            fixture = CompileFullEndpointTest,
        )
        lang.compile()
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("languages")
    fun `refined types`(lang: Language) {


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

    @ParameterizedTest(name = "{0}")
    @MethodSource("languages")
    fun `refined type boundary validation`(lang: Language) {

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

    @ParameterizedTest(name = "{0}")
    @MethodSource("languages")
    fun `model validation valid`(lang: Language) {

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

    @ParameterizedTest(name = "{0}")
    @MethodSource("languages")
    fun `model validation`(lang: Language) {

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

    @ParameterizedTest(name = "{0}")
    @MethodSource("languages")
    fun `complex model validation valid`(lang: Language) {

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

    @ParameterizedTest(name = "{0}")
    @MethodSource("languages")
    fun `complex model validation invalid`(lang: Language) {

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
