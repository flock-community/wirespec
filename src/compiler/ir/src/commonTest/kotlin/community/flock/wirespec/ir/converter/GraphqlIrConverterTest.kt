package community.flock.wirespec.ir.converter

import arrow.core.getOrElse
import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.core.parse.ast.Graphql
import community.flock.wirespec.compiler.core.parse.ast.Module
import community.flock.wirespec.compiler.utils.NoLogger
import community.flock.wirespec.ir.core.Field
import community.flock.wirespec.ir.core.Function
import community.flock.wirespec.ir.core.Literal
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.core.Namespace
import community.flock.wirespec.ir.core.ReturnStatement
import community.flock.wirespec.ir.core.Struct
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class GraphqlIrConverterTest {

    private fun parseModule(source: String): Module = object : ParseContext, NoLogger {
        override val spec = WirespecSpec
    }.parse(nonEmptyListOf(ModuleContent(FileUri("test.ws"), source)))
        .map { it.modules.first() }
        .getOrElse { fail("Parse failed: $it") }

    private fun Module.document(name: String): String {
        val graphql = statements.filterIsInstance<Graphql>().first { it.identifier.value == name }
        val namespace = graphql.convert(this).elements.filterIsInstance<Namespace>().first()
        val function = namespace.elements.filterIsInstance<Function>().first { it.name == Name.of("document") }
        return function.body.filterIsInstance<ReturnStatement>().first().expression.let { (it as Literal).value as String }
    }

    private fun Module.inputFields(name: String): List<String> {
        val graphql = statements.filterIsInstance<Graphql>().first { it.identifier.value == name }
        val namespace = graphql.convert(this).elements.filterIsInstance<Namespace>().first()
        return namespace.elements.filterIsInstance<Struct>().first { it.name == Name.of("Input") }
            .fields.filterIsInstance<Field>().map { it.name.camelCase() }
    }

    @Test
    fun documentWithVariablesAndSelection() {
        val module = parseModule(
            """
            type Pet {
                id: String,
                name: String?
            }
            graphql GetPet Query pet(id: String) -> Pet?
            """.trimIndent(),
        )

        assertEquals("query GetPet(\$id: String!) { pet(id: \$id) { id name } }", module.document("GetPet"))
    }

    @Test
    fun selectionCutsCyclesWithTypename() {
        val module = parseModule(
            """
            type Pet {
                id: String,
                friend: Pet?
            }
            graphql GetPet Query pet -> Pet
            """.trimIndent(),
        )

        assertEquals("query GetPet { pet { id friend { __typename } } }", module.document("GetPet"))
    }

    @Test
    fun selectionExpandsUnionsWithInlineFragments() {
        val module = parseModule(
            """
            type Pet {
                id: String
            }
            type Owner {
                name: String
            }
            type Result = Pet | Owner
            graphql Search Query search(text: String) -> Result[]
            """.trimIndent(),
        )

        assertEquals(
            "query Search(\$text: String!) { search(text: \$text) { __typename ... on Pet { id } ... on Owner { name } } }",
            module.document("Search"),
        )
    }

    @Test
    fun nestedNonNullableParametersAreLiftedIntoInput() {
        val module = parseModule(
            """
            type Post {
                id: String
            }
            type User {
                posts(first: Integer32, after: String?): Post[]
            }
            graphql GetUser Query user(id: String) -> User?
            """.trimIndent(),
        )

        assertEquals(
            "query GetUser(\$id: String!, \$postsFirst: Int!) { user(id: \$id) { posts(first: \$postsFirst) { id } } }",
            module.document("GetUser"),
        )
        assertEquals(listOf("id", "postsFirst"), module.inputFields("GetUser"))
    }

    @Test
    fun enumAndScalarLeavesGetNoSelection() {
        val module = parseModule(
            """
            enum Status { OPEN, CLOSED }
            type Ticket {
                id: String,
                status: Status
            }
            graphql GetTicket Query ticket -> Ticket
            """.trimIndent(),
        )

        assertEquals("query GetTicket { ticket { id status } }", module.document("GetTicket"))
    }
}
