package community.flock.wirespec.compiler.test

object CompileEnumTest {

    val compiler =
        // language=ws
        """
        |enum MyAwesomeEnum {
        |  ONE, Two, THREE_MORE, UnitedKingdom
        |}
        """.trimMargin().let(::compile)

    val negativeCompiler =
        // language=ws
        """
        |enum InnerErrorCode {
        |  0, 1, -1, 2, -999
        |}
        """.trimMargin().let(::compile)
}
