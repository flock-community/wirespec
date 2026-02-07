package community.flock.wirespec.compiler.test

object CompileEndpointWithRefinedTypeTest {
    val compiler =
        // language=ws
        """
        |type TodoId = String(/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/g)
        |
        |endpoint GetTodoById GET /todos/{id: TodoId} -> {
        |    200 -> TodoDto
        |}
        |type TodoDto {
        |    description: String
        |}
        """.trimMargin().let(::compile)
}
