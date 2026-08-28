package community.flock.wirespec.compiler.test

object CompileGraphqlTest : Fixture {

    override val source =
        // language=ws
        """
        |type Pet {
        |  id: String,
        |  name: String?,
        |  tags: String[]
        |}
        |
        |type PetInput {
        |  name: String
        |}
        |
        |graphql GetPet Query pet(id: String) -> Pet?
        |
        |graphql AddPet Mutation addPet(input: PetInput) -> Pet
        |
        |graphql OnPetAdded Subscription petAdded -> Pet
        """.trimMargin()

    override val compiler = source.let(::compile)
}
