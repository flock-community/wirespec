package community.flock.wirespec.compiler.core.tokenize

import community.flock.wirespec.compiler.core.WsCustomType
import kotlin.test.Test

class TokenizeImportTest {

    @Test
    fun testImportTokenize() = testTokenizer(
        """
            |import { Bar } "./bar.ws"
            |type Foo {
            |    bar: Bar
            |}
        """.trimMargin(),
        ImportDefinition, LeftCurly, WsCustomType, RightCurly, Literal,
        TypeDefinition, WsCustomType, LeftCurly,
        CustomValue, Colon, WsCustomType,
        RightCurly,
        EndOfProgram,
    )
}
