package community.flock.wirespec.verify

import community.flock.wirespec.compiler.test.CompileMinimalEndpointTest
import community.flock.wirespec.emitters.python.PythonIrEmitter
import community.flock.wirespec.emitters.rust.RustIrEmitter
import community.flock.wirespec.emitters.typescript.TypeScriptIrEmitter
import community.flock.wirespec.ir.core.ArrayIndexCall
import community.flock.wirespec.ir.core.BinaryOp
import community.flock.wirespec.emitters.rust.RustIrEmitter.Companion.borrow
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

                main(statics = {
                    raw(serializationCode(lang, CompileMinimalEndpointTest))
                    raw(transportationCode(lang))
                }) {

                    when (lang.emitter) {
                        is JavaIrEmitter -> {
                            raw("GetTodosClient endpointClient = new GetTodosClient(serialization, transportation)")
                            raw("GetTodos.Response<?> response = endpointClient.getTodos().join()")
                            raw("""assert response instanceof GetTodos.Response200 : "Response should be 200"""")
                            raw("""assert ((GetTodos.Response200) response).body().get(0).description().equals("test") : "Description should be test"""")
                        }
                        is KotlinIrEmitter -> {
                            raw(kotlinRunSuspend())
                            raw("""
                                |runSuspend {
                                |    val endpointClient = GetTodosClient(serialization = serialization, transportation = transportation)
                                |    val response = endpointClient.getTodos()
                                |    assert(response is GetTodos.Response200) { "Response should be 200" }
                                |    assert((response as GetTodos.Response200).body[0].description == "test") { "Description should be test" }
                                |}
                            """.trimMargin())
                        }
                        is TypeScriptIrEmitter -> {
                            raw("""
                                |const endpointClient = getTodosClient(serialization, transportation);
                                |(async () => {
                                |    const response = await endpointClient.getTodos();
                                |    if (response.status !== 200) throw new Error("Response should be 200");
                                |    if ((response as any).body[0].description !== "test") throw new Error("Description should be test");
                                |})();
                            """.trimMargin())
                        }
                        is PythonIrEmitter -> {
                            raw("async def async_main():")
                            raw("    endpoint_client = GetTodosClient(serialization=serialization, transportation=transportation)")
                            raw("    response = await endpoint_client.get_todos()")
                            raw("    assert isinstance(response, Response200), \"Response should be 200\"")
                            raw("    assert response.body[0].description == \"test\", \"Description should be test\"")
                            raw("asyncio.run(async_main())")
                        }
                        is ScalaIrEmitter -> {
                            raw("""
                                |val endpointClient = GetTodosClient(serialization = serialization, transportation = transportation)
                                |val response = endpointClient.getTodos()
                                |assert(response.isInstanceOf[GetTodos.Response200], "Response should be 200")
                                |assert(response.asInstanceOf[GetTodos.Response200].body.head.description == "test", "Description should be test")
                            """.trimMargin())
                        }
                        is RustIrEmitter -> {
                            raw("""
                                |use generated::client::get_todos_client::GetTodosClient;
                                |let endpoint_client = GetTodosClient { serialization: &serialization, transportation: &transportation };
                                |let response = pollster::block_on(endpoint_client.get_todos());
                                |match response {
                                |    Response::Response200(r) => {
                                |        assert_eq!(r.body[0].description, "test", "Description should be test");
                                |    }
                                |}
                            """.trimMargin())
                        }
                        else -> error("Unknown emitter: ${lang.emitter::class.simpleName}")
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

                main(statics = {
                    raw(serializationCode(lang, CompileMinimalEndpointTest))
                    raw(transportationCode(lang))
                }) {

                    when (lang.emitter) {
                        is JavaIrEmitter -> {
                            raw("Client mainClient = new Client(serialization, transportation)")
                            raw("GetTodos.Response<?> response = mainClient.getTodos().join()")
                            raw("""assert response instanceof GetTodos.Response200 : "Response should be 200"""")
                            raw("""assert ((GetTodos.Response200) response).body().get(0).description().equals("test") : "Description should be test"""")
                        }
                        is KotlinIrEmitter -> {
                            raw(kotlinRunSuspend())
                            raw("""
                                |runSuspend {
                                |    val mainClient = Client(serialization = serialization, transportation = transportation)
                                |    val response = mainClient.getTodos()
                                |    assert(response is GetTodos.Response200) { "Response should be 200" }
                                |    assert((response as GetTodos.Response200).body[0].description == "test") { "Description should be test" }
                                |}
                            """.trimMargin())
                        }
                        is TypeScriptIrEmitter -> {
                            raw("""
                                |const mainClient = client(serialization, transportation);
                                |(async () => {
                                |    const response = await mainClient.getTodos();
                                |    if (response.status !== 200) throw new Error("Response should be 200");
                                |    if ((response as any).body[0].description !== "test") throw new Error("Description should be test");
                                |})();
                            """.trimMargin())
                        }
                        is PythonIrEmitter -> {
                            raw("async def async_main():")
                            raw("    main_client = Client(serialization=serialization, transportation=transportation)")
                            raw("    response = await main_client.get_todos()")
                            raw("    assert isinstance(response, Response200), \"Response should be 200\"")
                            raw("    assert response.body[0].description == \"test\", \"Description should be test\"")
                            raw("asyncio.run(async_main())")
                        }
                        is ScalaIrEmitter -> {
                            raw("""
                                |val mainClient = Client(serialization = serialization, transportation = transportation)
                                |val response = mainClient.getTodos()
                                |assert(response.isInstanceOf[GetTodos.Response200], "Response should be 200")
                                |assert(response.asInstanceOf[GetTodos.Response200].body.head.description == "test", "Description should be test")
                            """.trimMargin())
                        }
                        is RustIrEmitter -> {
                            raw("""
                                |use generated::client::Client;
                                |let main_client = Client { serialization: MockSer, transportation: MockTransport { serialization: &serialization } };
                                |let response = pollster::block_on(main_client.get_todos());
                                |match response {
                                |    Response::Response200(r) => {
                                |        assert_eq!(r.body[0].description, "test", "Description should be test");
                                |    }
                                |}
                            """.trimMargin())
                        }
                        else -> error("Unknown emitter: ${lang.emitter::class.simpleName}")
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
