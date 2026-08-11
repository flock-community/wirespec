package community.flock.wirespec.compiler.test

object CompileAnyTest : Fixture {

    override val source =
        // language=ws
        """
        |type Message {
        |  payload: Any,
        |  metadata: Any?,
        |  attachments: Any[],
        |  extensions: { Any }
        |}
        """.trimMargin()

    override val compiler = source.let(::compile)
}
