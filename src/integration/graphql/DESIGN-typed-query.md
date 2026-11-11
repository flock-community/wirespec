# Design: a typed GraphQL query on the request

Status: proposal

## Goal

Today the request of an rpc carries its query as text:

```kotlin
public data class GraphQLRequest(override val query: String, val variables: GraphQLVariables) : Wirespec.GraphQLRequest
```

The selection DSL writes into a `StringBuilder` and the result is gone the moment it is printed. Nothing can read a
query back, so the extension can only generate clients.

This proposal makes the query a **typed value**: the arguments of the rpc, and a tree of the fields it selects, all the
way down. The client builds that value and prints it. A server parses the incoming text into the same value and hands
it to a handler, a function from request to response, the same way the `Handler` of an endpoint gets a typed `Request`.

```
            client                                         server
  request(id) { title() }                       POST /graphql {"query": "...", "variables": {...}}
          │                                                  │
          ▼                                                  ▼
  QueryTodoGraphQL.Request  ──── toGraphQL() ───▶   Wirespec.GraphQL.parse(text)       (untyped AST, shared file)
  (alias, arguments, selection)                              │
          ▲                                                  ▼
          └──────────── the same value ◀──────── QueryTodoGraphQL.Request.parse(...)   (typed binding, generated)
                                                             │
                                                             ▼
                                                  Handler: Request -> SelectedResponse
                                                             │
                                                             ▼
                                                  TodoSelection.write(result)  →  {"data": {...}, "errors": [...]}
```

The generated code stays dependency-free: the parser of executable documents is emitted into the shared `Wirespec`
file, like the four `GraphQL*` interfaces the extension already adds there.

## The analogy with endpoints

| Endpoint                                     | GraphQL rpc (this proposal)                                     |
|----------------------------------------------|-----------------------------------------------------------------|
| `PutTodo.Request` with `path`, `queries`, `headers`, `body` | `QueryTodoGraphQL.Request` with `alias`, `arguments`, `selection` |
| `PutTodo.Response<*>`                        | `QueryTodoGraphQL.SelectedResponse` (`data`, `errors`)          |
| `PutTodo.Handler`: `Request -> Response`     | `QueryTodoGraphQL.Handler`: `Request -> SelectedResponse`        |
| `fromRawRequest` / `toRawResponse`           | `GraphQLServer.handle(RawRequest): RawResponse`                 |
| `Wirespec.ServerEdge` / `Wirespec.Server`    | `Wirespec.GraphQLServerEdge` / `Wirespec.GraphQLServer`         |

One difference: an HTTP request hits one endpoint, but one GraphQL document can call several rpcs at once
(`{ a: todo(id: 1) { title } todos { id } }`). So the server parses a document into a list of rpc requests, dispatches
each one to its handler, and assembles one response.

## Class structure

All sketches are the Kotlin output for `src/jvmCodegen/resources/wirespec/todo.ws`. Java and Scala get the same shape
with their own idiom (see [Language shapes](#language-shapes)).

### 1. The selection of a type: an immutable tree

Every type a result reaches gets a `<Type>Selection`, now a value instead of a builder:

```kotlin
public data class TodoSelection(val fields: List<Field>) {

    /** One selected field, under its response key. */
    public sealed interface Field {
        val alias: String?
        public data class Typename(override val alias: String? = null) : Field
        public data class Id(override val alias: String? = null) : Field
        public data class Title(override val alias: String? = null) : Field
        public data class Done(override val alias: String? = null) : Field
        public data class Priority(override val alias: String? = null) : Field
        public data class Owner(override val alias: String? = null, val selection: UserSelection) : Field
    }

    // Look-ahead: what a handler asks before it fetches anything.
    val id: Boolean get() = fields.any { it is Field.Id }
    val title: Boolean get() = fields.any { it is Field.Title }
    val done: Boolean get() = fields.any { it is Field.Done }
    val priority: Boolean get() = fields.any { it is Field.Priority }
    val owner: UserSelection? get() = fields.filterIsInstance<Field.Owner>().map { it.selection }.reduceOrNull(UserSelection::plus)

    public operator fun plus(other: TodoSelection): TodoSelection
    public fun toGraphQL(): String                                  // "{ id title owner { name } }"
    public fun project(model: Todo): TodoResult                     // keep only what is selected
    public fun write(result: TodoResult?, path: List<Any>, nullable: Boolean): Wirespec.GraphQL.Written

    public class Builder {                                          // the DSL the client already uses
        public fun id(): Builder
        public fun title(): Builder
        public fun owner(selection: UserSelection.Builder.() -> Unit): Builder
        public fun build(): TodoSelection
    }

    public companion object {
        public val ALL: TodoSelection                               // every selectable field, all the way down
        public fun parse(selections: List<Wirespec.GraphQL.Selection>, context: Wirespec.GraphQL.Context): TodoSelection
    }
}
```

Why a list of sealed `Field`s and not a data class of booleans (`TodoSelection(id: Boolean, owner: UserSelection?)`):

- **Aliases.** A response is keyed by response key, not field name: `{ a: title b: title }` is a legal query. Each
  `Field` keeps its alias, so the server writes the right keys and the client prints them back.
- **Order.** GraphQL returns the fields in the order they were selected. A list keeps it.
- **Round trip.** `parse(print(selection)) == selection` holds, which is the main test of the design.
- **Exhaustiveness.** A handler or writer that `when`s over `Field` is checked by the compiler when the schema grows.

The boolean and nested accessors give the ergonomic view on top: `request.selection.owner?.let { ... }` is how a
handler decides whether to load the owner at all.

The `Field` subtypes are nested in `Field`, not directly in the selection, so a field named like a model
(`priority: Priority`) does not shadow the model type in the file.

A **union** selects `__typename` and one inline fragment per member; it cannot select fields directly:

```kotlin
public data class SearchResultSelection(val fields: List<Field>) {
    public sealed interface Field {
        val alias: String?
        public data class Typename(override val alias: String? = null) : Field
        public data class OnNote(val selection: NoteSelection) : Field { override val alias: String? = null }
        public data class OnTag(val selection: TagSelection) : Field { override val alias: String? = null }
    }
    val onNote: NoteSelection? get() = ...
    val onTag: TagSelection? get() = ...
}
```

Normalisation happens at parse time, so the typed tree is always *executable* and never needs the document again:

- Named fragments (`...TodoFields`) and inline fragments on the type itself are inlined.
- `@skip(if: $x)` and `@include(if: $x)` are evaluated against the variables and dropped.
- Fields with the same response key are merged into one `Field`, their sub-selections combined with `plus`; two fields
  with the same response key but a different field name are a validation error (spec: *Field Selection Merging*).

### 2. The request of an rpc

`GraphQLRequest` and `GraphQLVariables` are replaced by:

```kotlin
object QueryTodoGraphQL {

    public data class Request(
        override val alias: String = "result",
        val arguments: Arguments,
        val selection: TodoSelection = TodoSelection.ALL,
    ) : Wirespec.GraphQLRequest {
        override fun toGraphQL(): String =
            "query QueryTodo(\$id: ID!) { $alias: todo(id: \$id) ${selection.toGraphQL()} }"

        public companion object {
            public fun parse(field: Wirespec.GraphQL.Selection.Field, context: Wirespec.GraphQL.Context): Request
        }
    }

    /** The values of the arguments, resolved: the server has already substituted variables and defaults. */
    public data class Arguments(val id: ID)

    // Unchanged entry points for the client; they now build the typed request.
    public fun request(id: ID): Request = Request(arguments = Arguments(id))
    public fun request(id: ID, selection: TodoSelection.Builder.() -> Unit): Request =
        Request(arguments = Arguments(id), selection = TodoSelection.Builder().apply(selection).build())
}
```

- `arguments` holds **values**, not variables. On the wire the client still sends every argument as a variable named
  after it (`$id`), exactly as today, so `Arguments` doubles as the JSON `variables`. On the server it is filled from
  literals or variables alike: `todo(id: "1")` and `todo(id: $x)` parse to the same `Request`.
- An rpc without a result to select (a scalar, an enum, `Unit`) has no `selection`.
- The full request is the request whose selection is `ALL`. `TodoSelection.ALL` is generated from the same walk that
  `GraphQLOperations.document` does today, so the documents the client sends do not change: the existing fixtures
  check that `Request(...).toGraphQL()` equals `GraphQLOperations.document(rpc)`.
- The prefix of the document (variable declarations, the fields that lead to a namespace type) stays a build-time
  constant from `GraphQLOperations.open`/`close`, now split around the alias.

`Wirespec.GraphQLRequest` changes accordingly:

```kotlin
public interface GraphQLRequest {
    public val alias: String
    public fun toGraphQL(): String
}
```

The client edge prints the request into a wire body, a nested `GraphQLBody(query: String, variables: Arguments?)`,
instead of serializing the request itself.

### 3. The document: the typed root of an operation

A server needs the whole document, not one rpc. Per root operation type the extension generates a binder that turns
the selection set of an operation into a list of rpc requests:

```kotlin
// graphql/GraphQLOperation.kt
public sealed interface GraphQLOperation {
    val name: String?
    public data class Query(override val name: String?, val fields: List<QueryField>) : GraphQLOperation
    public data class Mutation(override val name: String?, val fields: List<MutationField>) : GraphQLOperation

    public companion object {
        public fun parse(document: Wirespec.GraphQL.Document, operationName: String?, context: Wirespec.GraphQL.Context): GraphQLOperation
    }
}

/** A field of `Query`: an rpc marked `@GraphQLQuery`, or `__typename`. */
public sealed interface QueryField { val alias: String }
public sealed interface MutationField { val alias: String }
public data class QueryTypename(override val alias: String) : QueryField
```

`QueryTodoGraphQL.Request`, `TodosGraphQL.Request`, `SearchGraphQL.Request` and `NotificationsGraphQL.Request` implement
`QueryField`; the mutations implement `MutationField`. Root fields keep their alias, so
`{ a: todo(id: 1) { title } b: todo(id: 2) { done } }` parses to two `QueryTodoGraphQL.Request`s, `a` and `b`.

**Namespace types.** An rpc marked `@GraphQLQuery("NotificationsQuery")` is a field of a type that another rpc returns
(`Query.notifications: NotificationsQuery`). The selection of such a type has a `Field` per rpc that carries that rpc's
request:

```kotlin
public data class NotificationsQuerySelection(val fields: List<Field>) {
    public sealed interface Field {
        val alias: String?
        public data class Typename(...) : Field
        public data class Inbox(val request: InboxGraphQL.Request) : Field { override val alias = request.alias }
        public data class InboxCount(val request: InboxCountGraphQL.Request) : Field { ... }
    }
}
```

The server resolves `notifications` itself (its result is an empty object) and dispatches the nested requests to their
own handlers, so a nested rpc has a handler like any other.

### 4. The handler: a function from request to response

Each rpc gets a `Handler` next to its client:

```kotlin
object QueryTodoGraphQL {
    public fun interface Handler : Wirespec.GraphQLHandler {
        public suspend fun queryTodo(request: Request): SelectedResponse

        public companion object {
            /** Serves the rpc from its plain `Service`: fetches the full model and keeps what is selected. */
            public fun of(service: QueryTodo.Service): Handler = Handler { request ->
                SelectedResponse(data = service.queryTodo(request.arguments.id)?.let(request.selection::project), errors = null)
            }
        }
    }
}
```

The handler returns the **partial result** (`TodoResult`, every field nullable) that the client already reads, so the
same type travels both ways. Two ways to implement it:

- **Selection-aware.** Read `request.selection` and fetch only what is asked; leave the rest `null`.
- **From the `Service`.** `Handler.of(service)` adapts the plain rpc `Service` that the client also implements: compute
  the full model and `project` it. Zero GraphQL code, at the cost of over-fetching.

An rpc with an error type (`CompleteTodo ! TodoError`) maps its `Error` to an entry of `errors` in `Handler.of`.

In the root package, next to `GraphQLClient`, a `GraphQLHandler` extends every rpc's `Handler`, just like
`GraphQLClient` implements every `Service`:

```kotlin
public interface GraphQLHandler : QueryTodoGraphQL.Handler, TodosGraphQL.Handler, SearchGraphQL.Handler, ... {
    public companion object {
        public fun of(service: QueryTodo.Service & Todos.Service & ...): GraphQLHandler   // one per language idiom
    }
}
```

### 5. Writing the response

`TodoSelection.write(result, path, nullable)` walks the selection, not the result, and builds the `data` as an ordered
`Map<String, Any?>` keyed by response key:

- Only selected fields are written, under their alias; `__typename` is written from the schema (for a union, from the
  member the result is).
- A selected field that the schema declares non-null but the handler left `null` is a field error
  (`Cannot return null for non-nullable field Todo.title`, with its `path`), and the null propagates to the nearest
  nullable parent, as the spec requires.
- Leaf values (`ID`, `Priority`, `Int`) go into the map as they are; the `Wirespec.Serialization` serializes the map,
  which it can since it already serializes the models.

The result of the call is a `Wirespec.GraphQL.Written(data: Any?, errors: List<Wirespec.GraphQL.Error>)`, and the
server prefixes the error paths with the root alias.

### 6. The server

```kotlin
public class GraphQLServer(
    private val serialization: Wirespec.Serialization,
    private val handler: GraphQLHandler,
) : Wirespec.GraphQLServer {
    override suspend fun handle(request: Wirespec.RawRequest): Wirespec.RawResponse
}
```

`handle` does, in order:

1. Read the body as a `Wirespec.GraphQL.Body(query, operationName, variables)` through the serialization.
2. `Wirespec.GraphQL.parse(query)` into the untyped `Document`. A syntax error is a response with `errors` and no `data`.
3. `GraphQLOperation.parse(document, operationName, context)` into the typed operation. A validation error (unknown
   field, missing required argument, a leaf with a selection set, ...) is a response with `errors` and no `data`.
4. Dispatch each root field to its handler with a generated, exhaustive `when` over `QueryField` / `MutationField`.
   Query fields may run concurrently; mutation fields run one after the other, as the spec requires.
5. Write every result under its alias and serialize `{"data": ..., "errors": ...}` into the `RawResponse`.

It only speaks `Wirespec.RawRequest`/`RawResponse`, so it plugs into any HTTP stack the same way an endpoint's
`ServerEdge` does:

```kotlin
val server = GraphQLServer(WirespecSerialization(), object : GraphQLHandler {
    override suspend fun queryTodo(request: QueryTodoGraphQL.Request) = QueryTodoGraphQL.SelectedResponse(
        data = todos.find(request.arguments.id)?.let { todo ->
            QueryTodoGraphQL.TodoResult(
                id = todo.id,
                title = todo.title.takeIf { request.selection.title },
                done = todo.done.takeIf { request.selection.done },
                priority = todo.priority.takeIf { request.selection.priority },
                owner = request.selection.owner?.let { owner -> owner.project(users.find(todo.ownerId)) },
            )
        },
        errors = null,
    )
    override suspend fun todos(request: TodosGraphQL.Request) = TodosGraphQL.Handler.of(todoService).todos(request)
    // ...
})

// Spring, Ktor, http4k, a test: anything that gives a RawRequest
post("/graphql") { respond(server.handle(call.toRawRequest())) }
```

The `Wirespec.GraphQLServer` and `Wirespec.GraphQLServerEdge` interfaces mirror their client counterparts, so the
Spring integration can later register a `GraphQLServer` bean the way it registers endpoint handlers.

## The shared runtime

Added by the extension to the shared `Wirespec` file, in a `Wirespec.GraphQL` namespace so the existing flat
`Wirespec.GraphQL*` interfaces stay as they are:

```kotlin
public object GraphQL {
    // The wire format
    public data class Body(val query: String, val operationName: String?, val variables: Map<String, Any?>?)
    public data class Error(val message: String, val locations: List<Location>?, val path: List<Any>?, val extensions: Map<String, Any?>?)
    public data class Location(val line: Int, val column: Int)
    public class Failure(public val errors: List<Error>) : RuntimeException()
    public data class Written(val data: Any?, val errors: List<Error>)

    // The executable document, untyped (spec §2: Document, OperationDefinition, FragmentDefinition, ...)
    public data class Document(val operations: List<Operation>, val fragments: Map<String, Fragment>)
    public data class Operation(val type: OperationType, val name: String?, val variables: List<VariableDefinition>, val directives: List<Directive>, val selections: List<Selection>)
    public enum class OperationType { QUERY, MUTATION, SUBSCRIPTION }
    public data class VariableDefinition(val name: String, val type: TypeRef, val defaultValue: Value?)
    public data class Fragment(val name: String, val typeCondition: String, val directives: List<Directive>, val selections: List<Selection>)
    public sealed interface Selection {
        public data class Field(val alias: String?, val name: String, val arguments: Map<String, Value>, val directives: List<Directive>, val selections: List<Selection>, val location: Location) : Selection
        public data class FragmentSpread(val name: String, val directives: List<Directive>, val location: Location) : Selection
        public data class InlineFragment(val typeCondition: String?, val directives: List<Directive>, val selections: List<Selection>, val location: Location) : Selection
    }
    public sealed interface Value { Variable, IntValue, FloatValue, StringValue, BooleanValue, NullValue, EnumValue, ListValue, ObjectValue }
    public sealed interface TypeRef { Named, ListOf, NonNull }
    public data class Directive(val name: String, val arguments: Map<String, Value>)

    public fun parse(text: String): Document                 // lexer + recursive descent, Failure with locations

    /** What the generated binders need: fragments, variables, and how to turn a value into a typed argument. */
    public class Context(document: Document, operation: Operation, variables: Map<String, Any?>, serialization: Serialization) {
        public fun fields(selections: List<Selection>, type: String): List<Selection.Field>     // inlines fragments, applies @skip/@include, merges response keys
        public fun inlineFragments(selections: List<Selection>): Map<String, List<Selection>>  // for unions: member -> selections
        public fun <T> argument(field: Selection.Field, name: String, type: KType, default: Value?, required: Boolean): T
    }
}
```

**Typed arguments.** A literal or a variable becomes a JSON-shaped tree (`String`, number, `Boolean`, `null`, `List`,
`Map`; an enum value as its name), which `argument` turns into the typed value by a round trip through the
`Serialization` the server already has: `serializeBody(tree)` then `deserializeBody(bytes, type)`. That reuses the
mapping of refined types, enums and input objects that the models already have, and needs no new interface. If that
round trip turns out to be too slow, a `deserializeValue(tree, type)` can be added to `Serialization` later without
changing the generated binders.

**Why a hand-written parser per language.** The IR has no loops, so the lexer and parser cannot be written in the IR
DSL. They are emitted as one `RawElement` per `Dialect` (Kotlin, Java, Scala), roughly 300 lines each, ported from
`GraphQLLexer`/`GraphQLDocumentParser` in the converter (which already handle strings, block strings, escapes and
locations) and extended with the executable definitions that converter rejects. Everything schema-specific (the
binders, printers, writers) is generated through the IR as today.

## Language shapes

| | Kotlin | Java | Scala |
|---|---|---|---|
| Selection | `data class TodoSelection(fields: List<Field>)` | `record TodoSelection(List<Field> fields)` | `case class TodoSelection(fields: List[TodoSelection.Field])` |
| `Field` | `sealed interface` with nested data classes | `sealed interface` with nested records | `sealed trait` + case classes in the companion |
| Look-ahead | `val title: Boolean`, `val owner: UserSelection?` | `boolean title()`, `Optional<UserSelection> owner()` | `def title: Boolean`, `def owner: Option[UserSelection]` |
| Builder lambda | `TodoSelection.Builder.() -> Unit` | `Function<TodoSelection.Builder, TodoSelection.Builder>` | `TodoSelection.Builder => TodoSelection.Builder` |
| Handler | `suspend fun` | `CompletableFuture<SelectedResponse>` | blocking |
| `QueryField` | `sealed interface` | `sealed interface` (same package) | plain `trait`: Scala only seals within one file |

The client's selection lambdas keep compiling: `{ title(); owner { name() } }` and `t -> t.id().owner(o -> o.name())`
infer the new `Builder` type.

Scala cannot seal `QueryField` across the rpc files. That does not matter for users: the only `match` over it is in the
generated server, and the generator, not the compiler, guarantees it is exhaustive.

## Scope

In:

- Queries and mutations, aliases, variables with defaults, named and inline fragments, `@skip`/`@include`, `__typename`.
- Validation that the typed binding gives for free: unknown fields, missing required arguments, leaf/composite
  selection mismatches, conflicting response keys, unknown fragments, undefined variables.

Out, for later:

- Subscriptions (the client skips them too).
- Introspection (`__schema`, `__type`). The SDL is known at build time (`WirespecToGraphQL`), so a follow-up can serve
  it; until then tools like GraphiQL need the SDL file.
- The same field selected twice with **different arguments** under different aliases, below the root. The partial
  result has one property per field, so it cannot hold both; the binder rejects it with a clear message. (At the root,
  and for nested rpcs, each alias is its own request, so it works there.)
- Printing a namespace selection with nested rpc requests on the client: the client does not build those today.
- GET requests and `application/graphql-response+json` from the GraphQL-over-HTTP spec.

## Implementation plan

Each step is a PR that keeps the build and the docs green.

1. **Typed selections on the client.** Replace the `StringBuilder` selections by the `Field` tree, its `Builder`,
   `toGraphQL`, `plus` and `ALL`; replace `GraphQLRequest`/`GraphQLVariables` by `Request`/`Arguments`; change
   `Wirespec.GraphQLRequest`; let the edges print the wire body. Split `GraphQLQueryBuilder` into
   `GraphQLSelections` (selection tree, builder, printer) and `GraphQLResults` (partial results).
   *Tests:* the documents sent stay byte-for-byte the same (existing client tests and docs examples), and
   `Request(...).toGraphQL() == GraphQLOperations.document(rpc)` for every rpc of `todo.ws` and `notifications.ws`.
2. **Shared runtime parser.** `Wirespec.GraphQL` AST, `parse`, `Error`, `Failure`, in the three dialects, in a new
   `GraphQLRuntime.kt` next to `Dialect.graphQLTypes()`.
   *Tests:* the parse examples of the spec, every literal kind, block strings, syntax errors with line and column; run
   against the Java, Kotlin and Scala output from the existing `jvmTest` setup.
3. **Typed binding.** `parse` on every selection and request, `GraphQLOperation`, `QueryField`/`MutationField`, and
   `Context` with fragment inlining, directives, merging and argument coercion.
   *Tests:* the round trip `parse(print(request)) == request` over every rpc and a range of selections; hand-written
   documents with aliases, fragments, `@skip`/`@include`, inline literals versus variables; every validation error.
4. **Server.** `Handler` and `Handler.of(service)` per rpc, `project`, `write` with null propagation,
   `GraphQLHandler`, `GraphQLServer`, `Wirespec.GraphQLServer`/`GraphQLServerEdge`.
   *Tests:* the generated **client against the generated server**, in memory: a `Wirespec.Transportation` that calls
   `server.handle` directly. Every call and selection of the existing client tests must give the same result, and
   a fixture service gives the expected JSON for hand-written documents.
5. **Docs and release.** A "Serving operations" section in `integration-graphql.mdx` with compiled examples in the three
   languages, the README, and a blog post.

## Open questions

1. **One extension or two?** A server-only project does not need the clients. Options: keep `GraphQLClient` and add
   `GraphQLServer`, both emitting the shared per-rpc file (requests, selections, results) and de-duplicating when both
   are registered; or one `GraphQL` extension that emits both. The second is simpler; the first keeps the generated code
   small. Proposal: one `GraphQL` extension, with `GraphQLClient` kept as an alias.
2. **Error type of an rpc.** `Handler.of` writes the `Error` of `CompleteTodo ! TodoError` as an entry of `errors`,
   which needs a `message`. Do we require the error type to have one, or put it in `extensions` with a generic message?
3. **Leaf serialization.** The response data is a `Map<String, Any?>` with typed leaves. Jackson handles that through
   the runtime type; a serialization that needs a static type per value would need a hook. Acceptable?
4. **Status codes.** Return `200` for field errors and `400` for syntax and validation errors (GraphQL-over-HTTP with
   `application/graphql-response+json`), or always `200` (`application/json`)?
