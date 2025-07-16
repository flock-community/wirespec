package community.flock.wirespec.compiler.test

object CompileTypeTest {

    val compiler = """
        |type Request {
        |  `type`: String,
        |  url: String,
        |  `BODY_TYPE`: String?,
        |  params: String[],
        |  headers: { String },
        |  body: { String?[]? }?
        |}
    """.trimMargin().let(::compile)
}
