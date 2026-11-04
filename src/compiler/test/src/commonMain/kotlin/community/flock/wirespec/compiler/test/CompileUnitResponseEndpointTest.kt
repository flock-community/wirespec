package community.flock.wirespec.compiler.test

public object CompileUnitResponseEndpointTest : Fixture {

    override val source: String =
        // language=ws
        """
        |endpoint PutTodo PUT /todos/{id: String} -> {
        |    200 -> TodoDto
        |    404 -> Unit
        |}
        |type TodoDto {
        |    id: String
        |}
        """.trimMargin()

    override val compiler: Compiler = source.let(::compile)
}
