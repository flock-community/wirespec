package community.flock.wirespec.compiler.test

object CompileEndpointWithRefinedTypeTest : Fixture {

    override val source = // language=ws
        """
        |type TodoId = String(/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/g)
        |
        |endpoint GetTodoById GET /todos/{id: TodoId} -> {
        |    200 -> TodoDto
        |}
        |type TodoDto {
        |    description: String
        |}
        """.trimMargin()

    override val compiler = source.let(::compile)
}
