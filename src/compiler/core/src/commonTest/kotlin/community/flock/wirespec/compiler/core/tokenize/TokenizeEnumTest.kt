package community.flock.wirespec.compiler.core.tokenize

import community.flock.wirespec.compiler.core.WsCustomType
import kotlin.test.Test

class TokenizeEnumTest {

    @Test
    fun testEnumTokenize() = testTokenizer(
        """
            |enum Foo {
            |    FOO,
            |    DELETE_BAR
            |}
        """.trimMargin(),
        EnumTypeDefinition, WsCustomType, LeftCurly,
        WsCustomType, Comma,
        WsCustomType,
        RightCurly,
        EndOfProgram,
    )


}
