# GraphQL integration

Ships the `GraphQLClientExtension`, an IR extension that generates a typed Kotlin, Java or Scala GraphQL client and
server for the rpcs marked `@GraphQLQuery` or `@GraphQLMutation`, such as the rpcs the GraphQL converter writes for a
schema. The generated code is dependency-free: it only uses `Wirespec.Transportation` and `Wirespec.Serialization`.

The extension adds the types of its clients to the shared `Wirespec` file, so generate that file along with them
(`shared`, on by default in Gradle and Maven, `--shared` in the CLI).

The extension is bundled with every Wirespec plugin as `GraphQLClient`. See the
[documentation](https://wirespec.io/docs/integration/graphql) for the setup and the generated client.

## Server

Every rpc has a `Service` with one function, which the client implements by sending the operation. To serve the
operations, implement those functions yourself and hand them to the `GraphQLServer` the extension writes in the root
package, next to the `GraphQLClient`. `GraphQLServer.Service` is the `Service` of every operation at once:

```kotlin
class TodoService : GraphQLServer.Service {
    override suspend fun queryTodo(id: ID): Todo? = repository.find(id)
    override suspend fun todos(done: Boolean?): List<Todo> = repository.all(done)
    override suspend fun addTodo(input: NewTodo): Todo = repository.add(input)
    // ... one function for each rpc
}

val server = GraphQLServer(serialization, TodoService())

// Route `POST /graphql` of your HTTP server here.
val response: Wirespec.RawResponse = server.handle(request)
```

`handle` takes the request of any GraphQL client as a `Wirespec.RawRequest`, with `{"query", "operationName",
"variables"}` as its JSON body, and gives the response to send back: status 200 and `{"data", "errors"}`. It suspends in
Kotlin, returns a `CompletableFuture` in Java and blocks in Scala, like the functions of the `Service`.

The server reads the document of the request, so it answers what the client asks for:

- It calls the `Service` of each field that is an rpc, with the arguments of the field read as the parameters of the
  rpc. An argument is a literal or a variable; one that is left out gets the default of the variable or of the schema.
- It answers with the fields the query selects of each result, under their aliases, through named fragments, inline
  fragments with a type condition, `@skip` and `@include`. `__typename` is the name of the type, also for the member of
  a union.
- The fields of a mutation run one after the other.
- An rpc of a namespace type, `@GraphQLQuery("NotificationsQuery")`, is served as a field of that type, inside the
  field that returns it.
- A `Service` that throws makes its field `null` and adds an error with the message of the exception and the path of
  the field; the other fields are still answered. The `Error` of an rpc with an error type is written to `errors` as it
  is, which is where the generated client reads it.
- A document that cannot be read, or that asks for a field that does not exist, is answered with `errors` and no `data`.

The parser and the execution are part of the generated `GraphQLServer`, so it needs no GraphQL library. The
`Wirespec.Serialization` reads the arguments and writes the response as a map of any values, so it has to support
those: `WirespecSerialization` of the Jackson integration does.

Not supported: subscriptions, introspection (`__schema` and `__type`), and arguments of the fields of a result, which
are ignored. The server does not check a document against the schema before it runs it, so an unknown field is only
reported when it is reached. The message of an exception is sent to the client, so throw exceptions whose message may
be read by it.
