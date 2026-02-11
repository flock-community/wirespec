package community.flock.wirespec.compiler.test

object CompileMinimalEndpointTest {

    val source =
        // language=ws
        """
        |endpoint GetTodos GET /todos -> {
        |    200 -> TodoDto[]
        |}
        |type TodoDto {
        |    description: String
        |}
        """.trimMargin()

    val compiler = source.let(::compile)
}
