package community.flock.wirespec.compiler.test

object CompileChannelTest {

    val compiler = """
        |channel Queue -> String
    """.trimMargin().let(::compile)
}
