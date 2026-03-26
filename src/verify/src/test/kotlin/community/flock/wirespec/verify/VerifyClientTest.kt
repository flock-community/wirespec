package community.flock.wirespec.verify

import community.flock.wirespec.compiler.test.CompileMinimalEndpointTest
import community.flock.wirespec.emitters.python.PythonIrEmitter
import community.flock.wirespec.emitters.rust.RustIrEmitter
import community.flock.wirespec.emitters.typescript.TypeScriptIrEmitter
import community.flock.wirespec.ir.core.ArrayIndexCall
import community.flock.wirespec.ir.core.BinaryOp
import community.flock.wirespec.ir.core.BorrowExpression
import community.flock.wirespec.ir.core.ConstructorStatement
import community.flock.wirespec.ir.core.FieldCall
import community.flock.wirespec.ir.core.FunctionBuilder
import community.flock.wirespec.ir.core.Literal
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.Type
import community.flock.wirespec.ir.core.VariableReference
import community.flock.wirespec.ir.core.file
import io.kotest.core.spec.style.FunSpec

class VerifyClientTest : FunSpec({

    languages.values.forEach { lang ->
        test("endpoint client - $lang") {
            lang.start(name = "client-test", fixture = CompileMinimalEndpointTest)

            val isRust = lang.emitter is RustIrEmitter
            val isPython = lang.emitter is PythonIrEmitter
            val isTypeScript = lang.emitter is TypeScriptIrEmitter
            val response200Type = response200Type(isRust, isPython)

            val testFile = file("EndpointClientTest") {
                endpointClientImports(lang, CompileMinimalEndpointTest)

                main(isAsync = true, statics = {
                    raw(serializationCode(lang, CompileMinimalEndpointTest))
                    raw(transportationCode(lang))
                }) {
                    when {
                        isTypeScript -> assign("endpointClient", functionCall("getTodosClient") {
                            arg("serialization", VariableReference("serialization"))
                            arg("transportation", VariableReference("transportation"))
                        })
                        isRust -> assign("endpointClient", construct(Type.Custom("GetTodosClient")) {
                            arg("serialization", BorrowExpression(VariableReference("serialization")))
                            arg("transportation", BorrowExpression(VariableReference("transportation")))
                        })
                        else -> assign("endpointClient", construct(Type.Custom("GetTodosClient")) {
                            arg("serialization", VariableReference("serialization"))
                            arg("transportation", VariableReference("transportation"))
                        })
                    }

                    val getTodosMethod = if (isPython) "get_todos" else "getTodos"
                    assign("response", functionCall(getTodosMethod,
                        receiver = VariableReference("endpointClient"),
                        isAwait = true,
                    ))

                    assertDescriptionSwitch(response200Type)
                }
            }

            lang.run(testFile)
        }
    }

    languages.values.forEach { lang ->
        test("main client - $lang") {
            lang.start(name = "client-test", fixture = CompileMinimalEndpointTest)

            val isRust = lang.emitter is RustIrEmitter
            val isPython = lang.emitter is PythonIrEmitter
            val isTypeScript = lang.emitter is TypeScriptIrEmitter
            val response200Type = response200Type(isRust, isPython)

            val testFile = file("MainClientTest") {
                mainClientImports(lang, CompileMinimalEndpointTest)

                main(isAsync = true, statics = {
                    raw(serializationCode(lang, CompileMinimalEndpointTest))
                    raw(transportationCode(lang))
                }) {
                    when {
                        isTypeScript -> assign("mainClient", functionCall("client") {
                            arg("serialization", VariableReference("serialization"))
                            arg("transportation", VariableReference("transportation"))
                        })
                        isRust -> assign("mainClient", construct(Type.Custom("Client")) {
                            arg("serialization", ConstructorStatement(Type.Custom("MockSer")))
                            arg("transportation", ConstructorStatement(
                                Type.Custom("MockTransport"),
                                mapOf(Name.of("serialization") to BorrowExpression(VariableReference("serialization"))),
                            ))
                        })
                        else -> assign("mainClient", construct(Type.Custom("Client")) {
                            arg("serialization", VariableReference("serialization"))
                            arg("transportation", VariableReference("transportation"))
                        })
                    }

                    val getTodosMethod = if (isPython) "get_todos" else "getTodos"
                    assign("response", functionCall(getTodosMethod,
                        receiver = VariableReference("mainClient"),
                        isAwait = true,
                    ))

                    assertDescriptionSwitch(response200Type)
                }
            }

            lang.run(testFile)
        }
    }
})

private fun response200Type(isRust: Boolean, isPython: Boolean): Type.Custom = when {
    isPython -> Type.Custom("Response200")
    isRust -> Type.Custom("Response::Response200")
    else -> Type.Custom("GetTodos.Response200")
}

private fun FunctionBuilder.assertDescriptionSwitch(response200Type: Type.Custom) {
    switch(VariableReference("response"), variable = "r") {
        case(response200Type) {
            assertThat(
                BinaryOp(
                    FieldCall(
                        ArrayIndexCall(
                            FieldCall(VariableReference("r"), Name.of("body")),
                            Literal(0, Type.Integer()),
                        ),
                        Name.of("description"),
                    ),
                    BinaryOp.Operator.EQUALS,
                    Literal("test", Type.String),
                ),
                "Description should be test",
            )
        }
    }
}
