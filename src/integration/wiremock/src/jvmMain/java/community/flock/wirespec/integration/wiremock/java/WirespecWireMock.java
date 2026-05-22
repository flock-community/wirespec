package community.flock.wirespec.integration.wiremock.java;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.UrlPattern;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import community.flock.wirespec.java.Wirespec;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Register a WireMock stub for a Wirespec endpoint, with a typed response.
 *
 * <pre>
 *     WirespecWireMock.stubFor(server, new GetTodos.Handler.Handlers(), response, serialization);
 * </pre>
 *
 * The endpoint's method and path template drive the WireMock request matcher (path
 * parameters match any non-slash segment), and the response is serialized through
 * the supplied {@link Wirespec.Serialization} into the stub's body, status, and headers.
 */
public final class WirespecWireMock {

    private static final Pattern PATH_PARAM_PATTERN = Pattern.compile("\\{[^/}]+}");

    private WirespecWireMock() {}

    public static <Req extends Wirespec.Request<?>, Res extends Wirespec.Response<?>> StubMapping stubFor(
            WireMockServer server,
            Wirespec.Server<Req, Res> endpoint,
            Res response,
            Wirespec.Serialization serialization
    ) {
        return server.stubFor(mappingBuilder(endpoint, response, serialization));
    }

    static <Req extends Wirespec.Request<?>, Res extends Wirespec.Response<?>> MappingBuilder mappingBuilder(
            Wirespec.Server<Req, Res> endpoint,
            Res response,
            Wirespec.Serialization serialization
    ) {
        Wirespec.RawResponse rawResponse = endpoint.getServer(serialization).to(response);
        UrlPattern urlPattern = urlPatternFor(endpoint.getPathTemplate());
        return requestBuilder(endpoint.getMethod(), urlPattern)
                .willReturn(responseBuilder(rawResponse));
    }

    private static MappingBuilder requestBuilder(String method, UrlPattern urlPattern) {
        return switch (method.toUpperCase()) {
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
