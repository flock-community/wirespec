package community.flock.wirespec.verify

import community.flock.wirespec.compiler.test.CompileMinimalEndpointTest
import community.flock.wirespec.emitters.java.JavaEmitter
import community.flock.wirespec.emitters.kotlin.KotlinEmitter
import community.flock.wirespec.emitters.python.PythonEmitter
import community.flock.wirespec.emitters.rust.RustEmitter
import community.flock.wirespec.emitters.scala.ScalaEmitter
import community.flock.wirespec.emitters.typescript.TypeScriptEmitter
import community.flock.wirespec.compiler.core.ir.BinaryOp
import community.flock.wirespec.compiler.core.ir.ConstructorStatement
import community.flock.wirespec.compiler.core.ir.Expression
import community.flock.wirespec.compiler.core.ir.FieldCall
import community.flock.wirespec.compiler.core.ir.Literal
import community.flock.wirespec.compiler.core.ir.Name
import community.flock.wirespec.compiler.core.ir.RawExpression
import community.flock.wirespec.compiler.core.ir.Type
import community.flock.wirespec.compiler.core.ir.VariableReference
import community.flock.wirespec.compiler.core.ir.file
import io.kotest.core.spec.style.FunSpec

class VerifyConversionTest : FunSpec({

    languages.values.forEach { lang ->
        test("conversion functions - $lang") {
            val isRust = lang.emitter is RustEmitter
            val isTypeScript = lang.emitter is TypeScriptEmitter
            val isPython = lang.emitter is PythonEmitter
            val endpointRef: Expression? = if (isRust) null else RawExpression("GetTodos")
            val requestType = if (isRust || isPython) Type.Custom("Request") else Type.Custom("GetTodos.Request")
            val response200Type = if (isRust || isPython) Type.Custom("Response200") else Type.Custom("GetTodos.Response200")
            val todoDtoType = Type.Custom("TodoDto")

            val testFile = file("ConversionTest") {
                when (lang.emitter) {
                    is JavaEmitter -> {
                        import("community.flock.wirespec.java", "Wirespec")
                        import("community.flock.wirespec.generated.endpoint", "GetTodos")
                        import("community.flock.wirespec.generated.model", "TodoDto")
                    }

                    is KotlinEmitter -> {
                        import("community.flock.wirespec.kotlin", "Wirespec")
                        import("community.flock.wirespec.generated.endpoint", "GetTodos")
                        import("community.flock.wirespec.generated.model", "TodoDto")
                    }

                    is TypeScriptEmitter -> {
                        import("./Wirespec", "Wirespec")
                        import("./endpoint/GetTodos", "GetTodos")
                        import("./model/TodoDto", "TodoDto")
                    }

                    is PythonEmitter -> {
                        import("community.flock.wirespec.generated.wirespec", "Wirespec")
                        import("community.flock.wirespec.generated.endpoint.GetTodos", "GetTodos")
                        import("community.flock.wirespec.generated.endpoint.GetTodos", "Request")
                        import("community.flock.wirespec.generated.endpoint.GetTodos", "Response200")
                        import("community.flock.wirespec.generated.model.TodoDto", "TodoDto")
                    }

                    is ScalaEmitter -> {
                        import("community.flock.wirespec.scala", "Wirespec")
                        import("community.flock.wirespec.generated.endpoint", "GetTodos")
                        import("community.flock.wirespec.generated.model", "TodoDto")
                    }

                    is RustEmitter -> {
                        // Rust imports are handled by run() use statements
                        import("community.flock.wirespec.generated.endpoint", "GetTodos")
                        import("community.flock.wirespec.generated.model", "TodoDto")
                    }
                }

                main(statics = { raw(serializationCode(lang, CompileMinimalEndpointTest)) }) {
                    // toRawRequest
                    when {
                        isRust -> {
                            raw("let request = Request::new()")
                            raw("let raw_request = to_raw_request(&serialization, request)")
                        }
                        isTypeScript -> {
                            raw("const request = GetTodos.request()")
                            raw("const rawRequest = GetTodos.toRawRequest(serialization, request)")
                        }
                        else -> {
                            assign("request", construct(requestType))
                            assign("rawRequest", functionCall("toRawRequest", receiver = endpointRef) {
                                arg("serialization", VariableReference("serialization"))
                                arg("request", VariableReference("request"))
                            })
                        }
                    }
                    assertThat(
                        BinaryOp(
                            FieldCall(VariableReference("rawRequest"), Name.of("method")),
                            BinaryOp.Operator.EQUALS,
                            Literal("GET", Type.String)
                        ),
                        "Method should be GET"
                    )

                    // fromRawRequest
                    when {
                        isRust -> raw("let from_raw = from_raw_request(&serialization, raw_request)")
                        isTypeScript -> raw("const fromRaw = GetTodos.fromRawRequest(serialization, rawRequest)")
                        else -> assign("fromRaw", functionCall("fromRawRequest", receiver = endpointRef) {
                            arg("serialization", VariableReference("serialization"))
                            arg("request", VariableReference("rawRequest"))
                        })
                    }

                    // toRawResponse
                    when {
                        isRust -> {
                            raw("""let response200 = Response200::new(vec![TodoDto { description: "test".to_string() }])""")
                            raw("let raw_response = to_raw_response(&serialization, response200.into())")
                        }
                        isTypeScript -> {
                            raw("const response200 = GetTodos.response200({ body: [{ description: 'test' }] })")
                            raw("const rawResponse = GetTodos.toRawResponse(serialization, response200)")
                        }
                        else -> {
                            assign("response200", construct(response200Type) {
                                arg(
                                    "body", listOf(
                                        listOf(
                                            ConstructorStatement(
                                                todoDtoType,
                                                mapOf(Name.of("description") to Literal("test", Type.String))
                                            )
                                        ),
                                        todoDtoType
                                    )
                                )
                            })
                            assign("rawResponse", functionCall("toRawResponse", receiver = endpointRef) {
                                arg("serialization", VariableReference("serialization"))
                                arg("response", VariableReference("response200"))
                            })
                        }
                    }
                    assertThat(
                        BinaryOp(
                            FieldCall(VariableReference("rawResponse"), Name.of("statusCode")),
                            BinaryOp.Operator.EQUALS,
                            Literal(200, Type.Integer())
                        ),
                        "Status should be 200"
                    )

                    // fromRawResponse
                    when {
                        isRust -> raw("let from_raw_resp = from_raw_response(&serialization, raw_response)")
                        isTypeScript -> raw("const fromRawResp = GetTodos.fromRawResponse(serialization, rawResponse)")
                        else -> assign("fromRawResp", functionCall("fromRawResponse", receiver = endpointRef) {
                            arg("serialization", VariableReference("serialization"))
                            arg("response", VariableReference("rawResponse"))
                        })
                    }
                }
            }

            lang.start(name = "conversion-test", fixture = CompileMinimalEndpointTest)
            lang.run(testFile)
        }
    }
})
