package community.flock.wirespec.compiler.test

object CompileRefinedTest {

    val compiler = """
        |type TodoId = String(/^[0-9a-fA-F]{8}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{12}$/g)
    """.trimMargin().let(::compile)
}
