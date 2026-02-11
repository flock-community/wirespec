package community.flock.wirespec.compiler.test

object CompileChannelTest {

    val source =
        // language=ws
        """
        |channel Queue -> String
        """.trimMargin()

    val compiler = source.let(::compile)
}
