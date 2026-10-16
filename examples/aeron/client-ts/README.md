# Aeron example: TypeScript client

The TypeScript half of [the Aeron example](../README.md), built on the `@flock/wirespec-aeron`
package ([`src/integration/aeron/typescript`](../../../src/integration/aeron/typescript)) — the
TypeScript counterpart of the `aeron-jvm` integration, carrying the frame protocol and the
correlating rpc client over an FFI binding to the Aeron C client library.

`gen.sh` compiles [`../spec/quote.ws`](../spec/quote.ws) to TypeScript with the Wirespec CLI; the
payloads travel as CBOR (binary), encoded by the minimal dependency-free codec in `src/cbor.ts`,
so the mapping between rpc frames and generated types is one encode or decode per rpc
(`src/rpc.ts`). Channels are `aeron:udp` endpoints, overridable via `REQUEST_CHANNEL` and
`REPLY_CHANNEL`. When this client calls `GetWatchlistQuotes`, the backend calls the **Rust**
client's `GetWatchlist` rpc and the quotes come back here: three languages in one loop.

```shell
./build.sh                 # generate, build, test, assemble the Docker context
npm start                  # talk to the running backend (../server); needs a local media driver
```

`build.sh` also builds `libaeron.so` from the Aeron sources at the media driver's version
(`build-libaeron.sh`, requires cmake ≥ 3.30) and assembles `target/docker` — a network-free
Docker build context with the node binary, the library, and the app's production `node_modules`.
