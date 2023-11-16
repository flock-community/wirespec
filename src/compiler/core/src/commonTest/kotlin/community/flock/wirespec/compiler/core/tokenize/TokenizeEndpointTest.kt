package community.flock.wirespec.compiler.core.tokenize

import community.flock.wirespec.compiler.core.Wirespec
import community.flock.wirespec.compiler.core.tokenize.types.Arrow
import community.flock.wirespec.compiler.core.tokenize.types.Brackets
import community.flock.wirespec.compiler.core.tokenize.types.Colon
import community.flock.wirespec.compiler.core.tokenize.types.CustomType
import community.flock.wirespec.compiler.core.tokenize.types.CustomValue
import community.flock.wirespec.compiler.core.tokenize.types.EndOfProgram
import community.flock.wirespec.compiler.core.tokenize.types.ForwardSlash
import community.flock.wirespec.compiler.core.tokenize.types.Invalid
import community.flock.wirespec.compiler.core.tokenize.types.LeftCurly
import community.flock.wirespec.compiler.core.tokenize.types.Method
import community.flock.wirespec.compiler.core.tokenize.types.Path
import community.flock.wirespec.compiler.core.tokenize.types.RightCurly
import community.flock.wirespec.compiler.core.tokenize.types.StartOfProgram
import community.flock.wirespec.compiler.core.tokenize.types.StatusCode
import community.flock.wirespec.compiler.core.tokenize.types.WsEndpointDef
import community.flock.wirespec.compiler.core.tokenize.types.WsString
import io.kotest.assertions.arrow.core.shouldNotContain
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class TokenizeEndpointTest {

    @Test
    fun testEmptySource() {
        val source = ""

        val expected = listOf(StartOfProgram, EndOfProgram)

        Wirespec.tokenize(source)
            .also { it.size shouldBe expected.size }
            .onEachIndexed { index, token -> token.type shouldBe expected[index] }
    }

    @Test
    fun testSourceLengthOfOneCharacterSource() {
        val source = "t"

        val expected = listOf(CustomValue, EndOfProgram)

        Wirespec.tokenize(source).removeWhiteSpace()
            .also { it.size shouldBe expected.size }
            .onEachIndexed { index, token -> token.type shouldBe expected[index] }
    }

    @Test
    fun testStatusCodeTokenize() {
        val source = """
            000 099 100 199 200 299 300 399 400 499 500 599 600 699
        """.trimIndent()

        val expected = listOf(
            Invalid, Invalid, Invalid, Invalid, Invalid, Invalid,
            StatusCode, StatusCode, StatusCode, StatusCode, StatusCode,
            StatusCode, StatusCode, StatusCode, StatusCode, StatusCode,
            Invalid, Invalid, Invalid, Invalid, Invalid, Invalid,
            EndOfProgram
        )

        Wirespec.tokenize(source).removeWhiteSpace()
            .also { it.size shouldBe expected.size }
            .onEachIndexed { index, token -> token.type shouldBe expected[index] }
    }

    @Test
    fun testEndpointTokenizer() {
        val source = """
            endpoint GetTodos GET /todos/{id: String} -> {
                200 -> Todo[]
                404 -> Error
            }

        """.trimIndent()

        val expected = listOf(
            WsEndpointDef, CustomType, Method, Path, ForwardSlash, LeftCurly, CustomValue, Colon, WsString,
            RightCurly, Arrow, LeftCurly, StatusCode, Arrow, CustomType, Brackets, StatusCode, Arrow, CustomType,
            RightCurly, EndOfProgram,
        )

        Wirespec.tokenize(source).removeWhiteSpace().run {
            size shouldBe expected.size
            map { it.type } shouldNotContain Invalid
            onEachIndexed { index, token -> token.type shouldBe expected[index] }
        }
    }
}
