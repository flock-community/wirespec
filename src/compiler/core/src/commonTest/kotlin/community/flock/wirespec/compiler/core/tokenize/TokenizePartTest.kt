package community.flock.wirespec.compiler.core.tokenize

import community.flock.wirespec.compiler.core.WirespecType
import kotlin.test.Test

class TokenizePartTest {

    @Test
    fun testTokenizePartAndSpread() = testTokenizer(
        // language=ws
        """
        |part Audit { createdAt: String }
        |type User { ...Audit, partner: String }
        """.trimMargin(),
        PartDefinition, WirespecType, LeftCurly, DromedaryCaseIdentifier, Colon, WsString, RightCurly,
        TypeDefinition, WirespecType, LeftCurly, Ellipsis, WirespecType, Comma, DromedaryCaseIdentifier, Colon, WsString, RightCurly,
        EndOfProgram,
    )
}
