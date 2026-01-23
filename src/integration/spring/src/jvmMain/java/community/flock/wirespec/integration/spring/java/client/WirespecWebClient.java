package community.flock.wirespec.integration.spring.java.client;

import community.flock.wirespec.java.Wirespec;
import community.flock.wirespec.java.Wirespec.Serialization;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

public class WirespecWebClient {
    private final WebClient client;
    private final Serialization wirespecSerde;

    public WirespecWebClient(WebClient client, Serialization wirespecSerde) {
        this.client = client;
        this.wirespecSerde = wirespecSerde;
    }

    @SuppressWarnings("unchecked")
    public <Req extends Wirespec.Request<?>, Res extends Wirespec.Response<?>> CompletableFuture<Res> send(Req request) {
        try {
            Class<?> declaringClass = request.getClass().getDeclaringClass();
            Class<?> handler = Arrays.stream(declaringClass.getDeclaredClasses())
                    .filter(c -> c.getSimpleName().equals("Handler"))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Handler not found"));

            Class<?> handlers = Arrays.stream(handler.getDeclaredClasses())
                    .filter(c -> c.getSimpleName().equals("Handlers"))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Handlers not found"));

            Wirespec.Client<Req, Res> instance = (Wirespec.Client<Req, Res>) handlers.getDeclaredConstructor().newInstance();
            Wirespec.ClientEdge<Req, Res> edge = instance.getClient(wirespecSerde);

            return executeRequest(edge.to(request), client)
                    .thenApply(edge::from);

        } catch (Exception e) {
            CompletableFuture<Res> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    private CompletableFuture<Wirespec.RawResponse> executeRequest(Wirespec.RawRequest request, WebClient client) {
        WebClient.RequestBodySpec spec = client.method(HttpMethod.valueOf(request.method()))
                .uri(uriBuilder -> {
                    uriBuilder.path(String.join("/", request.path()));
                    request.queries().forEach((key, value) -> {
                        if (value != null && !value.isEmpty()) {
                            uriBuilder.queryParam(key, value);
                        }
                    });
                    return uriBuilder.build();
                })
                .headers(headers -> request.headers().forEach((key, value) -> {
                    if (value != null && !value.isEmpty()) {
                        headers.addAll(key, value);
                    }
                }));

        if (request.body() != null) {
            spec.contentType(MediaType.APPLICATION_JSON);
            spec.bodyValue(request.body());
        }

        return spec.exchangeToMono(response ->
                        response.bodyToMono(byte[].class)
                                .map(body -> new Wirespec.RawResponse(
                                        response.statusCode().value(),
                                        CollectionUtils.toMultiValueMap(response.headers().asHttpHeaders()),
                                        body
                                ))
                                .defaultIfEmpty(new Wirespec.RawResponse(
                                        response.statusCode().value(),
                                        CollectionUtils.toMultiValueMap(response.headers().asHttpHeaders()),
                                        null
                                ))
                )
                .onErrorResume(throwable -> {
                    if (throwable instanceof WebClientResponseException e) {
                        return Mono.just(new Wirespec.RawResponse(
                                e.getStatusCode().value(),
                                CollectionUtils.toMultiValueMap(e.getHeaders()),
                                e.getResponseBodyAsByteArray()
                        ));
                    } else {
                        return Mono.error(throwable);
                    }
                })
                .toFuture();
    }
}
