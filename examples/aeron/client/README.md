# Aeron example: Rust client

The client half of [the Aeron example](../README.md), built on the `wirespec-aeron` crate
([`src/integration/aeron/rust`](../../../src/integration/aeron/rust)) — the Rust counterpart of
the `aeron-jvm` integration, carrying the frame protocol, the correlating rpc client, and the
serving loop.

`gen.sh` compiles [`../spec/quote.ws`](../spec/quote.ws) to Rust with the Wirespec CLI, applying
the `RustSerde` extension so the generated models carry serde derives; the mapping between rpc
frames and generated types is then a serde one-liner per rpc (`src/rpc.rs`). The example itself is
only business logic: `src/terminal.rs` implements the generated `GetWatchlist::Service` trait and
binds it onto a `wirespec_aeron` server, so the backend's server-to-client calls are answered on
this client's own stream.

```shell
./build.sh                        # generate, build --release, test (no media driver needed)
cargo run --release --bin client  # talk to the running backend (../server)
```
