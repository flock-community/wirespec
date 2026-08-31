# Wirespec × Aeron: Kotlin Spring Boot backend

A Spring Boot backend that serves the Wirespec `rpc` definitions in
[`src/main/wirespec/quote.ws`](src/main/wirespec/quote.ws) over [Aeron](https://aeron.io/) instead
of HTTP: Aeron is the transport, the Wirespec body serialization contract (JSON) is the payload
encoding, framed by the protocol of the `aeron-jvm` integration
([`src/integration/aeron`](../../src/integration/aeron)).

The `wirespec-maven-plugin` compiles the spec to Kotlin (models plus one `Service` interface per
rpc), `QuoteService` implements those interfaces, and `AeronConfiguration` embeds an Aeron media
driver and binds the service to an `AeronRpcServer`. The integration test calls the running server
through an `AeronRpcClient` over shared-memory IPC.

Requires the Wirespec artifacts in Maven local (`./gradlew publishToMavenLocal` from the repository
root). Build and test:

```shell
./gradlew :examples:build-maven-spring-aeron
```

Run the backend (media driver directory defaults to `${java.io.tmpdir}/wirespec-aeron`, override
with `--wirespec.aeron.dir=...`):

```shell
../build/maven-wrapper/mvnw spring-boot:run
```

Then talk to it from another process attached to the same media driver — for example the Rust
client in [`../rust-aeron`](../rust-aeron), which reaches this backend through shared memory
without a single HTTP request.
