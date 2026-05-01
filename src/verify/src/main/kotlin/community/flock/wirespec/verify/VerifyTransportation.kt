package community.flock.wirespec.verify

import community.flock.wirespec.emitters.java.JavaIrEmitter
import community.flock.wirespec.emitters.kotlin.KotlinIrEmitter
import community.flock.wirespec.emitters.python.PythonIrEmitter
import community.flock.wirespec.emitters.rust.RustIrEmitter
import community.flock.wirespec.emitters.scala.ScalaIrEmitter
import community.flock.wirespec.emitters.typescript.TypeScriptIrEmitter

internal fun transportationCode(lang: Language): String = when (lang.emitter) {
    is JavaIrEmitter -> """
        |static Wirespec.Transportation transportation = (Wirespec.RawRequest rawRequest) -> {
        |    assert rawRequest.method().equals("GET") : "Method should be GET";
        |    assert rawRequest.path().get(0).equals("todos") : "Path should start with todos";
        |    TodoDto todo = new TodoDto("test");
        |    byte[] body = serialization.serializeBody(java.util.List.of(todo), Wirespec.getType(TodoDto.class, java.util.List.class));
        |    return java.util.concurrent.CompletableFuture.completedFuture(new Wirespec.RawResponse(200, java.util.Collections.emptyMap(), java.util.Optional.of(body)));
        |};
    """.trimMargin()

    is KotlinIrEmitter -> """
        |val transportation = object : Wirespec.Transportation {
        |    override suspend fun transport(request: Wirespec.RawRequest): Wirespec.RawResponse {
        |        assert(request.method == "GET") { "Method should be GET" }
        |        assert(request.path[0] == "todos") { "Path should start with todos" }
        |        val todo = TodoDto(description = "test")
        |        val body = serialization.serializeBody(listOf(todo), kotlin.reflect.typeOf<List<TodoDto>>())
        |        return Wirespec.RawResponse(statusCode = 200, headers = emptyMap(), body = body)
        |    }
        |}
    """.trimMargin()

    is TypeScriptIrEmitter -> """
        |const transportation: Wirespec.Transportation = {
        |    transport: async (request: Wirespec.RawRequest): Promise<Wirespec.RawResponse> => {
        |        if (request.method !== "GET") throw new Error("Method should be GET");
        |        if (request.path[0] !== "todos") throw new Error("Path should start with todos");
        |        const todo: TodoDto = { description: "test" };
        |        const body = serialization.serializeBody([todo], "TodoDto");
        |        return { statusCode: 200, headers: {}, body };
        |    }
        |}
    """.trimMargin()

    is PythonIrEmitter -> """
        |class TestTransportation(Wirespec.Transportation):
        |    def __init__(self, serialization):
        |        self.serialization = serialization
        |    async def transport(self, request):
        |        assert request.method == "GET", "Method should be GET"
        |        assert request.path[0] == "todos", "Path should start with todos"
        |        todo = TodoDto(description="test")
        |        body = self.serialization.serializeBody([todo], "List[TodoDto]")
        |        return Wirespec.RawResponse(statusCode=200, headers={}, body=body)
        |transportation = TestTransportation(serialization)
    """.trimMargin()

    is RustIrEmitter -> """
        |use generated::wirespec::Transportation;
        |struct MockTransport<'a, S: Serialization> {
        |    serialization: &'a S,
        |}
        |impl<'a, S: Serialization> Transportation for MockTransport<'a, S> {
        |    async fn transport(&self, request: &RawRequest) -> RawResponse {
        |        assert_eq!(request.method, "GET", "Method should be GET");
        |        assert_eq!(request.path[0], "todos", "Path should start with todos");
        |        let todo = TodoDto { description: "test".to_string() };
        |        let body = self.serialization.serialize_body(&vec![todo], std::any::TypeId::of::<Vec<TodoDto>>());
        |        RawResponse { status_code: 200, headers: std::collections::HashMap::new(), body: Some(body) }
        |    }
        |}
        |#[allow(non_upper_case_globals)]
        |static transportation: MockTransport<'static, MockSer> = MockTransport { serialization: &serialization };
    """.trimMargin()

    is ScalaIrEmitter -> """
        |val transportation = new Wirespec.Transportation {
        |    override def transport(request: Wirespec.RawRequest): Wirespec.RawResponse = {
        |        assert(request.method == "GET", "Method should be GET")
        |        assert(request.path.head == "todos", "Path should start with todos")
        |        val todo = TodoDto(description = "test")
        |        val body = serialization.serializeBody(List(todo), scala.reflect.classTag[List[TodoDto]])
        |        Wirespec.RawResponse(statusCode = 200, headers = Map.empty, body = Some(body))
        |    }
        |}
    """.trimMargin()

    else -> error("Unknown emitter: ${lang.emitter::class.simpleName}")
}
