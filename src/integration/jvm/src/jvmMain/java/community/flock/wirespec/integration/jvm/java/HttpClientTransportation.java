package community.flock.wirespec.integration.jvm.java;

import community.flock.wirespec.java.Wirespec;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class HttpClientTransportation implements Wirespec.Transportation {

    private final String baseUrl;

    private final HttpClient client;

    public HttpClientTransportation(String baseUrl) {
        this(baseUrl, HttpClient.newBuilder().build());
    }

    public HttpClientTransportation(String baseUrl, HttpClient client) {
        this.baseUrl = baseUrl;
        this.client = client;
    }

    @Override
    public CompletableFuture<Wirespec.RawResponse> transport(Wirespec.RawRequest request) {

        String pathString = request.path().stream()
                .filter(it -> !it.isBlank())
                .collect(Collectors.joining("/"));

        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String uriString = base + "/" + pathString;

        if (!request.queries().isEmpty()) {
            String queryString = request.queries().entrySet().stream()
                    .flatMap(entry -> entry.getValue().stream()
                            .map(v -> entry.getKey() + "=" + URLEncoder.encode(v, StandardCharsets.UTF_8)))
                    .collect(Collectors.joining("&"));
            uriString += "?" + queryString;
        }
        URI uri = URI.create(uriString);

        String[] headers = request.headers().entrySet().stream()
                .flatMap(entry -> entry.getValue().stream()
                        .filter(value -> !value.isBlank())
                        .flatMap(value -> List.of(entry.getKey(), value).stream()))
                .toArray(String[]::new);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(uri)
                .method(
                        request.method().toUpperCase(),
                        request.body() != null
                                ? HttpRequest.BodyPublishers.ofByteArray(request.body())
                                : HttpRequest.BodyPublishers.noBody()
                );

        if (headers.length > 0) {
            requestBuilder.headers(headers);
        }

        return client.sendAsync(requestBuilder.build(), HttpResponse.BodyHandlers.ofByteArray())
                .thenApply(response -> new Wirespec.RawResponse(
                        response.statusCode(),
                        response.headers().map(),
                        response.body()
                ));
    }
}
