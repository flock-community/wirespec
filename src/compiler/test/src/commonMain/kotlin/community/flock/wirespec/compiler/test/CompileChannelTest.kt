package community.flock.wirespec.compiler.test

object CompileChannelTest : Fixture {

    override val source =
        // language=ws
        """
        |channel Queue -> String
        """.trimMargin()

    override val compiler = source.let(::compile)
}
