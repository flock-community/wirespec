package community.flock.wirespec.integration.wiremock.java;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.UrlPattern;
import community.flock.wirespec.java.Wirespec;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Build a WireMock stub for a Wirespec endpoint, with a typed response.
 *
 * <pre>
 *     wireMockServer.stubFor(
 *         WirespecWireMock.stubFor(new GetTodos.Handler.Handlers(), response, serialization)
 *     );
 * </pre>
 *
 * The returned {@link MappingBuilder} matches the endpoint's HTTP method and path
 * template (path parameters are matched as any non-slash segment) and replies with
 * the response serialized through the supplied {@link Wirespec.Serialization}.
 */
public final class WirespecWireMock {

    private static final Pattern PATH_PARAM_PATTERN = Pattern.compile("\\{[^/}]+}");

    private WirespecWireMock() {}

    public static <Req extends Wirespec.Request<?>, Res extends Wirespec.Response<?>> MappingBuilder stubFor(
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
