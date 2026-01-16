package community.flock.wirespec.compiler.test

object CompileChannelTest {

    val compiler =
        // language=ws
        """
        |channel Queue -> String
        """.trimMargin().let(::compile)
}
