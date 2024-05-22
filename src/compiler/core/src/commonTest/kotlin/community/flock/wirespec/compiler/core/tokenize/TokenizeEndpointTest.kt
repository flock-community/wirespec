package community.flock.wirespec.compiler.core.tokenize

import community.flock.wirespec.compiler.core.tokenize.types.Arrow
import community.flock.wirespec.compiler.core.tokenize.types.Brackets
import community.flock.wirespec.compiler.core.tokenize.types.Colon
import community.flock.wirespec.compiler.core.tokenize.types.Comma
import community.flock.wirespec.compiler.core.tokenize.types.CustomType
import community.flock.wirespec.compiler.core.tokenize.types.CustomValue
import community.flock.wirespec.compiler.core.tokenize.types.EndOfProgram
import community.flock.wirespec.compiler.core.tokenize.types.ForwardSlash
import community.flock.wirespec.compiler.core.tokenize.types.Hash
import community.flock.wirespec.compiler.core.tokenize.types.Invalid
import community.flock.wirespec.compiler.core.tokenize.types.LeftCurly
import community.flock.wirespec.compiler.core.tokenize.types.Method
import community.flock.wirespec.compiler.core.tokenize.types.Path
import community.flock.wirespec.compiler.core.tokenize.types.QuestionMark
import community.flock.wirespec.compiler.core.tokenize.types.RightCurly
import community.flock.wirespec.compiler.core.tokenize.types.StatusCode
import community.flock.wirespec.compiler.core.tokenize.types.WsEndpointDef
import community.flock.wirespec.compiler.core.tokenize.types.WsString
import kotlin.test.Test

class TokenizeEndpointTest {

    @Test
    fun testStatusCodeTokenize() = testTokenizer(
        """000 099 100 199 200 299 300 399 400 499 500 599 600 699""",
        Invalid, Invalid, Invalid, Invalid, Invalid, Invalid,
        StatusCode, StatusCode, StatusCode, StatusCode, StatusCode,
        StatusCode, StatusCode, StatusCode, StatusCode, StatusCode,
        Invalid, Invalid, Invalid, Invalid, Invalid, Invalid,
        EndOfProgram,
        noInvalid = false,
    )

    @Test
    fun testEndpointTokenizer() = testTokenizer(
        """
            |endpoint GetTodos GET /todos/{id: String} -> {
            |    200 -> Todo[]
            |    404 -> Error
            |}
        """.trimMargin(),
        WsEndpointDef, CustomType, Method, Path, ForwardSlash, LeftCurly, CustomValue, Colon, WsString,
        RightCurly, Arrow, LeftCurly, StatusCode, Arrow, CustomType, Brackets, StatusCode, Arrow, CustomType,
        RightCurly, EndOfProgram,
    )

    @Test
    fun testPOSTWithBodyTokenizer() = testTokenizer(
        """
            |endpoint PostTodo Todo POST /todos -> {
            |    200 -> Todo
            |}
        """.trimMargin(),
        WsEndpointDef, CustomType, CustomType, Method, Path, Arrow, LeftCurly,
        StatusCode, Arrow, CustomType, RightCurly, EndOfProgram,
    )

    @Test
    fun testQueryParamsTokenizer() = testTokenizer(
        """
            |endpoint GetTodos GET /todos
            |?{name: String, date: String} -> {
            |    200 -> Todo[]
            |}
        """.trimMargin(),
        WsEndpointDef, CustomType, Method, Path, QuestionMark, LeftCurly, CustomValue, Colon,
        WsString, Comma, CustomValue, Colon, WsString, RightCurly, Arrow, LeftCurly,
        StatusCode, Arrow, CustomType, Brackets, RightCurly, EndOfProgram,
    )

    @Test
    fun testHeadersTokenizer() = testTokenizer(
        """
            |endpoint GetTodos GET /todos
            |#{version: String, accept: String} -> {
            |    200 -> Todo[]
            |}
        """.trimMargin(),
        WsEndpointDef, CustomType, Method, Path, Hash, LeftCurly, CustomValue, Colon,
        WsString, Comma, CustomValue, Colon, WsString, RightCurly, Arrow, LeftCurly,
        StatusCode, Arrow, CustomType, Brackets, RightCurly, EndOfProgram,
    )
}
