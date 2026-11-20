package community.flock.wirespec.converter.graphql

import arrow.core.toNonEmptyListOrNull
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.emit.Emitted
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.core.parse.ParseOptions
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Annotation
import community.flock.wirespec.compiler.core.parse.ast.DefaultValue
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.Enum
import community.flock.wirespec.compiler.core.parse.ast.Part
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Rpc
import community.flock.wirespec.compiler.core.parse.ast.Spread
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.compiler.core.parse.ast.fields
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
            |  priority: Priority? = MEDIUM
            |}
            |
            |@GraphQLQuery
            |rpc Todos {
            |  done: Boolean?
            |} -> Todo[]
            |
            |@GraphQLQuery
            |@GraphQLName("todo")
            |rpc QueryTodo {
            |  id: ID
            |} -> Todo?
            |
            |@GraphQLMutation
            |rpc AddTodo {
            |  input: NewTodo
            |} -> Todo
            |
            |@GraphQLMutation
            |rpc CompleteTodo {
            |  id: ID
            |} -> Todo?
            |
            |@GraphQLSubscription
            |rpc TodoAdded {} -> Todo
            |
            |@GraphQLBuiltIn
            |type ID = String
            |
        """.trimMargin()
    }

    @Test
    fun convertsAnInterfaceToTheUnionOfItsImplementationsWhichSpreadItsFields() {
        val source = """
            interface Node { id: ID! }
            type User implements Node { id: ID! email: String }
            type Post implements Node { id: ID! title: String! }
            type Query { node(id: ID!): Node }
        """.trimIndent()
        source.toWirespec() shouldBe """
            |part NodeInterface {
            |  id: ID
            |}
            |
            |@GraphQLInterface("NodeInterface")
            |type Node = User | Post
            |
            |type User {
            |  ...NodeInterface,
            |  email: String?
            |}
            |
            |type Post {
            |  ...NodeInterface,
            |  title: String
            |}
            |
            |@GraphQLQuery
            |@GraphQLName("node")
            |rpc QueryNode {
            |  id: ID
            |} -> Node?
            |
            |@GraphQLBuiltIn
            |type ID = String
            |
        """.trimMargin()
        source.shouldRoundTrip()
    }

    @Test
    fun anImplementationTakesTheFieldsOfItsInterface() {
        val source = """
            interface Node { "The globally unique id" id: ID! }
            type Post implements Node { title: String! id: ID! @deprecated }
        """.trimIndent()
        source.toWirespec() shouldContain """
            |type Post {
            |  title: String,
            |  ...NodeInterface
            |}
        """.trimMargin()
        source.convertedBack() shouldBe GraphQLDocumentParser(
            """
            interface Node { "The globally unique id" id: ID! }
            type Post implements Node { title: String! "The globally unique id" id: ID! }
            """.trimIndent(),
        ).parseDocument()
    }

    @Test
    fun anInterfaceThatNothingImplementsIsATypeWithItsFields() {
        val source = """
            interface Searchable { title: String! }
            type Query { search: [Searchable!]! }
        """.trimIndent()
        source.toWirespec() shouldContain "@GraphQLInterface\ntype Searchable {\n  title: String\n}"
        convert(source).filterIsInstance<Part>() shouldBe emptyList()
        source.shouldRoundTrip()
    }

    @Test
    fun anInterfaceThatOnlyInterfacesImplementIsAnEmptyType() {
        val source = "interface Node { id: ID! } interface Resource implements Node { id: ID! url: String! }"
        source.toWirespec() shouldContain "@GraphQLInterface(\"NodeInterface\")\ntype Node {\n\n}"
        source.shouldRoundTrip()
    }

    @Test
    fun anInterfaceSpreadsTheInterfacesItImplements() {
        val source = """
            interface Node { id: ID! }
            interface Resource implements Node { id: ID! url: String! }
            type Image implements Resource & Node { id: ID! url: String! width: Int }
        """.trimIndent()
        source.toWirespec() shouldContain "part ResourceInterface {\n  ...NodeInterface,\n  url: String\n}"
        source.toWirespec() shouldContain "type Image {\n  ...ResourceInterface,\n  width: Integer32?\n}"
        source.shouldRoundTrip()
    }

    @Test
    fun listsTheInterfacesThatCannotBeSpread() {
        // Named brings an id that Node already spreads, and Remote is declared elsewhere.
        val source = """
            interface Node { id: ID! }
            interface Named { id: ID! name: String! }
            type User implements Node & Named & Remote { id: ID! name: String! }
        """.trimIndent()
        convert(source).definition<Type>("User").run {
            annotations shouldContainExactly listOf(
                Annotation("GraphQLImplements", listOf(Annotation.Parameter("default", Annotation.Value.Array(listOf(Annotation.Value.Single("Named"), Annotation.Value.Single("Remote")))))),
            )
            shape.value.first() shouldBe Spread(Reference.Custom("NodeInterface", false))
        }
        source.shouldRoundTrip()
    }

    @Test
    fun anExtensionLeavesTheInterfacesItCannotSpreadToItsType() {
        convert("interface Node { id: ID! } type User { id: ID! email: String } extend type User implements Node")
            .map { it.identifier.value } shouldContainExactly listOf("NodeInterface", "Node", "User", "ID")
        "interface Node { id: ID! } type User { id: ID! email: String } extend type User implements Node".convertedBack() shouldBe
            GraphQLDocumentParser("interface Node { id: ID! } type User implements Node { id: ID! email: String }").parseDocument()
        "interface Node { id: ID! } type User implements Node { id: ID! } extend type User implements Node @key".convertedBack() shouldBe
            GraphQLDocumentParser("interface Node { id: ID! } type User implements Node { id: ID! } extend type User @key").parseDocument()
    }

    @Test
    fun aRootOperationTypeListsItsInterfaces() {
        val source = "interface Node { id: ID! } type Query implements Node { id: ID! }"
        convert(source).definition<Type>("Query").annotations.map { it.name } shouldContainExactly listOf("GraphQLImplements")
        source.shouldRoundTrip()
    }

    @Test
    fun interfacesThatImplementEachOtherAreNotSpreadIntoEachOther() {
        val source = "interface A implements B { a: Int } interface B implements A { a: Int }"
        convert(source).filterIsInstance<Part>().flatMap { it.shape.value }.filterIsInstance<Spread>() shouldBe emptyList()
        source.shouldRoundTrip()
    }

    @Test
    fun mapsBuiltInScalarsOntoWirespecPrimitives() {
        val user = convert("type User { a: Int!, b: Float!, c: String!, d: Boolean!, e: ID! }").definition<Type>("User")
        user.shape.fields.map { it.reference } shouldContainExactly listOf(
            Reference.Primitive(Reference.Primitive.Type.Integer(Reference.Primitive.Type.Precision.P32, null), false),
            Reference.Primitive(Reference.Primitive.Type.Number(Reference.Primitive.Type.Precision.P64, null), false),
            Reference.Primitive(Reference.Primitive.Type.String(null), false),
            Reference.Primitive(Reference.Primitive.Type.Boolean, false),
            Reference.Custom("ID", false),
        )
    }

    @Test
    fun mapsNullabilityAndNestedLists() {
        convert("type T { a: [[Int!]]! }").definition<Type>("T").shape.fields.single().reference shouldBe Reference.Iterable(
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
        val rpc = convert("type Query { findUser(id: ID!, active: Boolean = true): User } type User { id: ID! }").definition<Rpc>("FindUser")
        rpc.shape.fields.map { it.identifier.value } shouldContainExactly listOf("id", "active")
        rpc.result shouldBe Reference.Custom("User", true)
        rpc.annotations.first() shouldBe Annotation("GraphQLQuery", emptyList())
    }

    @Test
    fun prefixesAnRpcWithItsRootTypeOnlyWhenItsNameIsTaken() {
        val definitions = convert("type Query { user: User, users: [User] } type Mutation { user: User } type User { id: ID! }")
        definitions.filterIsInstance<Rpc>().map { rpc -> rpc.identifier.value to rpc.annotations.map { it.name } } shouldContainExactly listOf(
            "QueryUser" to listOf("GraphQLQuery", "GraphQLName"),
            "Users" to listOf("GraphQLQuery"),
            "MutationUser" to listOf("GraphQLMutation", "GraphQLName"),
        )
    }

    @Test
    fun convertsAFieldWithArgumentsToAnRpcThatNamesItsType() {
        val source = """
            |interface Node {
            |  id: ID!
            |  related(first: Int): [Node!]
            |}
            |
            |type Todo implements Node {
            |  id: ID!
            |  title: String!
            |  related(first: Int): [Node!]
            |  comments("How many to return" first: Int = 10): [String!]! @deprecated
            |  history(since: String!): [String!]
            |}
            |
            |extend type Todo {
            |  tags(prefix: String): [String!]
            |}
            |
            |type Query {
            |  todo(id: ID!): Todo
            |}
        """.trimMargin()

        source.toWirespec() shouldBe """
            |part NodeInterface {
            |  id: ID
            |}
            |
            |@GraphQLInterface("NodeInterface")
            |type Node = Todo
            |
            |@GraphQLQuery("Node")
            |rpc NodeRelated {
            |  first: Integer32?
            |} -> Node[]?
            |
            |type Todo {
            |  ...NodeInterface,
            |  title: String
            |}
            |
            |@GraphQLQuery("Todo")
            |rpc TodoRelated {
            |  first: Integer32?
            |} -> Node[]?
            |
            |@GraphQLQuery("Todo")
            |@GraphQLDirective("@deprecated")
            |rpc TodoComments {
            |  @Description("How many to return")
            |  first: Integer32? = 10
            |} -> String[]
            |
            |@GraphQLQuery("Todo")
            |rpc TodoHistory {
            |  since: String
            |} -> String[]?
            |
            |@GraphQLExtend("Todo")
            |type TodoExtension {
            |
            |}
            |
            |@GraphQLQuery("TodoExtension")
            |rpc TodoTags {
            |  prefix: String?
            |} -> String[]?
            |
            |@GraphQLQuery
            |@GraphQLName("todo")
            |rpc QueryTodo {
            |  id: ID
            |} -> Todo?
            |
            |@GraphQLBuiltIn
            |type ID = String
            |
        """.trimMargin()
        source.shouldRoundTrip()
    }

    @Test
    fun namesAnRpcOfATypeAfterTheTypeAndTheField() {
        val rpcs = convert("type User { name(short: Boolean): String, userName(short: Boolean): String, _id(raw: Boolean): ID } type UserName { a: Int } type Query { user: User, users: [User] }")
            .filterIsInstance<Rpc>()
        rpcs.map { rpc -> rpc.identifier.value to rpc.annotations.map { it.name } } shouldContainExactly listOf(
            "UserName2" to listOf("GraphQLQuery", "GraphQLName"),
            "UserUserName" to listOf("GraphQLQuery"),
            "UserId" to listOf("GraphQLQuery", "GraphQLName"),
            "QueryUser" to listOf("GraphQLQuery", "GraphQLName"),
            "Users" to listOf("GraphQLQuery"),
        )
    }

    @Test
    fun convertsATypeNestedInARootTypeToRpcsThatNameIt() {
        val source = """
            |type Query {
            |  ping: Boolean
            |  "Everything about notifications"
            |  notifications: NotificationsQuery! @deprecated
            |  todos: [Todo!]!
            |  type: TypeQuery
            |}
            |
            |type Mutation {
            |  notifications: NotificationsMutation!
            |}
            |
            |type Todo {
            |  id: ID!
            |}
            |
            |"Queries about notifications"
            |type NotificationsQuery {
            |  archive: ArchiveQuery
            |  inbox(limit: Int! = 10): [Todo!]!
            |  inboxCount: Int!
            |}
            |
            |type ArchiveQuery {
            |  search(text: String!): [Todo!]!
            |}
            |
            |type NotificationsMutation {
            |  markRead(id: ID!): Boolean
            |}
            |
            |type TypeQuery {
            |  todo(id: ID!): Todo
            |}
        """.trimMargin()

        source.toWirespecFiles() shouldBe mapOf(
            "schema.ws" to """
                |@file:GraphQLQuery(path: { notifications: { type: "NotificationsQuery!", description: "Everything about notifications", directives: ["@deprecated"] } })
                |@file:GraphQLQuery(path: { notifications: { type: "NotificationsQuery!", description: "Everything about notifications", directives: ["@deprecated"] }, archive: "ArchiveQuery" })
                |@file:GraphQLMutation(path: { notifications: "NotificationsMutation!" })
                |@file:GraphQLQuery(path: { type: "TypeQuery" })
                |
                |@GraphQLQuery
                |rpc Ping {} -> Boolean?
                |
                |@GraphQLQuery
                |rpc Todos {} -> Todo[]
                |
                |type Todo {
                |  id: ID
                |}
                |
                |@Description("Queries about notifications")
                |type NotificationsQuery {
                |
                |}
                |
                |@GraphQLQuery("NotificationsQuery")
                |rpc Inbox {
                |  limit: Integer32 = 10
                |} -> Todo[]
                |
                |@GraphQLQuery("NotificationsQuery")
                |rpc InboxCount {} -> Integer32
                |
                |@GraphQLQuery("ArchiveQuery")
                |rpc Search {
                |  text: String
                |} -> Todo[]
                |
                |@GraphQLMutation("NotificationsMutation")
                |rpc MarkRead {
                |  id: ID
                |} -> Boolean?
                |
                |@GraphQLQuery("TypeQuery")
                |@GraphQLName("todo")
                |rpc TypeQueryTodo {
                |  id: ID
                |} -> Todo?
                |
                |@GraphQLBuiltIn
                |type ID = String
                |
            """.trimMargin(),
        )
        // A type without a Wirespec definition comes back at the first rpc that adds a field to it, and a field that nests a type after the others.
        source.convertedBack().unordered() shouldBe GraphQLDocumentParser(source).parseDocument().unordered()
    }

    @Test
    fun nestedTypesOfARootTypeAndItsExtensionSurviveTheRoundTrip() {
        """
            |schema {
            |  mutation: Commands
            |}
            |
            |type Commands {
            |  ping: Boolean
            |  todos: TodoMutations
            |}
            |
            |type TodoMutations {
            |  add(title: String!): ID!
            |  clear: Boolean
            |}
            |
            |extend type Commands {
            |  session: SessionMutations!
            |}
            |
            |type SessionMutations {
            |  login(name: String!): Boolean!
            |}
        """.trimMargin().shouldRoundTrip()
    }

    @Test
    fun nestsOnlyATypeWithOperationsThatOneFieldLeadsTo() {
        val nested = { source: String ->
            GraphQLParser.parse(ModuleContent(FileUri("schema.graphql"), source), strict = true).modules.head
                .takeIf { it.annotations.isNotEmpty() }
                ?.statements.orEmpty()
                .filterIsInstance<Rpc>()
                .filter { it.annotations.first().parameters.isNotEmpty() }
                .map { it.identifier.value }
        }
        nested("type Query { a: A! } type A { f(x: Int): Int }") shouldContainExactly listOf("F")
        nested("type Query { a: A } type A { b: B! } type B { f(x: Int): Int, g: Int }") shouldContainExactly listOf("F", "G")
        // Only holds data
        nested("type Query { a: A! } type A { f: Int }") shouldBe emptyList()
        // Two fields lead to it
        nested("type Query { a: A!, b: A } type A { f(x: Int): Int }") shouldBe emptyList()
        nested("type Query { a: A! } type A { f(x: Int): Int, self: A }") shouldBe emptyList()
        nested("type Query { a: A! } type A { f(x: Int): Int } union U = A") shouldBe emptyList()
        // The field that leads to it is a function, or returns a list
        nested("type Query { a(id: ID!): A! } type A { f(x: Int): Int }") shouldBe emptyList()
        nested("type Query { a: [A!]! } type A { f(x: Int): Int }") shouldBe emptyList()
        // It is more than its fields
        nested("type Query { a: A! } type A implements I { f(x: Int): Int } interface I { f(x: Int): Int }") shouldBe emptyList()
        nested("type Query { a: A! } type A { f(x: Int): Int } extend type A { g: Int }") shouldBe emptyList()
        // No root type leads to it
        nested("type Query { u: [User] } type User { a: A! } type A { f(x: Int): Int }") shouldBe emptyList()
        listOf(
            "type Query { a: A!, b: A } type A { f(x: Int): Int }",
            "type Query { a: A! } type A { self: A, f(x: Int): Int }",
            "type Query { a(id: ID!): A! } type A { f(x: Int): Int }",
            "type Query { a: A! } type A { f(x: Int): Int } extend type A { g: Int }",
            "type Query { u: [User] } type User { a: A! } type A { f(x: Int): Int }",
        ).forEach { it.shouldRoundTrip() }
    }

    @Test
    fun everyWayToGroupOperationsInATypeComesBackAsItWas() {
        val todo = "type T { id: ID! }"
        // The schema, and the rpcs that name a nested type.
        mapOf(
            "type Query { ns: NS! } type NS { f(x: Int): T } $todo" to listOf("F"),
            "type Query { ns: NS } type NS { f(x: Int): T } $todo" to listOf("F"),
            "type Query { a: A! } type A { b: B! g(x: Int): T } type B { f(x: Int): T } $todo" to listOf("G", "F"),
            "type Query { a: A! } type A { b: B! } type B { f(x: Int): T } $todo" to listOf("F"),
            "type Mutation { a: A! } type A { b: B } type B { c: C! } type C { f(x: Int): T } $todo" to listOf("F"),
            "type Query { a: A! ping: Boolean b: B } type A { f(x: Int): T } type B { g(x: Int): T } $todo" to listOf("F", "G"),
            "type Query { ns: NSQ! } type Mutation { ns: NSM! } type NSQ { f(x: Int): T } type NSM { g(x: Int): T } $todo" to listOf("F", "G"),
            "type Query { ping: Boolean } extend type Query { ns: NS! } type NS { f(x: Int): T } $todo" to listOf("F"),
            "schema { query: Q mutation: M } type Q { ns: NS! } type M { ns: NM } type NS { f(x: Int): T } type NM { g(x: Int): T } $todo" to listOf("F", "G"),
            "type Query { \"where\" ns: NS! @deprecated } \"what\" type NS @tag { \"how\" f(x: Int): T } directive @tag on OBJECT $todo" to listOf("F"),
            "type Query { ns: NS! @shareable } type NS @key(fields: \"id\") { id: ID! f(x: Int): T } directive @shareable on FIELD_DEFINITION directive @key(fields: String!) on OBJECT $todo" to listOf("Id", "F"),
            "type Query { viewer: V } type V { name: String friends(first: Int): [T!] } $todo" to listOf("Name", "Friends"),
            "type Subscription { ns: NS! } type NS { f(x: Int): T } $todo" to listOf("F"),
            "type Query { ns: NS! ping: Boolean } type NS { f(x: Int): T query: Query! } $todo" to listOf("F", "NSQuery"),
            "type Query { inbox: Inbox! } type Inbox { inbox(x: Int): T list: [T!] } $todo" to listOf("InboxInbox", "List"),
            // Not nested: more than one field leads to the type, or the type is more than its fields.
            "type Query { ns: NS! } type Mutation { ns: NS! } type NS { f(x: Int): T } $todo" to emptyList(),
            "type Query { a: NS! b: NS! } type NS { f(x: Int): T } $todo" to emptyList(),
            "type Query { ns: NS! any: U } type NS { f(x: Int): T } union U = NS | T $todo" to emptyList(),
            "type Query { ns: NS! } type NS { f(x: Int): T } extend type NS { g(x: Int): T } $todo" to emptyList(),
            "interface Node { id: ID! } type Query { ns: NS! } type NS implements Node { id: ID! f(x: Int): T } $todo" to emptyList(),
            // Not nested: the field that leads to the type is a function, or returns a list.
            "type Query { project(id: ID!): P! } type P { tasks(done: Boolean): [T!]! } $todo" to emptyList(),
            "type Query { ns: [NS!]! } type NS { f(x: Int): T } $todo" to emptyList(),
            // Not nested: nothing tells a type without a field with arguments from data.
            "type Mutation { session: S! } type S { logout: Boolean refresh: T } $todo" to emptyList(),
            // No type at all: the operations are grouped by their names.
            "type Query { notifications_inbox(limit: Int): [T!] notificationsCount: Int } $todo" to emptyList(),
        ).forEach { (source, nested) ->
            val module = GraphQLParser.parse(ModuleContent(FileUri("schema.graphql"), source), strict = true).modules.single()
            val namespaces = module.annotations.map { it.namespace() }
            module.statements.filterIsInstance<Rpc>()
                .filter { rpc -> rpc.annotations.first().parameters.any { it.name == "default" && (it.value as Annotation.Value.Single).value in namespaces } }
                .map { it.identifier.value } shouldContainExactly nested
            source.convertedBack().unordered() shouldBe GraphQLDocumentParser(source).parseDocument().unordered()
        }
    }

    @Test
    fun keepsARootTypeOnlyWhenItNeedsOne() {
        val types = { source: String -> convert(source).filterIsInstance<Type>().map { it.identifier.value } }
        types("type Query { a: Int }") shouldBe emptyList()
        types("\"The entry point\" type Query { a: Int }") shouldContainExactly listOf("Query")
        types("type Query @cached { a: Int }") shouldContainExactly listOf("Query")
        types("type Query { a: Int } type User { viewer: Query }") shouldContainExactly listOf("Query", "User")
        types("type Query") shouldContainExactly listOf("Query")
        types("schema { query: Root } type Root { a: Int }") shouldBe emptyList()
    }

    @Test
    fun keepsTheSchemaAndDirectiveDefinitionsInSpecAnnotations() {
        val source = """
            |"The entry point"
            |schema @link(url: "https://example.com") {
            |  query: Root
            |}
            |
            |extend schema @audit
            |
            |"Marks a type as an entity"
            |directive @key(fields: String!, "Resolved here" resolvable: Boolean = true @deprecated) repeatable on OBJECT | INTERFACE
            |
            |directive @audit on SCHEMA
            |
            |type Root {
            |  ping: Boolean
            |}
        """.trimMargin()
        source.toWirespec() shouldBe """
            |@spec:GraphQLSchema(query: "Root", description: "The entry point", directives: ["@link(url: \"https://example.com\")"])
            |@spec:GraphQLSchema(extend: "true", directives: ["@audit"])
            |@spec:GraphQLDirectiveDefinition(name: "key", locations: ["OBJECT", "INTERFACE"], repeatable: "true", description: "Marks a type as an entity", arguments: { fields: { type: "String!" }, resolvable: { type: "Boolean", defaultValue: "true", description: "Resolved here", directives: ["@deprecated"] } })
            |@spec:GraphQLDirectiveDefinition(name: "audit", locations: ["SCHEMA"])
            |
            |@GraphQLQuery("Root")
            |rpc Ping {} -> Boolean?
            |
        """.trimMargin()
        source.shouldRoundTrip()
    }

    @Test
    fun writesTheSchemaAndDirectiveDefinitionsFirst() {
        "type Query { a: Int } directive @cached on FIELD_DEFINITION extend schema @audit".convertedBack() shouldBe
            GraphQLDocumentParser("directive @cached on FIELD_DEFINITION extend schema @audit type Query { a: Int }").parseDocument()
    }

    @Test
    fun rejectsADocumentWithoutTypes() {
        shouldThrow<IllegalStateException> { convert("schema { query: Query } directive @cached on FIELD_DEFINITION") }.message shouldBe
            "Cannot convert GraphQL document schema.graphql: it defines no types, and Wirespec needs a definition besides the @spec: annotations of its schema and directives"
    }

    @Test
    fun keepsExtensionsOfRootTypesApart() {
        val source = "type Query { a: Int } extend type Query { b: Int } extend type Query { c: Int }"
        convert(source).filterIsInstance<Rpc>().map { rpc -> rpc.annotations.first().parameters.map { it.name to (it.value as Annotation.Value.Single).value } } shouldContainExactly listOf(
            emptyList(),
            listOf("extend" to "1"),
            listOf("extend" to "2"),
        )
        source.shouldRoundTrip()
    }

    @Test
    fun usesTheSchemaDefinitionForRootTypes() {
        val definitions = convert("schema { query: Root } type Root { ping: Boolean } type Query { notRoot: Boolean }")
        definitions.definition<Rpc>("Ping").annotations.first() shouldBe
            Annotation("GraphQLQuery", listOf(Annotation.Parameter("default", Annotation.Value.Single("Root"))))
        definitions.definition<Type>("Query").shape.fields.map { it.identifier.value } shouldContainExactly listOf("notRoot")
    }

    @Test
    fun renamesTypesWirespecCannotSpell() {
        val definitions = convert("type _Service { sdl: String } type lower { a: _Service }")
        definitions.definition<Type>("Service").annotations shouldContainExactly listOf(Annotation("GraphQLName", listOf(Annotation.Parameter("default", Annotation.Value.Single("_Service")))))
        definitions.definition<Type>("Lower").shape.fields.single().reference shouldBe Reference.Custom("Service", true)
    }

    @Test
    fun avoidsNameCollisions() {
        convert("type User { a: Int } type QueryUser { b: Int } type Query { user: User }").map { it.identifier.value } shouldContainExactly
            listOf("User", "QueryUser", "QueryUser2")
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
    fun usesEnumDefaultsOnlyForEntriesOfADeclaredEnum() {
        val source = """
            input I { a: Order = asc, b: Order = DESC, c: Color = RED, d: Remote = X, e: [Order] = [asc] }
            enum Order { asc }
            extend enum Order { DESC }
            scalar Color
        """.trimIndent()
        convert(source).definition<Type>("I").shape.fields.map { it.defaultValue } shouldContainExactly
            listOf(DefaultValue.EnumValue("asc"), null, null, null, null)
        source.toWirespec() shouldContain "a: Order? = `asc`"
        source.shouldRoundTrip(otherModules = "type Remote {}")
    }

    @Test
    fun usesWirespecDefaultsWhereTheyFit() {
        val source = """
            input I { a: Int = 1, b: Float = 0.5, c: Boolean = true, d: String = "x", e: String = null, f: Int! = -3, g: Float = 2, h: Float = 1e3, i: Role = ADMIN, j: [Int] = [1], k: ID = "x" }
            enum Role { ADMIN }
            type Query { q(a: Int = 1, b: [Int] = [1]): Int }
        """.trimIndent()
        val fields = convert(source).definition<Type>("I").shape.fields
        fields.map { it.defaultValue } shouldContainExactly listOf(
            DefaultValue.IntegerValue("1"),
            DefaultValue.NumberValue("0.5"),
            DefaultValue.BooleanValue(true),
            DefaultValue.StringValue("x"),
            DefaultValue.NullValue,
            DefaultValue.IntegerValue("-3"),
            null,
            null,
            DefaultValue.EnumValue("ADMIN"),
            null,
            null,
        )
        fields.drop(6).map { field -> field.annotations.map { it.name } } shouldContainExactly listOf(
            listOf("GraphQLDefault"),
            listOf("GraphQLDefault"),
            emptyList(),
            listOf("GraphQLDefault"),
            listOf("GraphQLDefault"),
        )
        convert(source).definition<Rpc>("Q").shape.fields.map { it.defaultValue to it.annotations.map { annotation -> annotation.name } } shouldContainExactly listOf(
            DefaultValue.IntegerValue("1") to emptyList(),
            null to listOf("GraphQLDefault"),
        )
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

    /** The type that the path of a `@file:` annotation leads to: the type of its last field, whatever its nullability. */
    private fun Annotation.namespace(): String = (parameters.single { it.name == "path" }.value as Annotation.Value.Dict).value.last().value
        .let { field -> (field as? Annotation.Value.Dict)?.value?.single { it.name == "type" }?.value ?: field }
        .let { (it as Annotation.Value.Single).value.trimEnd('!') }

    /** The definitions in any order, with the fields of each type in any order: what a schema with functions among its other fields comes back as. */
    private fun GraphQLModel.Document.unordered(): Set<GraphQLModel.Definition> = definitions
        .map { definition ->
            when (definition) {
                is GraphQLModel.ObjectTypeDefinition -> definition.copy(fields = definition.fields.sortedBy { it.name })
                is GraphQLModel.InterfaceTypeDefinition -> definition.copy(fields = definition.fields.sortedBy { it.name })
                else -> definition
            }
        }
        .toSet()

    private fun String.toWirespec(): String = toWirespecFiles().values.single()

    /** The Wirespec files a schema converts to, by name. */
    private fun String.toWirespecFiles(): Map<String, String> = GraphQLParser.parse(ModuleContent(FileUri("schema.graphql"), this), strict = true)
        .let { WirespecEmitter().emit(it, noLogger) }
        .associate { it.file to it.result }

    private inline fun <reified T : Definition> List<Definition>.definition(name: String): T = single { it.identifier.value == name }.shouldBeInstanceOf<T>()

    /**
     * GraphQL -> Wirespec AST -> Wirespec source -> Wirespec AST -> GraphQL has to give back the document it started from.
     */
    private fun String.shouldRoundTrip(otherModules: String? = null) {
        val document = GraphQLDocumentParser(this).parseDocument()
        val converted = GraphQLParser.parse(ModuleContent(FileUri("schema.graphql"), this), strict = true)
            .let { Validator.validate(ParseOptions(), it) }
            .shouldBeRight()
        val parsed = parseWirespec(WirespecEmitter().emit(converted, noLogger), otherModules).shouldBeRight()

        parsed.withoutOtherModules().modules.map { it.statements.toList() to it.annotations } shouldBe converted.modules.map { it.statements.toList() to it.annotations }
        parsed.annotations.map { it.annotation } shouldBe converted.annotations.map { it.annotation }
        GraphQLEmitter.validate(parsed) shouldBe emptyList()
        WirespecToGraphQL(parsed.withoutOtherModules()).convert() shouldBe document
        GraphQLDocumentParser(document.print()).parseDocument() shouldBe document
    }

    /** GraphQL -> Wirespec source -> GraphQL, for a schema that does not come back as it was written. */
    private fun String.convertedBack(): GraphQLModel.Document = GraphQLParser.parse(ModuleContent(FileUri("schema.graphql"), this), strict = true)
        .let { Validator.validate(ParseOptions(), it) }
        .shouldBeRight()
        .let { WirespecEmitter().emit(it, noLogger) }
        .let { parseWirespec(it, null).shouldBeRight() }
        .let { WirespecToGraphQL(it).convert() }

    /** The converted schema, without the module that only declares what it refers to. */
    private fun AST.withoutOtherModules(): AST = copy(modules = modules.filterNot { it.fileUri.value == OTHER_MODULES }.toNonEmptyListOrNull()!!)

    private fun parseWirespec(files: List<Emitted>, otherModules: String?) = object : ParseContext, NoLogger {
        override val spec = WirespecSpec
    }.parse(
        (files.map { ModuleContent(FileUri(it.file), it.result) } + listOfNotNull(otherModules?.let { ModuleContent(FileUri(OTHER_MODULES), it) })).toNonEmptyListOrNull()!!,
    )

    private companion object {
        const val OTHER_MODULES = "other.ws"
    }
}
