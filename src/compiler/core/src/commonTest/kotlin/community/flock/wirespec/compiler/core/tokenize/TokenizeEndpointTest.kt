package community.flock.wirespec.compiler.core.tokenize

import community.flock.wirespec.compiler.core.WirespecType
import kotlin.test.Test

class TokenizeEndpointTest {

    @Test
    fun testStatusCodeTokenize() = testTokenizer(
        """000 099 100 199 200 299 300 399 400 499 500 599 600 699""",
        Character, Character, Character, Character, Character, Character,
        StatusCode, StatusCode, StatusCode, StatusCode, StatusCode,
        StatusCode, StatusCode, StatusCode, StatusCode, StatusCode,
        Character, Character, Character, Character, Character, Character,
        EndOfProgram,
    )

    @Test
    fun testEndpointTokenizer() = testTokenizer(
        """
            |endpoint GetTodos GET /todos/{id: String} -> {
            |    200 -> Todo[]
            |    404 -> Error
            |}
        """.trimMargin(),
        EndpointDefinition,
        WirespecType, Method, Path, ForwardSlash, LeftCurly, DromedaryCaseIdentifier, Colon, WsString,
        RightCurly, Arrow, LeftCurly, StatusCode, Arrow,
        WirespecType, Brackets, StatusCode, Arrow,
        WirespecType,
        RightCurly, EndOfProgram,
    )

    @Test
    fun testPOSTWithBodyTokenizer() = testTokenizer(
        """
            |endpoint PostTodo Todo POST /todos -> {
            |    200 -> Todo
            |}
        """.trimMargin(),
        EndpointDefinition,
        WirespecType,
        WirespecType, Method, Path, Arrow, LeftCurly,
        StatusCode, Arrow,
        WirespecType, RightCurly, EndOfProgram,
    )

    @Test
    fun testQueryParamsTokenizer() = testTokenizer(
        """
            |endpoint GetTodos GET /todos
            |?{name: String, date: String} -> {
            |    200 -> Todo[]
            |}
        """.trimMargin(),
        EndpointDefinition,
        WirespecType, Method, Path, QuestionMark, LeftCurly, DromedaryCaseIdentifier, Colon,
        WsString, Comma, DromedaryCaseIdentifier, Colon, WsString, RightCurly, Arrow, LeftCurly,
        StatusCode, Arrow,
        WirespecType, Brackets, RightCurly, EndOfProgram,
    )

    @Test
    fun testHeadersTokenizer() = testTokenizer(
        """
            |endpoint GetTodos GET /todos
            |#{version: String, accept: String} -> {
            |    200 -> Todo[]
            |}
        """.trimMargin(),
        EndpointDefinition,
        WirespecType, Method, Path, Hash, LeftCurly, DromedaryCaseIdentifier, Colon,
        WsString, Comma, DromedaryCaseIdentifier, Colon, WsString, RightCurly, Arrow, LeftCurly,
        StatusCode, Arrow,
        WirespecType, Brackets, RightCurly, EndOfProgram,
    )
}
