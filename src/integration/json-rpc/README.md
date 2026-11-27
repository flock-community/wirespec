# JSON-RPC integration

Ships the `JsonRpcExtension`, an IR extension that sends and serves every rpc of a contract as a
[JSON-RPC 2.0](https://www.jsonrpc.org/specification) method, for Java, Kotlin, Scala, TypeScript, Python and Rust.

It works the way the generated code works for an endpoint: one file next to the `Client` of the endpoints holds a
`JsonRpcClient` with a function for every rpc, and a `JsonRpcServer` that answers single and batch requests by calling
a service that implements the `Service` of every rpc. Both are dependency-free: the consumer brings the
`Wirespec.Transportation` that moves a message and the `Wirespec.Serialization` that reads and writes the values.

The extension is bundled with every Wirespec plugin as `JsonRpc`:

```shell
wirespec compile -i ./wirespec -o ./src/generated -l Kotlin -e JsonRpc
```

See the [documentation](https://wirespec.io/docs/integration/json-rpc) for the generated code, the wire format and the
errors, and the [Kotlin](../../../examples/gradle-json-rpc) and [Rust](../../../examples/rust-json-rpc) examples.
