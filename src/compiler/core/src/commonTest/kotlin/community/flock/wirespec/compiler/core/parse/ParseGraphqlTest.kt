package community.flock.wirespec.compiler.core.parse

import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.core.parse.ast.Graphql
import community.flock.wirespec.compiler.core.parse.ast.Module
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.compiler.utils.NoLogger
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.test.Test

class ParseGraphqlTest {

    private fun parser(source: String) = object : ParseContext, NoLogger {
        override val spec = WirespecSpec
    }.parse(nonEmptyListOf(ModuleContent(FileUri("test.ws"), source))).map { it.modules.flatMap(Module::statements) }

    @Test
    fun testQueryWithArguments() {
        val source =
            // language=ws
            """
            |type Pet { id: String, name: String? }
            |graphql GetPet Query {id: String} -> Pet?
            """.trimMargin()

        parser(source)
            .shouldBeRight()
            .shouldHaveSize(2)[1]
            .shouldBeInstanceOf<Graphql>()
            .run {
                identifier.value shouldBe "GetPet"
                kind shouldBe Graphql.Kind.Query
                operation shouldBe "getPet"
                inputs.shouldHaveSize(1).first().run {
                    identifier.value shouldBe "id"
                    reference.shouldBeInstanceOf<Reference.Primitive>().type.shouldBeInstanceOf<Reference.Primitive.Type.String>()
                }
                output.shouldBeInstanceOf<Reference.Custom>().run {
                    value shouldBe "Pet"
                    isNullable shouldBe true
                }
            }
    }

    @Test
    fun testQueryWithoutArguments() {
        val source =
            // language=ws
            """
            |type Pet { id: String }
            |graphql GetPets Query {} -> Pet[]
            """.trimMargin()

        parser(source)
            .shouldBeRight()
            .shouldHaveSize(2)[1]
            .shouldBeInstanceOf<Graphql>()
            .run {
                identifier.value shouldBe "GetPets"
                kind shouldBe Graphql.Kind.Query
                operation shouldBe "getPets"
                inputs.shouldHaveSize(0)
                output.shouldBeInstanceOf<Reference.Iterable>().run {
                    isNullable shouldBe false
                    reference.shouldBeInstanceOf<Reference.Custom>().value shouldBe "Pet"
                }
            }
    }

    @Test
    fun testMutation() {
        val source =
            // language=ws
            """
            |type Pet { id: String }
            |type PetInput { name: String }
            |graphql AddPet Mutation {input: PetInput, tag: String?} -> Pet
            """.trimMargin()

        parser(source)
            .shouldBeRight()
            .shouldHaveSize(3)[2]
            .shouldBeInstanceOf<Graphql>()
            .run {
                kind shouldBe Graphql.Kind.Mutation
                operation shouldBe "addPet"
                inputs.shouldHaveSize(2)
                inputs[0].reference.shouldBeInstanceOf<Reference.Custom>().value shouldBe "PetInput"
                inputs[1].reference.isNullable shouldBe true
            }
    }

    @Test
    fun testSubscription() {
        val source =
            // language=ws
            """
            |type Pet { id: String }
            |graphql OnPetAdded Subscription {} -> Pet
            """.trimMargin()

        parser(source)
            .shouldBeRight()
            .shouldHaveSize(2)[1]
            .shouldBeInstanceOf<Graphql>()
            .kind shouldBe Graphql.Kind.Subscription
    }

    @Test
    fun testWrongKind() {
        val source =
            // language=ws
            """
            |type Pet { id: String }
            |graphql GetPet Fetch {} -> Pet
            """.trimMargin()

        parser(source)
            .shouldBeLeft()
            .first()
            .message
            .shouldContain("Query, Mutation or Subscription expected")
    }

    @Test
    fun testDuplicateOperationPerKind() {
        val source =
            // language=ws
            """
            |type Pet { id: String }
            |graphql GetPet Query {} -> Pet
            |graphql GetPetQuery Query {} -> Pet
            """.trimMargin()

        parser(source)
            .shouldBeLeft()
            .first()
            .message
            .shouldContain("Graphql Query field 'getPet' is already defined")
    }

    @Test
    fun testSameOperationDifferentKindIsAllowed() {
        val source =
            // language=ws
            """
            |type Pet { id: String }
            |graphql GetPet Query {} -> Pet
            |graphql GetPetMutation Mutation {id: String} -> Pet
            """.trimMargin()

        parser(source).shouldBeRight().shouldHaveSize(3)
    }

    @Test
    fun testNestedFieldParameters() {
        val source =
            // language=ws
            """
            |type Post { id: String }
            |type User { posts(first: Integer?, after: String?): Post[] }
            |graphql GetUser Query {id: String} -> User?
            """.trimMargin()

        parser(source)
            .shouldBeRight()
            .shouldHaveSize(3)[1]
            .shouldBeInstanceOf<Type>()
            .shape.value.shouldHaveSize(1)
            .first()
            .run {
                identifier.value shouldBe "posts"
                parameters.shouldHaveSize(2)
                parameters[0].identifier.value shouldBe "first"
                parameters[0].reference.isNullable shouldBe true
                parameters[1].identifier.value shouldBe "after"
            }
    }
}
