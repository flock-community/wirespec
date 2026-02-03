package community.flock.wirespec.integration.jvm.java;

import community.flock.wirespec.java.Wirespec;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class HttpClientTransportationTest {

    @Test
    void testTransport() throws Exception {
        HttpClient mockClient = mock(HttpClient.class);
        HttpResponse<byte[]> mockResponse = mock(HttpResponse.class);

        String body = "{\"hello\":\"world\"}";
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(body.getBytes());
        when(mockResponse.headers()).thenReturn(java.net.http.HttpHeaders.of(Collections.emptyMap(), (s1, s2) -> true));

        when(mockClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        HttpClientTransportation transportation = new HttpClientTransportation("https://67.com", mockClient);
        Wirespec.RawRequest request = new Wirespec.RawRequest(
                "POST",
                List.of("path", "to", "resource"),
                Map.of("query", List.of("v1")),
                Map.of("Content-Type", List.of("application/json")),
                body.getBytes()
        );

        Wirespec.RawResponse response = transportation.transport(request).get();

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        org.mockito.Mockito.verify(mockClient).sendAsync(requestCaptor.capture(), any());

        HttpRequest capturedRequest = requestCaptor.getValue();
        assertEquals("https://67.com/path/to/resource?query=v1", capturedRequest.uri().toString());
        assertEquals("POST", capturedRequest.method());
        assertEquals("application/json", capturedRequest.headers().firstValue("Content-Type").orElse(null));

        assertEquals(200, response.statusCode());
        assertArrayEquals(body.getBytes(), response.body());
    }

    @Test
    void testTransportTrailingSlash() throws Exception {
        HttpClient mockClient = mock(HttpClient.class);
        HttpResponse<byte[]> mockResponse = mock(HttpResponse.class);

        String body = "{\"hello\":\"world\"}";
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(body.getBytes());
        when(mockResponse.headers()).thenReturn(java.net.http.HttpHeaders.of(Collections.emptyMap(), (s1, s2) -> true));

        when(mockClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        HttpClientTransportation transportation = new HttpClientTransportation("https://67.com/", mockClient);
        Wirespec.RawRequest request = new Wirespec.RawRequest(
                "GET",
                List.of("path", "to", "resource"),
                Collections.emptyMap(),
                Collections.emptyMap(),
                null
        );

        transportation.transport(request).get();

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        org.mockito.Mockito.verify(mockClient).sendAsync(requestCaptor.capture(), any());

        HttpRequest capturedRequest = requestCaptor.getValue();
        assertEquals("https://67.com/path/to/resource", capturedRequest.uri().toString());
    }

    @Test
    void testTransportStaticMock() throws Exception {
        try (MockedStatic<HttpClient> httpClientMockedStatic = mockStatic(HttpClient.class)) {
            HttpClient.Builder mockBuilder = mock(HttpClient.Builder.class);
            HttpClient mockClient = mock(HttpClient.class);
            HttpResponse<byte[]> mockResponse = mock(HttpResponse.class);

            httpClientMockedStatic.when(HttpClient::newBuilder).thenReturn(mockBuilder);
            when(mockBuilder.build()).thenReturn(mockClient);

            String body = "{\"hello\":\"world\"}";
            when(mockResponse.statusCode()).thenReturn(200);
            when(mockResponse.body()).thenReturn(body.getBytes());
            when(mockResponse.headers()).thenReturn(java.net.http.HttpHeaders.of(Collections.emptyMap(), (s1, s2) -> true));

            when(mockClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(CompletableFuture.completedFuture(mockResponse));

            HttpClientTransportation transportation = new HttpClientTransportation("https://67.com");
            Wirespec.RawRequest request = new Wirespec.RawRequest(
                    "POST",
                    List.of("path", "to", "resource"),
                    Map.of("query", List.of("v1")),
                    Map.of("Content-Type", List.of("application/json")),
                    body.getBytes()
            );

            Wirespec.RawResponse response = transportation.transport(request).get();

            assertEquals(200, response.statusCode());
            assertArrayEquals(body.getBytes(), response.body());
        }
    }
}
