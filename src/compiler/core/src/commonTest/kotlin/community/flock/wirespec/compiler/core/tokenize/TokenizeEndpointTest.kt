package community.flock.wirespec.compiler.core.tokenize

import community.flock.wirespec.compiler.core.WirespecType
import kotlin.test.Test

class TokenizeEndpointTest {

    @Test
    fun testIntegerTokenize() = testTokenizer(
        """000 099 100 199 200 299 300 399 400 499 500 599 600 699""",
        Integer, Integer, Integer, Integer, Integer,
        Integer, Integer, Integer, Integer, Integer,
        Integer, Integer, Integer, Integer,
        EndOfProgram,
    )

    @Test
    fun testEndpointTokenizer() = testTokenizer(
        // language=ws
        """
        |endpoint GetTodos GET /todos/{id: String} -> {
        |    200 -> Todo[]
        |    404 -> Error
        |}
        """.trimMargin(),
        EndpointDefinition,
        WirespecType, Method, Path, ForwardSlash, LeftCurly, DromedaryCaseIdentifier, Colon, WsString,
        RightCurly, Arrow, LeftCurly, Integer, Arrow,
        WirespecType, Brackets, Integer, Arrow,
        WirespecType,
        RightCurly, EndOfProgram,
    )

    @Test
    fun testPOSTWithBodyTokenizer() = testTokenizer(
        // language=ws
        """
        |endpoint PostTodo Todo POST /todos -> {
        |    200 -> Todo
        |}
        """.trimMargin(),
        EndpointDefinition,
        WirespecType,
        WirespecType, Method, Path, Arrow, LeftCurly,
        Integer, Arrow,
        WirespecType, RightCurly, EndOfProgram,
    )

    @Test
    fun testQueryParamsTokenizer() = testTokenizer(
        // language=ws
        """
        |endpoint GetTodos GET /todos
        |?{name: String, date: String} -> {
        |    200 -> Todo[]
        |}
        """.trimMargin(),
        EndpointDefinition,
        WirespecType, Method, Path, QuestionMark, LeftCurly, DromedaryCaseIdentifier, Colon,
        WsString, Comma, DromedaryCaseIdentifier, Colon, WsString, RightCurly, Arrow, LeftCurly,
        Integer, Arrow,
        WirespecType, Brackets, RightCurly, EndOfProgram,
    )

    @Test
    fun testHeadersTokenizer() = testTokenizer(
        // language=ws
        """
        |endpoint GetTodos GET /todos
        |#{version: String, accept: String} -> {
        |    200 -> Todo[]
        |}
        """.trimMargin(),
        EndpointDefinition,
        WirespecType, Method, Path, Hash, LeftCurly, DromedaryCaseIdentifier, Colon,
        WsString, Comma, DromedaryCaseIdentifier, Colon, WsString, RightCurly, Arrow, LeftCurly,
        Integer, Arrow,
        WirespecType, Brackets, RightCurly, EndOfProgram,
    )
}
