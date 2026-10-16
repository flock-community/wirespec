# wirespec-aeron (Rust)

The Rust counterpart of the [`aeron-jvm` integration](..): serves and calls Wirespec `rpc`
definitions over [Aeron](https://aeron.io/), speaking the exact wire protocol documented in the
[integration README](../README.md) — the golden test vectors here are shared verbatim with the
Kotlin `RpcFrameTest`.

- `protocol` — the versioned little-endian frame codec.
- `client` — a synchronous rpc client: requests out on the shared request stream, responses
  correlated by id on this client's own reply stream.
- `server` — a serving loop: bind one handler per rpc method, answer each request on the reply
  channel and stream it names. `block_on` bridges the generated `async` Service traits into sync
  handlers.

Consumed by path dependency (the repository publishes no crates):

```toml
wirespec-aeron = { path = ".../src/integration/aeron/rust" }
```

See [`examples/aeron`](../../../../examples/aeron) for a Rust client talking to a Kotlin Spring
Boot backend through shared memory, in both directions.
