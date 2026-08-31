# Wirespec × Aeron: rpc in both directions

One Wirespec spec ([`spec/quote.ws`](spec/quote.ws)), two languages, no HTTP: a Kotlin Spring Boot
backend and a Rust client exchange Wirespec `rpc` frames through an [Aeron](https://aeron.io/)
media driver over shared memory, using the wire protocol of the `aeron-jvm` integration
([`src/integration/aeron`](../../src/integration/aeron)).

- [`server/`](server) — the Kotlin Spring Boot backend: the `wirespec-maven-plugin` compiles the
  spec, `QuoteService` implements the generated `Service` interfaces (`GetQuote`, `Ping`,
  `GetWatchlistQuotes`), and the app embeds the Aeron media driver.
- [`client/`](client) — the Rust client on [aeron-rs](https://crates.io/crates/aeron-rs):
  `gen.sh` compiles the same spec to Rust, and the generated `GetWatchlist::Service` trait is
  implemented by the client's own serving side.
- [`integration-test/`](integration-test) — runs both in Docker: two containers sharing one IPC
  namespace, so both map the same `/dev/shm`.

The calls flow in both directions. The client calls the backend (`Ping`, `GetQuote`), and when the
client calls `GetWatchlistQuotes`, the backend turns around and calls the **client's**
`GetWatchlist` rpc to learn which symbols to quote — a server-to-client call over the same medium:

```
client ── GetWatchlistQuotes ──▶ server
client ◀── GetWatchlist ───────── server      (the reverse direction)
client ── Watchlist ───────────▶ server
client ◀── QuoteList ──────────── server
```

Build everything and run all tests — the Spring in-JVM tests, the Rust codec tests, and (when a
Docker daemon is reachable) the two-container Docker test:

```shell
./gradlew :examples:build-aeron
```

Requires the Wirespec artifacts in Maven local first (`./gradlew publishToMavenLocal` from the
repository root).

Run it by hand instead: start the backend, then the client — it attaches to the backend's media
driver directory (default `${java.io.tmpdir}/wirespec-aeron`):

```shell
../build/maven-wrapper/mvnw -f server spring-boot:run
cd client && cargo run --release --bin client
```

```
Ping -> pong
GetQuote AAPL -> AAPL 178.25 USD
GetQuote NOPE -> error UNKNOWN_SYMBOL: No quote for symbol 'NOPE'
Serving GetWatchlist for the backend
GetWatchlistQuotes -> AAPL 178.25 USD
GetWatchlistQuotes -> FLCK 42 EUR
```
