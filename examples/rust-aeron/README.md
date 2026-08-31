# Wirespec × Aeron: Rust client

A Rust client for the Kotlin Spring Boot backend in
[`../maven-spring-aeron`](../maven-spring-aeron), talking [Aeron](https://aeron.io/) instead of
HTTP: both processes attach to the same Aeron media driver and exchange Wirespec rpc frames over
shared memory.

`gen.sh` compiles the backend's spec (`../maven-spring-aeron/src/main/wirespec/quote.ws`) to Rust
with the Wirespec CLI, `src/protocol.rs` mirrors the wire protocol of the `aeron-jvm` integration
([`src/integration/aeron`](../../src/integration/aeron)) — the golden test vectors are shared
verbatim with its Kotlin tests — and `src/client.rs` implements a synchronous request/response
client on [aeron-rs](https://crates.io/crates/aeron-rs).

Build and test (no media driver needed; the tests cover the codec and the mapping onto the
generated types):

```shell
./gradlew :examples:build-rust-aeron
```

Talk to the backend — start it first (`../build/maven-wrapper/mvnw spring-boot:run` in
`../maven-spring-aeron`), then:

```shell
cargo run --bin client                # media driver dir defaults to $TMPDIR/wirespec-aeron
cargo run --bin client -- /tmp/wirespec-aeron
```

```
Ping -> pong
GetQuote AAPL -> AAPL 178.25 USD
GetQuote NOPE -> error UNKNOWN_SYMBOL: No quote for symbol 'NOPE'
```
