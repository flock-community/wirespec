package community.flock.wirespec.compiler.test

public object CompileLiteralTest : Fixture {

    override val source: String =
        // language=ws
        """
        |type Cat {
        |  kind: "cat",
        |  name: String
        |}
        |
        |type Dog {
        |  kind: "dog",
        |  nickname: "good \"boy\""?,
        |  tags: "pet"[],
        |  tagsByKey: {"pet"}
        |}
        |
        |endpoint GetDogs GET /dogs ?{sort: "name"} -> {
        |  200 -> Dog[]
        |}
        """.trimMargin()

    override val compiler: Compiler = source.let(::compile)
}
