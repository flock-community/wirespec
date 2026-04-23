package community.flock.wirespec.compiler.test

object CompileGeneratorTest : Fixture {

    override val source =
        // language=ws
        """
        |type UUID = String(/^[0-9a-f]{8}${'$'}/g)
        |
        |enum Color {
        |  RED, GREEN, BLUE
        |}
        |
        |type Address {
        |  street: String,
        |  number: Integer,
        |  postalCode: UUID
        |}
        |
        |type Person {
        |  name: String,
        |  age: Integer,
        |  addresses: Address[],
        |  favoriteColor: Color,
        |  nickname: String?
        |}
        |
        |type Contact = Person | Address
        """.trimMargin()

    override val compiler = source.let(::compile)
}
