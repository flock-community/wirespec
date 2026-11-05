# GraphQL client integration

Ships the `GraphQLClientExtension`, an IR extension that generates a typed Kotlin, Java or Scala GraphQL client for the
rpcs marked `@GraphQLQuery` or `@GraphQLMutation`, such as the rpcs the GraphQL converter writes for a schema. The generated
client is dependency-free: it sends the operation through `Wirespec.Transportation` and `Wirespec.Serialization`.

The extension is bundled with every Wirespec plugin as `GraphQLClient`. See the
[documentation](https://wirespec.io/docs/integration/graphql) for the setup and the generated code.
