package community.flock.wirespec.integration.spring.java.client;

import community.flock.wirespec.java.Wirespec;
import community.flock.wirespec.java.Wirespec.Serialization;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.AbstractMap;
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
    private final Map<Class<?>, Boolean> streamingCache = new ConcurrentHashMap<>();
    private final Map<Map.Entry<Class<?>, Integer>, Constructor<?>> streamingResponseConstructorCache = new ConcurrentHashMap<>();

    public WirespecWebClient(WebClient client, Serialization wirespecSerde) {
        this.client = client;
        this.wirespecSerde = wirespecSerde;
    }

    @SuppressWarnings("unchecked")
    public <Req extends Wirespec.Request<?>, Res extends Wirespec.Response<?>> CompletableFuture<Res> send(Req request) {
        try {
            Class<?> declaringClass = request.getClass().getDeclaringClass();
            Method toRequest = toRequestCache.computeIfAbsent(declaringClass, cls -> findHandlerMethod(cls, "toRequest"));

            Wirespec.RawRequest rawRequest = (Wirespec.RawRequest) toRequest.invoke(null, wirespecSerde, request);

            if (isStreaming(declaringClass)) {
                return executeStreaming(rawRequest, declaringClass).thenApply(res -> (Res) res);
            }

            Method fromResponse = fromResponseCache.computeIfAbsent(declaringClass, cls -> findHandlerMethod(cls, "fromResponse"));
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

    private static Method findHandlerMethod(Class<?> cls, String name) {
        Class<?> handlerClass = Arrays.stream(cls.getDeclaredClasses())
                .filter(c -> c.getSimpleName().equals("Handler"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Handler not found in " + cls));
        return Arrays.stream(handlerClass.getDeclaredMethods())
                .filter(m -> m.getName().equals(name) && Modifier.isStatic(m.getModifiers()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(name + " method not found in " + handlerClass));
    }

    private boolean isStreaming(Class<?> declaringClass) {
        return streamingCache.computeIfAbsent(declaringClass, cls -> {
            try {
                Class<?> handlerClass = Arrays.stream(cls.getDeclaredClasses())
                        .filter(c -> c.getSimpleName().equals("Handler"))
                        .findFirst()
                        .orElse(null);
                if (handlerClass == null) return false;
                Class<?> handlersClass = Arrays.stream(handlerClass.getDeclaredClasses())
                        .filter(c -> c.getSimpleName().equals("Handlers"))
                        .findFirst()
                        .orElse(null);
                if (handlersClass == null) return false;
                return handlersClass.getField("STREAMING").getBoolean(null);
            } catch (Exception e) {
                return false;
            }
        });
    }

    private CompletableFuture<Wirespec.Response<?>> executeStreaming(Wirespec.RawRequest request, Class<?> declaringClass) {
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

        return spec.<Wirespec.Response<?>>exchangeToMono(response ->
                        response.bodyToMono(Resource.class)
                                .<Wirespec.Response<?>>map(resource -> buildStreamingResponse(declaringClass, response.statusCode().value(), resource))
                                .switchIfEmpty(Mono.<Wirespec.Response<?>>fromCallable(() ->
                                        buildStreamingResponse(declaringClass, response.statusCode().value(), new ByteArrayResource(new byte[0]))))
                )
                .<Wirespec.Response<?>>onErrorResume(throwable -> {
                    if (throwable instanceof WebClientResponseException e) {
                        return Mono.just(buildStreamingResponse(
                                declaringClass,
                                e.getStatusCode().value(),
                                new ByteArrayResource(e.getResponseBodyAsByteArray())
                        ));
                    } else {
                        return Mono.error(throwable);
                    }
                })
                .toFuture();
    }

    private Wirespec.Response<?> buildStreamingResponse(Class<?> declaringClass, int statusCode, Resource resource) {
        Constructor<?> constructor = streamingResponseConstructorCache.computeIfAbsent(
                new AbstractMap.SimpleImmutableEntry<>(declaringClass, statusCode),
                key -> {
                    Class<?> cls = key.getKey();
                    int status = key.getValue();
                    Class<?> responseClass = Arrays.stream(cls.getDeclaredClasses())
                            .filter(c -> c.getSimpleName().equals("Response" + status))
                            .findFirst()
                            .orElseThrow(() -> new IllegalStateException("No Response" + status + " class in " + cls.getName()));
                    return Arrays.stream(responseClass.getDeclaredConstructors())
                            .filter(c -> c.getParameterCount() == 1 && Resource.class.isAssignableFrom(c.getParameterTypes()[0]))
                            .findFirst()
                            .orElseThrow(() -> new IllegalStateException("No 1-arg Resource constructor on " + responseClass.getName()));
                }
        );
        try {
            return (Wirespec.Response<?>) constructor.newInstance(resource);
        } catch (Exception e) {
            throw new RuntimeException(e);
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
