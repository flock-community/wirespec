package community.flock.wirespec.compiler.test

public object CompileEnumTest : Fixture {

    override val source: String =
        // language=ws
        """
        |enum MyAwesomeEnum {
        |  ONE, Two, THREE_MORE, UnitedKingdom, -1, 0, 10, -999, 88
        |}
        """.trimMargin()

    override val compiler: Compiler = source.let(::compile)
}
