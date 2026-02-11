package community.flock.wirespec.compiler.test

object CompileEnumTest {

    val source =
        // language=ws
        """
        |enum MyAwesomeEnum {
        |  ONE, Two, THREE_MORE, UnitedKingdom, -1, 0, 10, -999, 88
        |}
        """.trimMargin()

    val compiler = source.let(::compile)
}
