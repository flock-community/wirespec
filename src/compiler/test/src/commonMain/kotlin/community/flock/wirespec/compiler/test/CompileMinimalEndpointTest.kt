package community.flock.wirespec.compiler.test

object CompileMinimalEndpointTest : Fixture {

    override val source =
        // language=ws
        """
        |endpoint GetTodos GET /todos -> {
        |    200 -> TodoDto[]
        |}
        |type TodoDto {
        |    description: String
        |}
        """.trimMargin()

    override val compiler = source.let(::compile)
}
