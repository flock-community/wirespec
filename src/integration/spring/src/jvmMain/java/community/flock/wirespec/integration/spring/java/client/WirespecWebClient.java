package community.flock.wirespec.integration.spring.java.client;

import community.flock.wirespec.java.Wirespec;
import community.flock.wirespec.java.Wirespec.Serialization;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class WirespecWebClient {
    private final WebClient client;
    private final Serialization wirespecSerde;
    private final Map<Class<?>, Method> toRequestCache = new ConcurrentHashMap<>();
    private final Map<Class<?>, Method> fromResponseCache = new ConcurrentHashMap<>();

    public WirespecWebClient(WebClient client, Serialization wirespecSerde) {
        this.client = client;
        this.wirespecSerde = wirespecSerde;
    }

    @SuppressWarnings("unchecked")
    public <Req extends Wirespec.Request<?>, Res extends Wirespec.Response<?>> CompletableFuture<Res> send(Req request) {
        try {
            Class<?> declaringClass = request.getClass().getDeclaringClass();
            Method toRequest = toRequestCache.computeIfAbsent(declaringClass, cls -> {
                Class<?> handlerClass = Arrays.stream(cls.getDeclaredClasses())
                        .filter(c -> c.getSimpleName().equals("Handler"))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Handler not found in " + cls));
                return Arrays.stream(handlerClass.getDeclaredMethods())
                        .filter(m -> m.getName().equals("toRequest") && Modifier.isStatic(m.getModifiers()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("toRequest method not found in " + handlerClass));
            });
            Method fromResponse = fromResponseCache.computeIfAbsent(declaringClass, cls -> {
                Class<?> handlerClass = Arrays.stream(cls.getDeclaredClasses())
                        .filter(c -> c.getSimpleName().equals("Handler"))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Handler not found in " + cls));
                return Arrays.stream(handlerClass.getDeclaredMethods())
                        .filter(m -> m.getName().equals("fromResponse") && Modifier.isStatic(m.getModifiers()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("fromResponse method not found in " + handlerClass));
            });

            Wirespec.RawRequest rawRequest = (Wirespec.RawRequest) toRequest.invoke(null, wirespecSerde, request);
            return executeRequest(rawRequest, client)
                    .thenApply(rawResponse -> {
                        try {
                            return (Res) fromResponse.invoke(null, wirespecSerde, rawResponse);
                        } catch (Exception ex) {
                            throw new RuntimeException(ex);
                        }
                    });

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

        if (request.body().isPresent()) {
            spec.contentType(MediaType.APPLICATION_JSON);
            spec.bodyValue(request.body().get());
        }

        return spec.exchangeToMono(response ->
                        response.bodyToMono(byte[].class)
                                .map(body -> new Wirespec.RawResponse(
                                        response.statusCode().value(),
                                        CollectionUtils.toMultiValueMap(response.headers().asHttpHeaders()),
                                        Optional.of(body)
                                ))
                                .defaultIfEmpty(new Wirespec.RawResponse(
                                        response.statusCode().value(),
                                        CollectionUtils.toMultiValueMap(response.headers().asHttpHeaders()),
                                        Optional.empty()
                                ))
                )
                .onErrorResume(throwable -> {
                    if (throwable instanceof WebClientResponseException e) {
                        return Mono.just(new Wirespec.RawResponse(
                                e.getStatusCode().value(),
                                CollectionUtils.toMultiValueMap(e.getHeaders()),
                                Optional.of(e.getResponseBodyAsByteArray())
                        ));
                    } else {
                        return Mono.error(throwable);
                    }
                })
                .toFuture();
    }
}
