package community.flock.wirespec.converter.graphql

import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.utils.NoLogger
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class GraphQLEmitterTest : NoLogger {

    private fun roundTrip(sdl: String): String = GraphQLParser
        .parse(ModuleContent(FileUri("schema.graphqls"), sdl), strict = false)
        .let { GraphQLEmitter.emit(it, logger) }
        .head.result

    @Test
    fun emitCanonicalSchema() {
        val sdl =
            // language=graphql
            """
            |scalar DateTime
            |
            |"A pet in the store"
            |type Pet implements Node {
            |  id: ID!
            |  name: String
            |  status: Status
            |  createdAt: DateTime
            |  tags: [String!]!
            |  posts(first: Int, offset: Int): [Post]
            |}
            |
            |interface Node {
            |  id: ID!
            |}
            |
            |type Post {
            |  id: ID!
            |  body: String @deprecated(reason: "use content")
            |}
            |
            |enum Status {
            |  AVAILABLE
            |  SOLD
            |}
            |
            |union SearchResult = Pet | Post
            |
            |input PetInput {
            |  name: String!
            |}
            |
            |type Query {
            |  pet(id: ID!): Pet
            |  search(text: String!): [SearchResult!]
            |}
            |
            |type Mutation {
            |  addPet(input: PetInput!): Pet!
            |}
            |
            |type Subscription {
            |  petAdded: Pet!
            |}
            """.trimMargin()

        roundTrip(sdl) shouldBe sdl + "\n"
    }

    @Test
    fun emittedSchemaIsStableUnderReparse() {
        val sdl =
            // language=graphql
            """
            type Query { pet(id: ID!): Pet }
            type Pet { id: ID!, name: String, friend: Pet }
            input PetInput { name: String! }
            enum Status { A B }
            """.trimIndent()

        val once = roundTrip(sdl)
        val twice = roundTrip(once)
        twice shouldBe once
    }
}
