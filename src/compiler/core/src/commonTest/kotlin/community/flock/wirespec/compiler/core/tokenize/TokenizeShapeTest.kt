package community.flock.wirespec.compiler.core.tokenize

import community.flock.wirespec.compiler.core.WirespecType
import kotlin.test.Test

class TokenizeShapeTest {

    @Test
    fun testTokenizeShape() = testTokenizer(
        // language=ws
        """
        |type Foo {
        |  dictA: {String},
        |  dictB: {String[]}
        |}
        """.trimMargin(),
        TypeDefinition, WirespecType, LeftCurly,
        DromedaryCaseIdentifier, Colon, LeftCurly, WsString, RightCurly, Comma,
        DromedaryCaseIdentifier, Colon, LeftCurly, WsString, Brackets, RightCurly,
        RightCurly, EndOfProgram,
    )
}
