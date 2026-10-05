package community.flock.wirespec.converter.graphql

import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.core.parse.ParseOptions
import community.flock.wirespec.compiler.core.parse.ast.Annotation
import community.flock.wirespec.compiler.core.parse.ast.DefaultValue
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Enum
import community.flock.wirespec.compiler.core.parse.ast.Field
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Rpc
import community.flock.wirespec.compiler.core.parse.ast.Spread
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.compiler.core.validate.Validator
import community.flock.wirespec.compiler.utils.NoLogger
import community.flock.wirespec.compiler.utils.noLogger
import community.flock.wirespec.converter.graphql.GraphQLDocumentPrinter.print
import community.flock.wirespec.emitters.wirespec.WirespecEmitter
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlin.test.Test

class GraphQLParserTest {

    @Test
    fun kitchenSinkSurvivesTheRoundTrip() {
        loadResource("kitchen-sink.graphql").shouldRoundTrip()
    }

    @Test
    fun todoSurvivesTheRoundTrip() {
        loadResource("todo.graphql").shouldRoundTrip()
    }

    @Test
    fun federationSurvivesTheRoundTrip() {
        // `_Any` and `_Service` are provided by the federation runtime, so they are declared in another module.
        loadResource("federation.graphql").shouldRoundTrip(otherModules = "type GraphQLAny {}\ntype Service {}")
    }

    @Test
    fun convertsTodoToIdiomaticWirespec() {
        loadResource("todo.graphql").toWirespec() shouldBe """
            |@Description("A task to do")
            |type Todo {
            |  id: ID,
            |  @Description("What needs doing")
            |  title: String,
            |  done: Boolean,
            |  priority: Priority?,
            |  tags: String[]?
            |}
            |
            |enum Priority {
            |  LOW, MEDIUM, HIGH
            |}
            |
            |@GraphQLInput
            |type NewTodo {
            |  title: String,
            |  @GraphQLDefault("MEDIUM")
            |  priority: Priority?
            |}
            |
            |type Query {
            |
            |}
            |
            |@GraphQLField(parent: "Query", name: "todos")
            |rpc QueryTodos {
            |  done: Boolean?
            |} -> Todo[]
            |
            |@GraphQLField(parent: "Query", name: "todo")
            |rpc QueryTodo {
            |  id: ID
            |} -> Todo?
            |
            |type Mutation {
            |
            |}
            |
            |@GraphQLField(parent: "Mutation", name: "addTodo")
            |rpc MutationAddTodo {
            |  input: NewTodo
            |} -> Todo
            |
            |@GraphQLField(parent: "Mutation", name: "completeTodo")
            |rpc MutationCompleteTodo {
            |  id: ID
            |} -> Todo?
            |
            |type Subscription {
            |
            |}
            |
            |@GraphQLField(parent: "Subscription", name: "todoAdded")
            |rpc SubscriptionTodoAdded {} -> Todo
            |
            |@GraphQLBuiltIn
            |type ID = String
            |
        """.trimMargin()
    }

    @Test
    fun spreadsInterfaceFieldsAsParts() {
        """
            interface Node { id: ID! }
            interface Named { name: String }
            type User implements Node & Named { id: ID! name: String email: String }
            type Query { node(id: ID!): Node }
        """.trimIndent().toWirespec() shouldBe """
            |part NodeFields {
            |  id: ID
            |}
            |
            |part NamedFields {
            |  name: String?
            |}
            |
            |@GraphQLInterface("NodeFields")
            |type Node {
            |  ...NodeFields
            |}
            |
            |@GraphQLInterface("NamedFields")
            |type Named {
            |  ...NamedFields
            |}
            |
            |@GraphQLImplements(["Node", "Named"])
            |type User {
            |  ...NodeFields,
            |  ...NamedFields,
            |  email: String?
            |}
            |
            |type Query {
            |
            |}
            |
            |@GraphQLField(parent: "Query", name: "node")
            |rpc QueryNode {
            |  id: ID
            |} -> Node?
            |
            |@GraphQLBuiltIn
            |type ID = String
            |
        """.trimMargin()
    }

    @Test
    fun namesThePartOfAnInterface() {
        val definitions = convert("interface Node { id: ID! } interface Marker extend interface Marker @tag")
        definitions.definition<Type>("Node").annotations.first() shouldBe
            Annotation("GraphQLInterface", listOf(Annotation.Parameter("default", Annotation.Value.Single("NodeFields"))))
        definitions.definition<Type>("Marker").annotations.first() shouldBe Annotation("GraphQLInterface", emptyList())
        definitions.definition<Type>("MarkerExtension").annotations.first() shouldBe Annotation("GraphQLInterface", emptyList())
    }

    @Test
    fun spreadsInterfaceFieldsOnlyWhenTheyAreDeclaredExactly() {
        val source = """
            interface Node { "The id" id: ID! }
            interface Timestamped { createdAt: String!, updatedAt: String }
            type User implements Node & Timestamped { id: ID!, updatedAt: String, createdAt: String! }
            type Post implements Timestamped { title: String, createdAt: String!, updatedAt: String, body: String }
        """.trimIndent()
        val definitions = convert(source)
        definitions.definition<Type>("User").shape.entries.map { (it as? Field)?.identifier?.value ?: it.toString() } shouldContainExactly
            listOf("id", "updatedAt", "createdAt")
        definitions.definition<Type>("Post").shape.entries shouldContainExactly listOf(
            definitions.definition<Type>("Post").shape.value[0],
            Spread(DefinitionIdentifier("TimestampedFields")),
            definitions.definition<Type>("Post").shape.value[3],
        )
        source.shouldRoundTrip()
    }

    @Test
    fun mapsBuiltInScalarsOntoWirespecPrimitives() {
        val user = convert("type User { a: Int!, b: Float!, c: String!, d: Boolean!, e: ID! }").definition<Type>("User")
        user.shape.value.map { it.reference } shouldContainExactly listOf(
            Reference.Primitive(Reference.Primitive.Type.Integer(Reference.Primitive.Type.Precision.P32, null), false),
            Reference.Primitive(Reference.Primitive.Type.Number(Reference.Primitive.Type.Precision.P64, null), false),
            Reference.Primitive(Reference.Primitive.Type.String(null), false),
            Reference.Primitive(Reference.Primitive.Type.Boolean, false),
            Reference.Custom("ID", false),
        )
    }

    @Test
    fun mapsNullabilityAndNestedLists() {
        convert("type T { a: [[Int!]]! }").definition<Type>("T").shape.value.single().reference shouldBe Reference.Iterable(
            reference = Reference.Iterable(
                reference = Reference.Primitive(Reference.Primitive.Type.Integer(Reference.Primitive.Type.Precision.P32, null), false),
                isNullable = true,
            ),
            isNullable = false,
        )
    }

    @Test
    fun convertsCustomScalarsToRefinedTypes() {
        convert("scalar DateTime").definition<Refined>("DateTime").reference shouldBe
            Reference.Primitive(Reference.Primitive.Type.String(null), false)
    }

    @Test
    fun convertsRootFieldsToRpcs() {
        val rpc = convert("type Query { user(id: ID!, active: Boolean = true): User } type User { id: ID! }").definition<Rpc>("QueryUser")
        rpc.shape.value.map { it.identifier.value } shouldContainExactly listOf("id", "active")
        rpc.result shouldBe Reference.Custom("User", true)
        rpc.annotations.first() shouldBe Annotation(
            "GraphQLField",
            listOf(
                Annotation.Parameter("parent", Annotation.Value.Single("Query")),
                Annotation.Parameter("name", Annotation.Value.Single("user")),
            ),
        )
    }

    @Test
    fun usesTheSchemaDefinitionForRootTypes() {
        val definitions = convert("schema { query: Root } type Root { ping: Boolean } type Query { notRoot: Boolean }")
        definitions.definition<Rpc>("RootPing")
        definitions.definition<Type>("Query").shape.value.map { it.identifier.value } shouldContainExactly listOf("notRoot")
    }

    @Test
    fun renamesTypesWirespecCannotSpell() {
        val definitions = convert("type _Service { sdl: String } type lower { a: _Service }")
        definitions.definition<Type>("Service").annotations shouldContainExactly listOf(Annotation("GraphQLName", listOf(Annotation.Parameter("default", Annotation.Value.Single("_Service")))))
        definitions.definition<Type>("Lower").shape.value.single().reference shouldBe Reference.Custom("Service", true)
    }

    @Test
    fun avoidsNameCollisions() {
        convert("type QueryUser { a: Int } type Query { user: QueryUser }").map { it.identifier.value } shouldContainExactly
            listOf("QueryUser", "Query", "QueryUser2")
    }

    @Test
    fun keepsEnumValuesThatAreNotPascalCase() {
        val source = "enum Order { asc, desc, GET, _x }"
        convert(source).definition<Enum>("Order").entries shouldContainExactly listOf("asc", "desc", "GET", "_x")
        source.toWirespec() shouldContain "`asc`, `desc`, `GET`, `_x`"
        source.shouldRoundTrip()
    }

    @Test
    fun escapesDescriptionsAndDefaultValues() {
        val source = "input I { \"a \\\"quoted\\\" \\\\ value\\nwith newline\" s: String = \"x\\\"y\" }"
        source.toWirespec() shouldContain "@Description(\"a \\\"quoted\\\" \\\\ value\\nwith newline\")"
        source.toWirespec() shouldContain "s: String? = \"x\\\"y\""
        source.shouldRoundTrip()
    }

    @Test
    fun usesWirespecDefaultsWhereTheyFit() {
        val source = """
            input I { a: Int = 1, b: Float = 0.5, c: Boolean = true, d: String = "x", e: String = null, f: Int! = -3, g: Float = 2, h: Float = 1e3, i: Role = ADMIN, j: [Int] = [1], k: ID = "x" }
            enum Role { ADMIN }
            type Query { q(a: Int = 1): Int }
        """.trimIndent()
        val fields = convert(source).definition<Type>("I").shape.value
        fields.map { it.defaultValue } shouldContainExactly listOf(
            DefaultValue.IntegerValue("1"),
            DefaultValue.NumberValue("0.5"),
            DefaultValue.BooleanValue(true),
            DefaultValue.StringValue("x"),
            DefaultValue.NullValue,
            DefaultValue.IntegerValue("-3"),
            null,
            null,
            null,
            null,
            null,
        )
        fields.drop(6).map { field -> field.annotations.map { it.name } } shouldContainExactly List(5) { listOf("GraphQLDefault") }
        convert(source).definition<Rpc>("QueryQ").shape.value.single().run {
            defaultValue shouldBe null
            annotations.map { it.name } shouldContainExactly listOf("GraphQLDefault")
        }
        source.shouldRoundTrip()
    }

    @Test
    fun rejectsExecutableDefinitions() {
        shouldThrow<IllegalStateException> { convert("query { me { id } }") }.message shouldContain "only schema definitions"
        shouldThrow<IllegalStateException> { convert("{ me }") }.message shouldContain "only schema definitions"
        shouldThrow<IllegalStateException> { convert("fragment F on User { id }") }.message shouldContain "only schema definitions"
    }

    @Test
    fun rejectsInvalidDocuments() {
        shouldThrow<IllegalStateException> { convert("") }
        shouldThrow<IllegalStateException> { convert("\"described\" extend type User { a: Int }") }
        shouldThrow<IllegalStateException> { convert("type User { a: Int") }
        shouldThrow<IllegalStateException> { convert("type User { a(x: Int = \$v): Int }") }
        shouldThrow<IllegalStateException> { convert("type User { a(x: Int = 01): Int }") }
        shouldThrow<IllegalStateException> { convert("type User { a(x: Int = 1.): Int }") }
        shouldThrow<IllegalStateException> { convert("directive @d on NOWHERE") }
        shouldThrow<IllegalStateException> { convert("enum E { true }") }
        shouldThrow<IllegalStateException> { convert("extend type User") }
        shouldThrow<IllegalStateException> { convert("type User { a: String = \"unterminated }") }
    }

    @Test
    fun readsStringsAsTheSpecificationDescribes() {
        val source = "type T {\n  \"\"\"\n    first\n      indented\n    last \\\"\"\"\n  \"\"\"\n  a: Int\n  \"\\u00e9\\u{1F600}\\uD83D\\uDE00\\/\" b: Int\n}"
        val fields = (GraphQLDocumentParser(source).parseDocument().definitions.single() as GraphQLModel.ObjectTypeDefinition).fields
        fields.map { it.description } shouldContainExactly listOf("first\n  indented\nlast \"\"\"", "é😀😀/")
    }

    @Test
    fun readsNumbersAsWritten() {
        val source = "input I { a: Float = -0, b: Float = 1.5e10, c: Float = 2E-3, d: Int = 42 }"
        val values = (GraphQLDocumentParser(source).parseDocument().definitions.single() as GraphQLModel.InputObjectTypeDefinition).fields.map { it.defaultValue }
        values shouldContainExactly listOf(
            GraphQLModel.Value.IntValue("-0"),
            GraphQLModel.Value.FloatValue("1.5e10"),
            GraphQLModel.Value.FloatValue("2E-3"),
            GraphQLModel.Value.IntValue("42"),
        )
    }

    private fun loadResource(name: String): String = SystemFileSystem.source(Path("src/commonTest/resources/$name")).buffered().readString()

    private fun convert(source: String): List<Definition> = GraphQLParser.parse(ModuleContent(FileUri("schema.graphql"), source), strict = true)
        .modules.flatMap { it.statements }

    private fun String.toWirespec(): String = GraphQLParser.parse(ModuleContent(FileUri("schema.graphql"), this), strict = true)
        .let { WirespecEmitter().emit(it, noLogger).single().result }

    private inline fun <reified T : Definition> List<Definition>.definition(name: String): T = single { it.identifier.value == name }.shouldBeInstanceOf<T>()

    /**
     * GraphQL -> Wirespec AST -> Wirespec source -> Wirespec AST -> GraphQL has to give back the document it started from.
     */
    private fun String.shouldRoundTrip(otherModules: String? = null) {
        val document = GraphQLDocumentParser(this).parseDocument()
        val converted = GraphQLParser.parse(ModuleContent(FileUri("schema.graphql"), this), strict = true)
            .let { Validator.validate(ParseOptions(), it) }
            .shouldBeRight()
        val wirespec = WirespecEmitter().emit(converted, noLogger).single().result
        val parsed = parseWirespec(wirespec, otherModules).shouldBeRight()

        parsed.modules.head.statements.toList() shouldBe converted.modules.head.statements.toList()
        parsed.parts shouldBe converted.parts
        WirespecToGraphQL(parsed.modules.head.statements.toList(), noLogger).convert() shouldBe document
        GraphQLDocumentParser(GraphQLEmitter.emit(parsed.copy(modules = nonEmptyListOf(parsed.modules.head)), noLogger).single().result).parseDocument() shouldBe document
        GraphQLDocumentParser(document.print()).parseDocument() shouldBe document
    }

    private fun parseWirespec(source: String, otherModules: String?) = object : ParseContext, NoLogger {
        override val spec = WirespecSpec
    }.parse(
        nonEmptyListOf(ModuleContent(FileUri("schema.ws"), source)) +
            listOfNotNull(otherModules?.let { ModuleContent(FileUri("other.ws"), it) }),
    )
}
