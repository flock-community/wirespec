package community.flock.wirespec.compiler.test

public object CompileAnyTest : Fixture {

    override val source: String =
        // language=ws
        """
        |type Message {
        |  payload: Any,
        |  metadata: Any?,
        |  attachments: Any[],
        |  extensions: { Any }
        |}
        """.trimMargin()

    override val compiler: Compiler = source.let(::compile)
}
