# Example: JSON-RPC with Rust

Serves the rpcs of [users.ws](users.ws) as JSON-RPC 2.0 methods over HTTP with actix-web, and calls them with the
generated client over reqwest.

The [JsonRpc extension](https://wirespec.io/docs/integration/json-rpc) generates a `json_rpc` module with a
`JsonRpcClient` and a `JsonRpcServer`, next to the models and rpcs. Both are dependency-free; this example brings the
two things that depend on the stack, just like it would for endpoints:

- [transportation.rs](src/transportation.rs) posts a request to the server with reqwest;
- [serialization.rs](src/serialization.rs) reads and writes the params, results and errors with `serde_json`. The
  [RustSerde extension](https://wirespec.io/docs/integration/rust-serde) makes every model a serde type, so it covers
  them all.

[server.rs](src/server.rs) hands the requests of `POST /rpc` to the server, which calls [Users](src/users.rs), the
implementation of the `Service` of every rpc.

## Generate

[gen.sh](gen.sh) builds the Wirespec CLI when it is missing and generates `src/generated`:

```shell
wirespec compile -i users.ws -o src/generated -l Rust -p '' --shared -e JsonRpc -e RustSerde
```

## Run

```shell
./gen.sh
```

```shell
cargo run --bin server
```

```shell
cargo run --bin client
```

```text
Created: User { id: "1", name: "Ada", email: Some("ada@example.com") }
Renamed: User { id: "1", name: "Ada Lovelace", email: Some("ada@example.com") }
No user 42
All: [User { id: "1", name: "Ada Lovelace", email: Some("ada@example.com") }]
```

Any JSON-RPC client can call the server too:

```shell
curl -s localhost:8080/rpc -d '{"jsonrpc":"2.0","method":"findUsers","id":1}'
```

```json
{"jsonrpc":"2.0","result":[{"id":"1","name":"Ada Lovelace","email":"ada@example.com"}],"id":1}
```

The client's functions return a `Result`: the Rust `Service` of an rpc cannot fail, so the client does not implement it,
and fails with a `JsonRpcError` when the request could not be sent, the response could not be read, or the server
answered with an error object. The [tests](tests/json_rpc.rs) call the server with the generated client, and with plain
JSON-RPC requests.
