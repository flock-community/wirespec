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
        |graphql GetPet Query {id: String} -> Pet?
        |
        |graphql AddPet Mutation {input: PetInput} -> Pet
        |
        |graphql OnPetAdded Subscription {} -> Pet
        """.trimMargin()

    override val compiler = source.let(::compile)
}
