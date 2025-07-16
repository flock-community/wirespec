package community.flock.wirespec.compiler.test

object CompileFullEndpointTest {

    val compiler = """
        |endpoint PutTodo PUT PotentialTodoDto /todos/{id: String}
        |    ?{done: Boolean, name: String?}
        |    #{token: Token, refreshToken: Token?} -> {
        |    200 -> TodoDto
        |    201 -> TodoDto #{token: Token, refreshToken: Token?}
        |    500 -> Error
        |}
        |type PotentialTodoDto {
        |    name: String,
        |    done: Boolean
        |}
        |type Token {
        |    iss: String
        |}
        |type TodoDto {
        |    id: String,
        |    name: String,
        |    done: Boolean
        |}
        |type Error {
        |    code: Integer,
        |    description: String
        |}
    """.trimMargin().let(::compile)
}
