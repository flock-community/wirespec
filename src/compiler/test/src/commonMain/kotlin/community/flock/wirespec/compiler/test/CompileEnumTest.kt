package community.flock.wirespec.compiler.test

object CompileEnumTest : Fixture {

    override val source =
        // language=ws
        """
        |enum MyAwesomeEnum {
        |  ONE, Two, THREE_MORE, UnitedKingdom, -1, 0, 10, -999, 88
        |}
        """.trimMargin()

    override val compiler = source.let(::compile)
}
