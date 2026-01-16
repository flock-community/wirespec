package community.flock.wirespec.compiler.core.tokenize

import community.flock.wirespec.compiler.core.WirespecType
import kotlin.test.Test

class TokenizeEnumTest {

    @Test
    fun testEnumTokenize() = testTokenizer(
        // language=ws
        """
        |enum Foo {
        |    FOO,
        |    DELETE_BAR
        |}
        """.trimMargin(),
        EnumTypeDefinition, WirespecType, LeftCurly,
        WirespecType, Comma,
        WirespecType,
        RightCurly,
        EndOfProgram,
    )
}
