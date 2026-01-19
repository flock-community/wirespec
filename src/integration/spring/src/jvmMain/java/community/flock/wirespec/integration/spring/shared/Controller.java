package community.flock.wirespec.integration.spring.shared;

import jakarta.servlet.http.HttpServletRequest;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public interface Controller {

    static List<String> extractPath(HttpServletRequest request) {
        String path = request.getPathInfo();
        if (path == null) {
            path = request.getServletPath();
        }
        return Arrays.stream(path.split("/"))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    static Map<String, List<String>> extractQueries(HttpServletRequest request) {
        return extractQueries(request.getQueryString());
    }

    static Map<String, List<String>> extractQueries(String queryString) {
        if (queryString == null) {
            return Collections.emptyMap();
        }

        return Arrays.stream(queryString.split("&"))
                .flatMap(param -> {
                    String[] parts = param.split("=", 2);
                    if (parts.length < 2) {
                        throw new IllegalArgumentException("Invalid query parameter format: " + param);
                    }
                    String key = parts[0];
                    String value = parts[1];
                    String decodedKey = URLDecoder.decode(key, StandardCharsets.UTF_8);

                    return Arrays.stream(value.split(","))
                            .map(it -> Map.entry(decodedKey, URLDecoder.decode(it, StandardCharsets.UTF_8)));
                })
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())
                ));
    }
}
