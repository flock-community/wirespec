package community.flock.wirespec.examples.maven.spring.integration.client;

import community.flock.wirespec.java.Wirespec;
import community.flock.wirespec.java.Wirespec.Serialization;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/**
 * Synchronous Wirespec client backed by Spring Framework 7's {@link RestClient}.
 *
 * <p>This is the {@code RestClient} counterpart of the reactive {@code WirespecWebClient} shipped in
 * the Spring integration. It serializes a typed Wirespec {@code Request} to a {@link
 * Wirespec.RawRequest} using the endpoint's generated {@code Handler}, performs a blocking exchange,
 * and deserializes the {@link Wirespec.RawResponse} back into the typed sealed {@code Response}.
 *
 * <p>The backing {@code RestClient} is configured through Spring Boot 4's HTTP service group
 * properties ({@code spring.http.client.service.group.*}) and the {@code
 * RestClientHttpServiceGroupConfigurer} bean in {@link WirespecClientConfig}.
 */
public class WirespecRestClient {

    private final RestClient client;
    private final Serialization wirespecSerde;
    private final Map<Class<?>, Method> toRequestCache = new ConcurrentHashMap<>();
    private final Map<Class<?>, Method> fromResponseCache = new ConcurrentHashMap<>();

    public WirespecRestClient(RestClient client, Serialization wirespecSerde) {
        this.client = client;
        this.wirespecSerde = wirespecSerde;
    }

    @SuppressWarnings("unchecked")
    public <Req extends Wirespec.Request<?>, Res extends Wirespec.Response<?>> Res send(Req request) {
        try {
            Class<?> declaringClass = request.getClass().getDeclaringClass();
            Method toRequest = toRequestCache.computeIfAbsent(declaringClass, WirespecRestClient::findToRequest);
            Method fromResponse = fromResponseCache.computeIfAbsent(declaringClass, WirespecRestClient::findFromResponse);

            Wirespec.RawRequest rawRequest = (Wirespec.RawRequest) toRequest.invoke(null, wirespecSerde, request);
            Wirespec.RawResponse rawResponse = executeRequest(rawRequest);
            return (Res) fromResponse.invoke(null, wirespecSerde, rawResponse);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Wirespec.RawResponse executeRequest(Wirespec.RawRequest request) {
        RestClient.RequestBodySpec spec = client.method(HttpMethod.valueOf(request.method()))
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

        RestClient.RequestHeadersSpec<?> headersSpec = spec;
        if (request.body().isPresent()) {
            headersSpec = spec.contentType(MediaType.APPLICATION_JSON).body(request.body().get());
        }

        // exchange() gives full control over the response and, unlike retrieve(),
        // does not raise on 4xx/5xx so error statuses map onto typed responses.
        return headersSpec.exchange(
                (req, res) -> new Wirespec.RawResponse(
                        res.getStatusCode().value(),
                        toHeaderMap(res.getHeaders()),
                        Optional.ofNullable(res.bodyTo(byte[].class))),
                false);
    }

    // Spring Framework 7's HttpHeaders no longer implements MultiValueMap, so build the
    // Map<String, List<String>> that Wirespec expects via the header-focused headerSet().
    private static Map<String, List<String>> toHeaderMap(org.springframework.http.HttpHeaders headers) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        headers.headerSet().forEach(entry -> result.put(entry.getKey(), entry.getValue()));
        return result;
    }

    private static Method findToRequest(Class<?> endpointClass) {
        return findHandlerMethod(endpointClass, "toRawRequest", "toRequest");
    }

    private static Method findFromResponse(Class<?> endpointClass) {
        return findHandlerMethod(endpointClass, "fromRawResponse", "fromResponse");
    }

    private static Method findHandlerMethod(Class<?> endpointClass, String... names) {
        Class<?> handlerClass = Arrays.stream(endpointClass.getDeclaredClasses())
                .filter(c -> c.getSimpleName().equals("Handler"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Handler not found in " + endpointClass));
        return Arrays.stream(handlerClass.getDeclaredMethods())
                .filter(m -> Modifier.isStatic(m.getModifiers()))
                .filter(m -> Arrays.asList(names).contains(m.getName()))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException(Arrays.toString(names) + " method not found in " + handlerClass));
    }
}
