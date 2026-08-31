# Aeron example: Rust client

The client half of [the Aeron example](../README.md), built on
[aeron-rs](https://crates.io/crates/aeron-rs). `gen.sh` compiles
[`../spec/quote.ws`](../spec/quote.ws) to Rust with the Wirespec CLI, `src/protocol.rs` mirrors
the wire protocol of the `aeron-jvm` integration — the golden test vectors are shared verbatim
with its Kotlin tests — and `src/client.rs` implements a synchronous request/response client.

It also serves: `src/server.rs` implements the generated `GetWatchlist::Service` trait and answers
the backend's server-to-client calls on its own stream.

```shell
./build.sh                        # generate, build --release, test (no media driver needed)
cargo run --release --bin client  # talk to the running backend (../server)
```
