package community.flock.wirespec.compiler.test

public object CompileTypeTest : Fixture {

    override val source: String =
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

    override val compiler: Compiler = source.let(::compile)
}
