package community.flock.wirespec.compiler.test

object CompileUnionTest {

    val compiler =
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
        """.trimMargin().let(::compile)
}
