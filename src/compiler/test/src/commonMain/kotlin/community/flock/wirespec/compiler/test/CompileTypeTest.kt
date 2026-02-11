package community.flock.wirespec.compiler.test

object CompileTypeTest {

    val source =
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

    val compiler = source.let(::compile)
}
