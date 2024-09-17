package community.flock.wirespec.examples.app.common;

import community.flock.wirespec.java.Wirespec;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.CompletableFuture.completedFuture;

@Component
public class WirespecHandler {

    private final RestTemplate restTemplate;

    public WirespecHandler(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public CompletableFuture<Wirespec.RawResponse> handle(Wirespec.RawRequest request) {
        var req = new RequestEntity<>(
                request.body(),
                CollectionUtils.toMultiValueMap(request.headers()),
                HttpMethod.valueOf(request.method()),
                URI.create(request.path().stream().reduce((acc, cur) -> acc + "/" + cur).orElse(""))
        );
        var res = restTemplate.exchange(req, String.class);
        return completedFuture(new Wirespec.RawResponse(
                res.getStatusCode().value(),
                res.getHeaders(),
                res.getBody()
        ));
    }
}
