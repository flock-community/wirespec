# Example: JSON-RPC with Kotlin and ktor

Serves the rpcs of [users.ws](src/main/wirespec/users.ws) as JSON-RPC 2.0 methods over HTTP, and calls them with the
generated client.

The [JsonRpc extension](https://wirespec.io/docs/integration/json-rpc) generates a `JsonRpcClient` and a
`JsonRpcServer` next to the models and rpcs. Both are dependency-free; this example brings the two things that depend
on the stack, just like it would for endpoints:

- [KtorTransportation](src/main/kotlin/community/flock/wirespec/example/jsonrpc/KtorTransportation.kt) posts a
  request to the server with a ktor client;
- [JacksonSerialization](src/main/kotlin/community/flock/wirespec/example/jsonrpc/JacksonSerialization.kt) reads and
  writes the params, results and errors with Jackson.

[Application.kt](src/main/kotlin/community/flock/wirespec/example/jsonrpc/Application.kt) hands the requests of
`POST /rpc` to the server, which calls [Users](src/main/kotlin/community/flock/wirespec/example/jsonrpc/Users.kt), the
implementation of every rpc.

## Generate

```kotlin
tasks.register<CompileWirespecTask>("wirespec-kotlin") {
    input = layout.projectDirectory.dir("src/main/wirespec")
    output = layout.buildDirectory.dir("generated")
    packageName = "community.flock.wirespec.generated"
    language = Language.Kotlin
    extensionClasses = listOf(JsonRpcExtension::class.java)
}
```

## Run

Publish Wirespec to Maven local from the root of the repository first (`./gradlew publishToMavenLocal`), then:

```shell
../../gradlew run
```

```shell
curl -s localhost:8080/rpc -d '{"jsonrpc":"2.0","method":"createUser","params":{"name":"Ada"},"id":1}'
```

```json
{"jsonrpc":"2.0","result":{"id":"1","name":"Ada","email":null},"id":1}
```

[UsersTest](src/test/kotlin/community/flock/wirespec/example/jsonrpc/UsersTest.kt) calls the server with the
generated client, and with plain JSON-RPC requests.
