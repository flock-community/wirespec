package community.flock.wirespec.compiler.test

public object CompileRpcTest : Fixture {

    override val source: String =
        // language=ws
        """
        |type User { name: String }
        |rpc GetUser {
        |  id: String
        |} -> User ! String
        |rpc Ping {} -> String
        """.trimMargin()

    override val compiler: Compiler = source.let(::compile)
}
