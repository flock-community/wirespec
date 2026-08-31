# Wirespec Aeron integration

Serves and calls Wirespec `rpc` definitions over [Aeron](https://aeron.io/), using Aeron as the
transport and the Wirespec body serialization contract as the payload encoding - any
`Wirespec.BodySerialization` plugs in, textual (JSON) or binary (e.g. Jackson's `CBORMapper`).

The module is language-neutral on the wire: any client that speaks the frame format below can talk
to an `AeronRpcServer`, whatever language it is written in (see `examples/aeron` for a Rust
client talking to a Kotlin Spring Boot backend over UDP with CBOR payloads, in both directions).

## Wire protocol

One Aeron message per rpc request or response, version 1, all integers little-endian:

| field            | type  | notes                                        |
|------------------|-------|----------------------------------------------|
| protocol version | `u8`  | `1`                                          |
| frame kind       | `u8`  | `1` REQUEST, `2` RESULT, `3` ERROR           |
| correlation id   | `i64` | echoed back on the response                  |
| method           | `u16` length + UTF-8 bytes | the rpc's name          |
| reply channel    | `u16` length + UTF-8 bytes | REQUEST only            |
| reply stream id  | `i32` | REQUEST only                                 |
| payload          | `u32` length + bytes                                 |

Requests arrive on one shared channel/stream; every request names the channel and stream its
response should be published on, so each client subscribes to its own reply stream - on any Aeron
channel, `aeron:ipc` (shared memory) or `aeron:udp` (network) alike. Payload bytes are produced by
the configured body serialization: a REQUEST payload is the rpc's parameters keyed by name (empty
for a parameterless rpc), RESULT and ERROR payloads hold the rpc's result and error values, and a
plain string value is raw UTF-8 text.

## Usage

```kotlin
val server = AeronRpcServer(aeron, serialization).apply {
    bindEmpty("Ping") { "pong" }                                      // rpc Ping {} -> String
    bindResult<GetQuoteParams, Quote, QuoteError>("GetQuote") { ... } // rpc GetQuote { symbol: String } -> Quote ! QuoteError
    start()
}

val client = AeronRpcClient(aeron, serialization).apply { start() }
client.call<String>("Ping")
client.callResult<GetQuoteParams, Quote, QuoteError>("GetQuote", GetQuoteParams("AAPL"))
```

Any `Wirespec.BodySerialization` works; the Jackson integration provides one, and handing its
`WirespecSerialization` a `CBORMapper` instead of an `ObjectMapper` makes the payloads binary. Agrona reaches into
JDK internals, so JVMs running an Aeron media driver or client need:

```
--add-opens java.base/jdk.internal.misc=ALL-UNNAMED --add-opens java.base/sun.nio.ch=ALL-UNNAMED
```
