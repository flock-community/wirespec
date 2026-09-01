# Wirespec × Aeron: rpc in both directions

One Wirespec spec ([`spec/quote.ws`](spec/quote.ws)), three languages, no HTTP: a Kotlin Spring
Boot backend, a Rust client and a TypeScript client exchange Wirespec `rpc` frames over
[Aeron](https://aeron.io/) UDP - every channel is an `aeron:udp` endpoint, each side attaches to
its own media driver, and the payloads are CBOR (binary) - using the wire protocol of the
`aeron-jvm` integration ([`src/integration/aeron`](../../src/integration/aeron)).

- [`server/`](server) — the Kotlin Spring Boot backend: the `wirespec-maven-plugin` compiles the
  spec, `QuoteService` implements the generated `Service` interfaces (`GetQuote`, `Ping`,
  `GetWatchlistQuotes`), and the app embeds the Aeron media driver.
- [`client/`](client) — the Rust client on [aeron-rs](https://crates.io/crates/aeron-rs):
  `gen.sh` compiles the same spec to Rust, and the generated `GetWatchlist::Service` trait is
  implemented by the client's own serving side.
- [`client-ts/`](client-ts) — the TypeScript client on the Aeron C client library (via FFI):
  `gen.sh` compiles the same spec to TypeScript, and each rpc maps onto the generated types
  through a minimal CBOR codec.
- [`integration-test/`](integration-test) — runs all of them in Docker, connected over the
  container network: the backend embeds its driver, and each client pod is a media-driver sidecar
  plus the client sharing one IPC namespace.

The calls flow in both directions. A client calls the backend (`Ping`, `GetQuote`), and when a
client calls `GetWatchlistQuotes`, the backend turns around and calls the **Rust client's**
`GetWatchlist` rpc to learn which symbols to quote — a server-to-client call over the same medium.
When the TypeScript client asks, the loop crosses three languages:

```
ts client ── GetWatchlistQuotes ──▶ server
              rust client ◀── GetWatchlist ── server      (the reverse direction)
              rust client ── Watchlist ─────▶ server
ts client ◀── QuoteList ─────────── server
```

Build everything and run all tests — the Spring in-JVM tests, the Rust and TypeScript codec
tests, and (when a Docker daemon is reachable) the multi-container Docker test:

```shell
./gradlew :examples:build-aeron
```

Requires the Wirespec artifacts in Maven local first (`./gradlew publishToMavenLocal` from the
repository root).

Run it by hand instead (single machine: both sides attach to the backend's driver directory,
default `${java.io.tmpdir}/wirespec-aeron`, and the UDP endpoints default to `localhost`; on
separate machines, run a media driver next to the client and point the `*_CHANNEL` variables and
`wirespec.aeron.*Channel` properties at the real hostnames):

```shell
../build/maven-wrapper/mvnw -f server spring-boot:run
cd client && cargo run --release --bin client
cd client-ts && npm start                        # after ./build.sh; needs a local media driver
```

```
Ping -> pong
GetQuote AAPL -> AAPL 178.25 USD
GetQuote NOPE -> error UNKNOWN_SYMBOL: No quote for symbol 'NOPE'
Serving GetWatchlist for the backend
GetWatchlistQuotes -> AAPL 178.25 USD
GetWatchlistQuotes -> FLCK 42 EUR
```
