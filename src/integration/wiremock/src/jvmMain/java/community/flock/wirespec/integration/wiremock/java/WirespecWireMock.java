package community.flock.wirespec.integration.wiremock.java;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.UrlPattern;
import community.flock.wirespec.integration.jackson.java.WirespecSerialization;
import community.flock.wirespec.java.Wirespec;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Start building a WireMock stub for a Wirespec endpoint. Mirrors WireMock's own
 * {@code get(urlEqualTo(...))} / {@code post(urlEqualTo(...))} factories — the returned
 * builder then accepts a typed Wirespec {@link Wirespec.Response} via
 * {@link WirespecMappingBuilder#willReturn(Wirespec.Response)}.
 *
 * <p>Two calling styles:
 *
 * <pre>
 *     import static community.flock.wirespec.integration.wiremock.java.WirespecWireMock.wirespec;
 *
 *     // Pass an explicit Server instance:
 *     server.stubFor(wirespec(new GetTodos.Handler.Handlers())
 *             .willReturn(new GetTodos.Response200(todos)));
 *
 *     // Or pass the endpoint class and let the integration find the Server reflectively
 *     // (the Java equivalent of Kotlin's reified wirespec&lt;GetTodos&gt;()):
 *     server.stubFor(wirespec(GetTodos.class)
 *             .willReturn(new GetTodos.Response200(todos)));
 * </pre>
 *
 * The endpoint's HTTP method and path template drive the WireMock request matcher
 * (path parameters match any non-slash segment).
 */
public final class WirespecWireMock {

    private static final Pattern PATH_PARAM_PATTERN = Pattern.compile("\\{[^/}]+}");
    private static final Wirespec.Serialization DEFAULT_SERIALIZATION = new WirespecSerialization(new ObjectMapper());

    private WirespecWireMock() {}

    public static WirespecMappingBuilder wirespec(Wirespec.Server<?, ?> endpoint) {
        return new WirespecMappingBuilder(endpoint, requestBuilder(endpoint));
    }

    /**
     * Reflection-based overload — {@code wirespec(GetTodos.class)} locates the
     * {@link Wirespec.Server} implementation generated inside the endpoint class.
     */
    public static <E extends Wirespec.Endpoint> WirespecMappingBuilder wirespec(Class<E> endpointClass) {
        Wirespec.Server<?, ?> server = findServer(endpointClass);
        if (server == null) {
            throw new IllegalArgumentException(
                    "No Wirespec.Server implementation found inside " + endpointClass.getName()
                            + ". Expected the generated <Endpoint>.Handler structure.");
        }
        return wirespec(server);
    }

    public static final class WirespecMappingBuilder {
        private final Wirespec.Server<?, ?> endpoint;
        private final MappingBuilder mapping;

        WirespecMappingBuilder(Wirespec.Server<?, ?> endpoint, MappingBuilder mapping) {
            this.endpoint = endpoint;
            this.mapping = mapping;
        }

        /**
         * Serialize {@code response} through a Jackson-backed {@link Wirespec.Serialization}
         * and attach it as this stub's response. Returns the underlying {@link MappingBuilder}
         * so callers can keep chaining WireMock methods (e.g. {@code .atPriority(...)},
         * {@code .inScenario(...)}).
         */
        public MappingBuilder willReturn(Wirespec.Response<?> response) {
            return willReturn(response, DEFAULT_SERIALIZATION);
        }

        /**
         * Serialize {@code response} through {@code serialization} and attach it as this stub's response.
         * Pass your own {@link Wirespec.Serialization} to customize the ObjectMapper or swap in a
         * different serializer.
         */
        @SuppressWarnings({"unchecked", "rawtypes"})
        public MappingBuilder willReturn(Wirespec.Response<?> response, Wirespec.Serialization serialization) {
            Wirespec.Server typed = endpoint;
            return mapping.willReturn(responseBuilder(typed.getServer(serialization).to(response)));
        }
    }

    private static Wirespec.Server<?, ?> findServer(Class<?> klass) {
        if (Wirespec.Server.class.isAssignableFrom(klass) && !klass.isInterface()) {
            // Kotlin companion objects expose a static INSTANCE field; Java-emitted Handlers
            // are records with a public no-arg constructor.
            try {
                Field instanceField = klass.getDeclaredField("INSTANCE");
                instanceField.setAccessible(true);
                return (Wirespec.Server<?, ?>) instanceField.get(null);
            } catch (ReflectiveOperationException ignored) {
                // fall through
            }
            try {
                Constructor<?> ctor = klass.getDeclaredConstructor();
                ctor.setAccessible(true);
                return (Wirespec.Server<?, ?>) ctor.newInstance();
            } catch (ReflectiveOperationException ignored) {
                // fall through
            }
        }
        for (Class<?> nested : klass.getDeclaredClasses()) {
            Wirespec.Server<?, ?> found = findServer(nested);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static MappingBuilder requestBuilder(Wirespec.Server<?, ?> endpoint) {
        UrlPattern urlPattern = urlPatternFor(endpoint.getPathTemplate());
        return switch (endpoint.getMethod().toUpperCase()) {
            case "GET" -> WireMock.get(urlPattern);
            case "PUT" -> WireMock.put(urlPattern);
            case "POST" -> WireMock.post(urlPattern);
            case "DELETE" -> WireMock.delete(urlPattern);
            case "PATCH" -> WireMock.patch(urlPattern);
            case "HEAD" -> WireMock.head(urlPattern);
            case "OPTIONS" -> WireMock.options(urlPattern);
            case "TRACE" -> WireMock.trace(urlPattern);
            default -> WireMock.any(urlPattern);
        };
    }

    private static ResponseDefinitionBuilder responseBuilder(Wirespec.RawResponse rawResponse) {
        ResponseDefinitionBuilder builder = WireMock.aResponse().withStatus(rawResponse.statusCode());
        for (Map.Entry<String, java.util.List<String>> entry : rawResponse.headers().entrySet()) {
            for (String value : entry.getValue()) {
                builder.withHeader(entry.getKey(), value);
            }
        }
        rawResponse.body().ifPresent(builder::withBody);
        return builder;
    }

    static UrlPattern urlPatternFor(String pathTemplate) {
        if (!PATH_PARAM_PATTERN.matcher(pathTemplate).find()) {
            return WireMock.urlPathEqualTo(pathTemplate);
        }
        String[] parts = PATH_PARAM_PATTERN.split(pathTemplate, -1);
        StringBuilder regex = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            regex.append(Pattern.quote(parts[i]));
            if (i < parts.length - 1) {
                regex.append("[^/]+");
            }
        }
        return WireMock.urlPathMatching(regex.toString());
    }
}
