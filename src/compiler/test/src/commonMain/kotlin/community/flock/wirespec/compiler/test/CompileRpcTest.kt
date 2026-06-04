package community.flock.wirespec.compiler.test

object CompileRpcTest : Fixture {

    override val source =
        // language=ws
        """
        |type Todo { name: String }
        |type Error { code: String }
        |rpc CreateTodo(todo: Todo) -> Todo ! Error
        """.trimMargin()

    override val compiler = source.let(::compile)
}
