package community.flock.wirespec.compiler.test

object CompileTypeTest : Fixture {

    override val source =
        // language=ws
        """
        |type Request {
        |  `type`: String,
        |  url: String,
        |  `BODY_TYPE`: String?,
        |  params: String[],
        |  headers: { String },
        |  body: { String?[]? }?
        |}
        """.trimMargin()

    override val compiler = source.let(::compile)
}
