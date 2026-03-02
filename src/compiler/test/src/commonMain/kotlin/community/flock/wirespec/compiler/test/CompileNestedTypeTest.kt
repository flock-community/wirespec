package community.flock.wirespec.compiler.test

object CompileNestedTypeTest : Fixture {

    override val source =
        // language=ws
        """
        |type DutchPostalCode = String(/^([0-9]{4}[A-Z]{2})${'$'}/g)
        |
        |type Address {
        |  street: String,
        |  houseNumber: Integer,
        |  postalCode: DutchPostalCode
        |}
        |
        |type Person {
        |  name: String,
        |  address: Address,
        |  tags: String[]
        |}
        """.trimMargin()

    override val compiler = source.let(::compile)
}
