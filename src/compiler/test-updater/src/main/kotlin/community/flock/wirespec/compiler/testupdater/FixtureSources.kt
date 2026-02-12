package community.flock.wirespec.compiler.testupdater

object FixtureSources {

    val fullEndpoint = """
        |endpoint PutTodo PUT PotentialTodoDto /todos/{id: String}
        |    ?{done: Boolean, name: String?}
        |    #{token: Token, `Refresh-Token`: Token?} -> {
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
    """.trimMargin()

    val channel = """
        |channel Queue -> String
    """.trimMargin()

    val enum = """
        |enum MyAwesomeEnum {
        |  ONE, Two, THREE_MORE, UnitedKingdom
        |}
    """.trimMargin()

    val minimalEndpoint = """
        |endpoint GetTodos GET /todos -> {
        |    200 -> TodoDto[]
        |}
        |type TodoDto {
        |    description: String
        |}
    """.trimMargin()

    val refined = """
        |type TodoId = String(/^[0-9a-fA-F]{8}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{12}$/g)
    """.trimMargin()

    val type = """
        |type Request {
        |  `type`: String,
        |  url: String,
        |  `BODY_TYPE`: String?,
        |  params: String[],
        |  headers: { String },
        |  body: { String?[]? }?
        |}
    """.trimMargin()

    val union = """
        |type UserAccount = UserAccountPassword | UserAccountToken
        |type UserAccountPassword {
        |  username: String,
        |  password: String
        |}
        |type UserAccountToken {
        |  token: String
        |}
        |type User {
        |   username: String,
        |   account: UserAccount
        |}
    """.trimMargin()
}
