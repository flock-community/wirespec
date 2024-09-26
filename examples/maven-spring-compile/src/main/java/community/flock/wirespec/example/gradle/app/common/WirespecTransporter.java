package community.flock.wirespec.example.gradle.app.common;

import community.flock.wirespec.java.Wirespec.RawRequest;
import community.flock.wirespec.java.Wirespec.RawResponse;
import org.springframework.http.RequestEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.CompletableFuture;

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
        var req = new RequestEntity<>(
                request.body(),
                toMultiValueMap(request.headers()),
                valueOf(request.method()),
                create(request.path().stream().reduce((acc, cur) -> acc + "/" + cur).orElse(""))
        );
        var res = restTemplate.exchange(req, String.class);
        return completedFuture(new RawResponse(
                res.getStatusCode().value(),
                res.getHeaders(),
                res.getBody()
        ));
    }
}
