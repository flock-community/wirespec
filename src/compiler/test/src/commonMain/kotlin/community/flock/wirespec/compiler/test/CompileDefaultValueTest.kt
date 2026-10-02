package community.flock.wirespec.compiler.test

public object CompileDefaultValueTest : Fixture {

    override val source: String =
        // language=ws
        """
        |type Settings {
        |  name: String = "anonymous",
        |  greeting: String = "Hello \"${'$'}name\"",
        |  retries: Integer = 3,
        |  offset: Integer = -1,
        |  port: Integer32 = 8080,
        |  ratio: Number = 1.5,
        |  scale: Number = 2,
        |  weight: Number32 = 0.5,
        |  active: Boolean = true,
        |  nickname: String? = null,
        |  limit: Integer? = 10,
        |  tags: String[]
        |}
        """.trimMargin()

    override val compiler: Compiler = source.let(::compile)
}
