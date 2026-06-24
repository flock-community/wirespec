# Wirespec × Spring Boot 4 HTTP Service Client

This example shows how to drive a Wirespec-generated client with Spring Boot 4 /
Spring Framework 7's [HTTP Service Client enhancements](https://spring.io/blog/2025/09/23/http-service-client-enhancements).

Wirespec generates typed endpoints whose `Handler` methods take a single
`Request` and return a sealed `Response<*>` union, so the generated client cannot
be backed by Spring's stock `HttpServiceProxyFactory` directly. Instead, the
endpoints are driven by a small synchronous `RestClient` adapter, and that
`RestClient` is configured with the **new per-group HTTP service configuration**.

## Pieces

| File | Role |
|------|------|
| [`WirespecRestClient`](src/main/java/community/flock/wirespec/examples/maven/spring/integration/client/WirespecRestClient.java) | Synchronous client backed by `RestClient`; serializes a Wirespec `Request` and deserializes the `Response`. The `RestClient` counterpart of the integration's reactive `WirespecWebClient`. |
| [`WirespecClientConfig`](src/main/java/community/flock/wirespec/examples/maven/spring/integration/client/WirespecClientConfig.java) | Declares the `todo` HTTP service group with `@ImportHttpServices`, configures it with a `RestClientHttpServiceGroupConfigurer`, and exposes the `WirespecRestClient` bean. |
| [`TodoServiceClient`](src/main/java/community/flock/wirespec/examples/maven/spring/integration/client/TodoServiceClient.java) | A plain `@HttpExchange` interface that registers the `todo` group so its group properties/configurer become active. |
| [`TodoClient`](src/main/java/community/flock/wirespec/examples/maven/spring/integration/client/TodoClient.java) | Hand-written delegating handler implementing the generated `Handler` interfaces. |

## Configuration

Group settings live under `spring.http.client.service.group.<group>.*`
(see [`application.yaml`](src/main/resources/application.yaml)):

```yaml
spring:
  http:
    client:
      service:
        group:
          todo:
            base-url: http://localhost:8080
```

The same group is configured programmatically:

```java
@Bean
RestClientHttpServiceGroupConfigurer todoGroupConfigurer() {
    return groups -> groups.filterByName("todo")
            .forEachClient((group, builder) -> builder.defaultHeader("User-Agent", "wirespec-todo-client"));
}
```

## Run the tests

```shell
./mvnw -pl examples/maven-spring-boot-4-integration test
```

[`TodoClientTest`](src/test/java/community/flock/wirespec/examples/maven/spring/integration/TodoClientTest.java)
starts the server on the fixed port the group is pointed at and calls `/todos`
through the typed Wirespec client.
