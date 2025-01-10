package community.flock.wirespec.compiler.core.tokenize

import community.flock.wirespec.compiler.core.WsCustomType
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
        TypeDefinition, WsCustomType, LeftCurly,
        CustomValue, Colon, LeftCurly, WsString, RightCurly, Comma,
        CustomValue, Colon, LeftCurly, WsString, Brackets, RightCurly,
        RightCurly, EndOfProgram,
    )
}
