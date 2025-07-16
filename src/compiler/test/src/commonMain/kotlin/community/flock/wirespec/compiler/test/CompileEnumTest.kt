package community.flock.wirespec.compiler.test

object CompileEnumTest {

    val compiler = """
        |enum MyAwesomeEnum {
        |  ONE, Two, THREE_MORE, UnitedKingdom
        |}
    """.trimMargin().let(::compile)
}
