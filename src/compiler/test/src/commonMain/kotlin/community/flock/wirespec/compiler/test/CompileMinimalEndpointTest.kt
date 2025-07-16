package community.flock.wirespec.compiler.test

object CompileMinimalEndpointTest {

    val compiler = """
        |endpoint GetTodos GET /todos -> {
        |    200 -> TodoDto[]
        |}
        |type TodoDto {
        |    description: String
        |}
    """.trimMargin().let(::compile)
}
