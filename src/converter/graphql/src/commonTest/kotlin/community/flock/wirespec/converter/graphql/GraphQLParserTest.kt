package community.flock.wirespec.converter.graphql

import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.parse.ast.Enum
import community.flock.wirespec.compiler.core.parse.ast.Graphql
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.compiler.core.parse.ast.Union
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.test.Test

class GraphQLParserTest {

    private fun parse(sdl: String) = GraphQLParser
        .parse(ModuleContent(FileUri("schema.graphqls"), sdl), strict = false)
        .modules.head.statements.toList()

    @Test
    fun parseObjectType() {
        val statements = parse(
            // language=graphql
            """
            "A pet in the store"
            type Pet @deprecated(reason: "old") {
              id: ID!
              name: String
              tags: [String!]!
              friends: [Pet]
            }
            """.trimIndent(),
        )

        statements.shouldHaveSize(1).first().shouldBeInstanceOf<Type>().run {
            comment?.value shouldBe "A pet in the store"
            identifier.value shouldBe "Pet"
            annotations.shouldHaveSize(1).first().run {
                name shouldBe "deprecated"
                parameters.shouldHaveSize(1).first().name shouldBe "reason"
            }
            shape.value.shouldHaveSize(4).run {
                get(0).run {
                    identifier.value shouldBe "id"
                    annotations.map { it.name } shouldContainExactly listOf("id")
                    reference.shouldBeInstanceOf<Reference.Primitive>().run {
                        type.shouldBeInstanceOf<Reference.Primitive.Type.String>()
                        isNullable shouldBe false
                    }
                }
                get(1).reference.isNullable shouldBe true
                get(2).reference.shouldBeInstanceOf<Reference.Iterable>().run {
                    isNullable shouldBe false
                    reference.shouldBeInstanceOf<Reference.Primitive>().isNullable shouldBe false
                }
                get(3).reference.shouldBeInstanceOf<Reference.Iterable>().run {
                    isNullable shouldBe true
                    reference.shouldBeInstanceOf<Reference.Custom>().run {
                        value shouldBe "Pet"
                        isNullable shouldBe true
                    }
                }
            }
        }
    }

    @Test
    fun parseInputInterfaceEnumUnionScalar() {
        val statements = parse(
            // language=graphql
            """
            scalar DateTime

            enum Status { AVAILABLE SOLD }

            interface Node {
              id: ID!
            }

            type Pet implements Node {
              id: ID!
              status: Status
              createdAt: DateTime
            }

            union SearchResult = Pet | Owner

            type Owner {
              name: String!
            }

            input PetInput {
              name: String!
            }
            """.trimIndent(),
        )

        statements.shouldHaveSize(7)
        statements[0].shouldBeInstanceOf<Refined>().identifier.value shouldBe "DateTime"
        statements[1].shouldBeInstanceOf<Enum>().entries shouldBe setOf("AVAILABLE", "SOLD")
        statements[2].shouldBeInstanceOf<Type>().run {
            annotations.map { it.name } shouldContainExactly listOf(GraphQLConverter.INTERFACE_MARKER)
        }
        statements[3].shouldBeInstanceOf<Type>().run {
            extends.map { it.value } shouldContainExactly listOf("Node")
            shape.value[2].reference.shouldBeInstanceOf<Reference.Custom>().value shouldBe "DateTime"
        }
        statements[4].shouldBeInstanceOf<Union>().entries.map { it.value } shouldContainExactly listOf("Pet", "Owner")
        statements[5].shouldBeInstanceOf<Type>().identifier.value shouldBe "Owner"
        statements[6].shouldBeInstanceOf<Type>().run {
            annotations.map { it.name } shouldContainExactly listOf(GraphQLConverter.INPUT_MARKER)
        }
    }

    @Test
    fun parseOperations() {
        val statements = parse(
            // language=graphql
            """
            type Query {
              pet(id: ID!): Pet
              pets: [Pet!]!
            }

            type Mutation {
              addPet(input: PetInput!): Pet!
            }

            type Subscription {
              petAdded: Pet!
            }

            type Pet { id: ID! }
            input PetInput { name: String! }
            """.trimIndent(),
        )

        val graphqls = statements.filterIsInstance<Graphql>()
        graphqls.shouldHaveSize(4)
        graphqls[0].run {
            identifier.value shouldBe "PetQuery"
            kind shouldBe Graphql.Kind.Query
            operation shouldBe "pet"
            inputs.shouldHaveSize(1).first().run {
                identifier.value shouldBe "id"
                reference.isNullable shouldBe false
            }
            output.shouldBeInstanceOf<Reference.Custom>().run {
                value shouldBe "Pet"
                isNullable shouldBe true
            }
        }
        graphqls[1].identifier.value shouldBe "PetsQuery"
        graphqls[2].run {
            identifier.value shouldBe "AddPetMutation"
            kind shouldBe Graphql.Kind.Mutation
        }
        graphqls[3].run {
            identifier.value shouldBe "PetAddedSubscription"
            kind shouldBe Graphql.Kind.Subscription
        }
        statements.filterIsInstance<Type>().shouldHaveSize(2)
    }

    @Test
    fun parseSchemaBlockWithCustomRootNames() {
        val statements = parse(
            // language=graphql
            """
            schema {
              query: MyQuery
            }

            type MyQuery {
              pet: Pet
            }

            type Pet { id: ID! }
            """.trimIndent(),
        )

        statements.filterIsInstance<Graphql>().shouldHaveSize(1).first().kind shouldBe Graphql.Kind.Query
        statements.filterIsInstance<Type>().shouldHaveSize(1)
    }

    @Test
    fun parseNestedFieldArguments() {
        val statements = parse(
            // language=graphql
            """
            type User {
              posts(first: Int, after: String): [Post!]!
            }

            type Post { id: ID! }
            """.trimIndent(),
        )

        statements.first().shouldBeInstanceOf<Type>().shape.value.first().run {
            identifier.value shouldBe "posts"
            parameters.shouldHaveSize(2)
            parameters[0].identifier.value shouldBe "first"
            parameters[0].reference.shouldBeInstanceOf<Reference.Primitive>().run {
                type.shouldBeInstanceOf<Reference.Primitive.Type.Integer>().precision shouldBe Reference.Primitive.Type.Precision.P32
                isNullable shouldBe true
            }
        }
    }

    @Test
    fun parseDescriptionsAndComments() {
        val statements = parse(
            // language=graphql
            """
            # a line comment
            \"\"\"
            Block description
            over two lines
            \"\"\"
            type Pet {
              id: ID!
            }
            """.trimIndent().replace("\\\"", "\""),
        )

        statements.first().shouldBeInstanceOf<Type>().comment?.value shouldBe "Block description\nover two lines"
    }
}
