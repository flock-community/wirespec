package community.flock.wirespec.compiler.core.tokenize

import community.flock.wirespec.compiler.core.Wirespec
import community.flock.wirespec.compiler.core.tokenize.types.Arrow
import community.flock.wirespec.compiler.core.tokenize.types.Brackets
import community.flock.wirespec.compiler.core.tokenize.types.CustomType
import community.flock.wirespec.compiler.core.tokenize.types.EndOfProgram
import community.flock.wirespec.compiler.core.tokenize.types.GET
import community.flock.wirespec.compiler.core.tokenize.types.Invalid
import community.flock.wirespec.compiler.core.tokenize.types.LeftCurly
import community.flock.wirespec.compiler.core.tokenize.types.Path
import community.flock.wirespec.compiler.core.tokenize.types.RightCurly
import community.flock.wirespec.compiler.core.tokenize.types.StatusCode
import community.flock.wirespec.compiler.core.tokenize.types.WsEndpointDef
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TokenizeEndpointTest {

    @Test
    fun testEmptySource() {
        val source = ""

        val expected = listOf(EndOfProgram)

        Wirespec.tokenize(source).removeWhiteSpace().run {
            onEach { println(it.type) }
            assertEquals(expected.size, size)
            onEachIndexed { index, token -> assertEquals(expected[index], token.type) }
        }
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

        Wirespec.tokenize(source).removeWhiteSpace().run {
            onEach { println(it.type) }
            assertEquals(expected.size, size)
            onEachIndexed { index, token -> assertEquals(expected[index], token.type) }
        }
    }

    @Test
    fun testTokenizer() {
        val source = """
            endpoint GetTodos GET /todos {
                200 -> Todo[]
                404 -> Error
            }

        """.trimIndent()

        val expected = listOf(
            WsEndpointDef, CustomType, GET, Path, LeftCurly, StatusCode,
            Arrow, CustomType, Brackets, StatusCode, Arrow, CustomType, RightCurly,
            EndOfProgram,
        )

        Wirespec.tokenize(source).removeWhiteSpace().run {
            onEach { println(it.type) }
            assertTrue(none { it.type is Invalid })
            assertEquals(expected.size, size)
            onEachIndexed { index, token -> assertEquals(expected[index], token.type) }
        }
    }
}
