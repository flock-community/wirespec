# @flock/wirespec-aeron (TypeScript)

The TypeScript/Node counterpart of the [`aeron-jvm` integration](..): calls Wirespec `rpc`
definitions over [Aeron](https://aeron.io/), speaking the exact wire protocol documented in the
[integration README](../README.md) — the golden test vectors here are shared verbatim with the
Kotlin `RpcFrameTest` and the [`wirespec-aeron` Rust crate](../rust).

- `protocol` — the versioned little-endian frame codec.
- `aeron` — a thin FFI binding (via [koffi](https://www.npmjs.com/package/koffi)) over the
  [Aeron C client library](https://github.com/aeron-io/aeron) (`libaeron.so`): attach to a media
  driver, add publications and subscriptions, offer, poll with fragment reassembly. There is no
  native JavaScript Aeron client; the C client speaks the same driver protocol as the Java and
  Rust ones, so a Node process attaches to any media driver of the same version through it.
- `client` — an async rpc client: requests out on the shared request stream, responses correlated
  by id on this client's own reply stream.

Point `WIRESPEC_AERON_LIB` at a `libaeron.so` built at the media driver's version (see
`examples/aeron/client-ts/build-libaeron.sh`), or leave it unset to load from the system path.
Consumed by path dependency (the repository publishes no npm artifact for it):

```json
"dependencies": { "@flock/wirespec-aeron": "file:../../../src/integration/aeron/typescript" }
```

```shell
npm install && npm test   # frame codec golden tests, no media driver needed
```
