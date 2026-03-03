package community.flock.wirespec.compiler.test

object CompileRefinedTest : Fixture {

    override val source =
        // language=ws
        """
        |type TodoId = String(/^[0-9a-fA-F]{8}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{12}$/g)
        |type TodoNoRegex = String
        |
        |type TestInt = Integer
        |type TestInt0 = Integer(_,_)
        |type TestInt1 = Integer(0,_)
        |type TestInt2 = Integer(1,3)
        |
        |type TestNum = Number
        |type TestNum0 = Number(_,_)
        |type TestNum1 = Number(_,0.5)
        |type TestNum2 = Number(-0.2,0.5)
        |
        """.trimMargin()

    override val compiler = source.let(::compile)
}
