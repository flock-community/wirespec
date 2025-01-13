package community.flock.wirespec.example.maven.custom.app.common;

import community.flock.wirespec.java.Wirespec.RawRequest;
import community.flock.wirespec.java.Wirespec.RawResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static java.net.URI.create;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.springframework.http.HttpMethod.valueOf;
import static org.springframework.util.CollectionUtils.toMultiValueMap;

@Component
public class WirespecTransporter {

    private final RestTemplate restTemplate;

    public WirespecTransporter(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public CompletableFuture<RawResponse> transport(RawRequest request) {
        final var headers = new HttpHeaders();
        headers.putAll(request.headers());

        final var req = RequestEntity
            .method(valueOf(request.method()), create(request.path().stream().reduce((acc, cur) -> acc + "/" + cur).orElse("")))
            .headers(headers)
            .body(request.body());

        final var res = restTemplate.exchange(req, String.class);
        return completedFuture(new RawResponse(
                res.getStatusCode().value(),
                res.getHeaders().toSingleValueMap(),
                res.getBody()
        ));
    }
}
