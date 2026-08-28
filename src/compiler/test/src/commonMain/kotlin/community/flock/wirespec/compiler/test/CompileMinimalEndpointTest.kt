package community.flock.wirespec.compiler.test

public object CompileMinimalEndpointTest : Fixture {

    override val source: String =
        // language=ws
        """
        |endpoint GetTodos GET /todos -> {
        |    200 -> TodoDto[]
        |}
        |type TodoDto {
        |    description: String
        |}
        """.trimMargin()

    override val compiler: Compiler = source.let(::compile)
}
