package community.flock.wirespec.compiler.test

object CompileEnumTest {

    val compiler =
        // language=ws
        """
        |enum MyAwesomeEnum {
        |  ONE, Two, THREE_MORE, UnitedKingdom
        |}
        """.trimMargin().let(::compile)
}
