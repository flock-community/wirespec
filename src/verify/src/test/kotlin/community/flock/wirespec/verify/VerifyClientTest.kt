package community.flock.wirespec.verify

import community.flock.wirespec.compiler.test.CompileMinimalEndpointTest
import community.flock.wirespec.emitters.java.JavaIrEmitter
import community.flock.wirespec.emitters.kotlin.KotlinIrEmitter
import community.flock.wirespec.emitters.python.PythonIrEmitter
import community.flock.wirespec.emitters.rust.RustIrEmitter
import community.flock.wirespec.emitters.scala.ScalaIrEmitter
import community.flock.wirespec.emitters.typescript.TypeScriptIrEmitter
import community.flock.wirespec.ir.core.file
import io.kotest.core.spec.style.FunSpec

class VerifyClientTest : FunSpec({

    languages.values.forEach { lang ->
        test("endpoint client - $lang") {
            lang.start(name = "client-test", fixture = CompileMinimalEndpointTest)

            val testFile = file("EndpointClientTest") {
                when (lang.emitter) {
                    is JavaIrEmitter -> {
                        import("community.flock.wirespec.java", "Wirespec")
                        import("community.flock.wirespec.generated.endpoint", "GetTodos")
                        import("community.flock.wirespec.generated.endpoint", "GetTodosClient")
                        import("community.flock.wirespec.generated.model", "TodoDto")
                    }
                    is KotlinIrEmitter -> {
                        import("community.flock.wirespec.kotlin", "Wirespec")
                        import("community.flock.wirespec.generated.endpoint", "GetTodos")
                        import("community.flock.wirespec.generated.endpoint", "GetTodosClient")
                        import("community.flock.wirespec.generated.model", "TodoDto")
                        import("kotlin.coroutines", "createCoroutine")
                        import("kotlin.coroutines", "resume")
                    }
                    is TypeScriptIrEmitter -> {
                        import("./Wirespec", "Wirespec")
                        import("./endpoint/GetTodos", "GetTodos")
                        import("./endpoint/GetTodosClient", "getTodosClient")
                        import("./model/TodoDto", "TodoDto")
                    }
                    is PythonIrEmitter -> {
                        raw("from community.flock.wirespec.generated.wirespec import Wirespec")
                        raw("from community.flock.wirespec.generated.endpoint.GetTodosClient import GetTodosClient")
                        raw("from community.flock.wirespec.generated.endpoint.GetTodos import Response200")
                        raw("from community.flock.wirespec.generated.model.TodoDto import TodoDto")
                        raw("import asyncio")
                    }
                    is ScalaIrEmitter -> {
                        import("community.flock.wirespec.scala", "Wirespec")
                        import("community.flock.wirespec.generated.endpoint", "GetTodos")
                        import("community.flock.wirespec.generated.endpoint", "GetTodosClient")
                        import("community.flock.wirespec.generated.model", "TodoDto")
                    }
                    is RustIrEmitter -> {
                        // Only import GetTodos (namespace) and TodoDto (model) via IR imports.
                        // GetTodosClient is a plain struct, not a namespace — import it via raw use statement.
                        import("community.flock.wirespec.generated.endpoint", "GetTodos")
                        import("community.flock.wirespec.generated.model", "TodoDto")
                    }
                }

                if (lang.emitter is RustIrEmitter) {
                    raw(rustSerializationDefs())
                    raw(rustTransportationDefs())
                }
                if (lang.emitter is PythonIrEmitter) {
                    raw(pythonSerializationDefs())
                    raw(pythonTransportationDefs())
                }

                main {
                    raw(serializationCode(lang))
                    raw(transportationCode(lang))

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
                                |use generated::endpoint::get_todos_client::GetTodosClient;
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
                }
            }

            lang.run(testFile)
        }
    }

    languages.values.forEach { lang ->
        test("main client - $lang") {
            lang.start(name = "client-test", fixture = CompileMinimalEndpointTest)

            val testFile = file("MainClientTest") {
                when (lang.emitter) {
                    is JavaIrEmitter -> {
                        import("community.flock.wirespec.java", "Wirespec")
                        import("community.flock.wirespec.generated.endpoint", "GetTodos")
                        import("community.flock.wirespec.generated.endpoint", "Client")
                        import("community.flock.wirespec.generated.model", "TodoDto")
                    }
                    is KotlinIrEmitter -> {
                        import("community.flock.wirespec.kotlin", "Wirespec")
                        import("community.flock.wirespec.generated.endpoint", "GetTodos")
                        import("community.flock.wirespec.generated.endpoint", "Client")
                        import("community.flock.wirespec.generated.model", "TodoDto")
                        import("kotlin.coroutines", "createCoroutine")
                        import("kotlin.coroutines", "resume")
                    }
                    is TypeScriptIrEmitter -> {
                        import("./Wirespec", "Wirespec")
                        import("./endpoint/GetTodos", "GetTodos")
                        import("./endpoint/Client", "client")
                        import("./model/TodoDto", "TodoDto")
                    }
                    is PythonIrEmitter -> {
                        raw("from community.flock.wirespec.generated.wirespec import Wirespec")
                        raw("from community.flock.wirespec.generated.endpoint.Client import Client")
                        raw("from community.flock.wirespec.generated.endpoint.GetTodos import Response200")
                        raw("from community.flock.wirespec.generated.model.TodoDto import TodoDto")
                        raw("import asyncio")
                    }
                    is ScalaIrEmitter -> {
                        import("community.flock.wirespec.scala", "Wirespec")
                        import("community.flock.wirespec.generated.endpoint", "GetTodos")
                        import("community.flock.wirespec.generated.endpoint", "Client")
                        import("community.flock.wirespec.generated.model", "TodoDto")
                    }
                    is RustIrEmitter -> {
                        // Only import GetTodos (namespace) and TodoDto (model) via IR imports.
                        // Client is a plain struct, not a namespace — import it via raw use statement.
                        import("community.flock.wirespec.generated.endpoint", "GetTodos")
                        import("community.flock.wirespec.generated.model", "TodoDto")
                    }
                }

                if (lang.emitter is RustIrEmitter) {
                    raw(rustSerializationDefs())
                    raw(rustTransportationDefs())
                }
                if (lang.emitter is PythonIrEmitter) {
                    raw(pythonSerializationDefs())
                    raw(pythonTransportationDefs())
                }

                main {
                    raw(serializationCode(lang))
                    raw(transportationCode(lang))

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
                                |use generated::endpoint::client::Client;
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
                }
            }

            lang.run(testFile)
        }
    }
})

private fun kotlinRunSuspend(): String = """
    |fun <T> runSuspend(block: suspend () -> T): T {
    |    var result: Result<T>? = null
    |    block.createCoroutine(object : kotlin.coroutines.Continuation<T> {
    |        override val context = kotlin.coroutines.EmptyCoroutineContext
    |        override fun resumeWith(r: Result<T>) { result = r }
    |    }).resume(Unit)
    |    return result!!.getOrThrow()
    |}
""".trimMargin()
