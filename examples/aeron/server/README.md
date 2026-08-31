# Aeron example: Kotlin Spring Boot backend

The server half of [the Aeron example](../README.md). The `wirespec-maven-plugin` compiles
[`../spec/quote.ws`](../spec/quote.ws) to Kotlin (models plus one `Service` interface per rpc),
`QuoteService` implements those interfaces, and `AeronConfiguration` embeds an Aeron media driver
and binds the service to an `AeronRpcServer` from the `aeron-jvm` integration.

The reverse direction lives here too: the `GetWatchlistQuotes` handler uses an `AeronRpcClient`
to call the connected client's `GetWatchlist` rpc before answering.

`QuoteRpcTest` covers all of it in one JVM over IPC — including the server-to-client call, with
the test playing the client's serving role.

```shell
../../build/maven-wrapper/mvnw spring-boot:run    # media driver dir: ${java.io.tmpdir}/wirespec-aeron
```
