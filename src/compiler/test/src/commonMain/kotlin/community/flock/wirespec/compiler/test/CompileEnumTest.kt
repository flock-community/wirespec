package community.flock.wirespec.compiler.test

object CompileEnumTest {

    val compiler =
        // language=ws
        """
        |enum MyAwesomeEnum {
        |  ONE, Two, THREE_MORE, UnitedKingdom, -1, 0, 10, -999, 88
        |}
        """.trimMargin().let(::compile)
}
