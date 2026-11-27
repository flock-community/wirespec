package community.flock.wirespec.compiler.test

public object CompileLiteralTest : Fixture {

    override val source: String =
        // language=ws
        """
        |type Dog {
        |  kind: "dog",
        |  name: String
        |}
        """.trimMargin()

    override val compiler: Compiler = source.let(::compile)
}
