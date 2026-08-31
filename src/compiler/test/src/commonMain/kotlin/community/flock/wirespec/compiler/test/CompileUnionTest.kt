package community.flock.wirespec.compiler.test

public object CompileUnionTest : Fixture {

    override val source: String =
        // language=ws
        """
        |type UserAccount = UserAccountPassword | UserAccountToken
        |type UserAccountPassword {
        |  username: String,
        |  password: String
        |}
        |type UserAccountToken {
        |  token: String
        |}
        |type User {
        |   username: String,
        |   account: UserAccount
        |}
        """.trimMargin()

    override val compiler: Compiler = source.let(::compile)
}
