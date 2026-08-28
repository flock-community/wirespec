package community.flock.wirespec.compiler.test

public object CompileChannelTest : Fixture {

    override val source: String =
        // language=ws
        """
        |channel Queue -> String
        """.trimMargin()

    override val compiler: Compiler = source.let(::compile)
}
