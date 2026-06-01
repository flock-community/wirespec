package community.flock.wirespec.compiler.test

object CompileRpcTest : Fixture {

    override val source =
        // language=ws
        """
        |type Todo { name: String }
        |rpc CreateTodo(todo: Todo) -> Todo
        """.trimMargin()

    override val compiler = source.let(::compile)
}
