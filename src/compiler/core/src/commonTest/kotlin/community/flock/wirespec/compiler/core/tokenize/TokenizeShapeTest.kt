package community.flock.wirespec.compiler.core.tokenize

import community.flock.wirespec.compiler.core.tokenize.types.Brackets
import community.flock.wirespec.compiler.core.tokenize.types.Colon
import community.flock.wirespec.compiler.core.tokenize.types.Comma
import community.flock.wirespec.compiler.core.tokenize.types.CustomType
import community.flock.wirespec.compiler.core.tokenize.types.CustomValue
import community.flock.wirespec.compiler.core.tokenize.types.EndOfProgram
import community.flock.wirespec.compiler.core.tokenize.types.LeftCurly
import community.flock.wirespec.compiler.core.tokenize.types.RightCurly
import community.flock.wirespec.compiler.core.tokenize.types.WsString
import community.flock.wirespec.compiler.core.tokenize.types.WsTypeDef
import kotlin.test.Test

class TokenizeShapeTest {

    @Test
    fun testTokenizeShape() = testTokenizer(
        """
            |type Foo {
            |  dictA: {String},
            |  dictB: {String[]}
            |}
        """.trimMargin(),
        WsTypeDef, CustomType, LeftCurly,
        CustomValue, Colon, LeftCurly, WsString, RightCurly, Comma,
        CustomValue, Colon, LeftCurly, WsString, Brackets, RightCurly,
        RightCurly, EndOfProgram,
    )
}
