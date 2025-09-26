package community.flock.wirespec.integration.spring.java.generated.endpoint;

import community.flock.wirespec.java.Wirespec;

import community.flock.wirespec.integration.spring.java.generated.model.ApiResponse;

public interface UploadFile extends Wirespec.Endpoint {
  public record Path(
    Long petId
  ) implements Wirespec.Path {}

  public record Queries(
    java.util.Optional<String> additionalMetadata
  ) implements Wirespec.Queries {}

  class RequestHeaders implements Wirespec.Request.Headers {}

  class Request implements Wirespec.Request<String> {
    private final Path path;
    private final Wirespec.Method method;
    private final Queries queries;
    private final RequestHeaders headers;
    private final String body;
    public Request(Long petId, java.util.Optional<String> additionalMetadata, String body) {
      this.path = new Path(petId);
      this.method = Wirespec.Method.POST;
      this.queries = new Queries(additionalMetadata);
      this.headers = new RequestHeaders();
      this.body = body;
    }
    @Override public Path getPath() { return path; }
    @Override public Wirespec.Method getMethod() { return method; }
    @Override public Queries getQueries() { return queries; }
    @Override public RequestHeaders getHeaders() { return headers; }
    @Override public String getBody() { return body; }
  }

  sealed interface Response<T> extends Wirespec.Response<T> {}
  sealed interface Response2XX<T> extends Response<T> {}
  sealed interface ResponseApiResponse extends Response<ApiResponse> {}

  record Response200(ApiResponse body) implements Response2XX<ApiResponse>, ResponseApiResponse {
    @Override public int getStatus() { return 200; }
    @Override public Headers getHeaders() { return new Headers(); }
    @Override public ApiResponse getBody() { return body; }
    class Headers implements Wirespec.Response.Headers {}
  }

  interface Handler extends Wirespec.Handler {

    static Wirespec.RawRequest toRequest(Wirespec.Serializer serialization, Request request) {
      return new Wirespec.RawRequest(
        request.method.name(),
        java.util.List.of("pet", serialization.serializePath(request.path.petId, Wirespec.getType(Long.class, null)), "uploadImage"),
        java.util.Map.ofEntries(java.util.Map.entry("additionalMetadata", serialization.serializeParam(request.queries.additionalMetadata, Wirespec.getType(String.class, java.util.Optional.class)))),
        java.util.Collections.emptyMap(),
        serialization.serializeBody(request.getBody(), Wirespec.getType(String.class, null))
      );
    }

    static Request fromRequest(Wirespec.Deserializer serialization, Wirespec.RawRequest request) {
      return new Request(
        serialization.deserializePath(request.path().get(1), Wirespec.getType(Long.class, null)),
        serialization.deserializeParam(request.queries().getOrDefault("additionalMetadata", java.util.Collections.emptyList()), Wirespec.getType(String.class, java.util.Optional.class)),
        serialization.deserializeBody(request.body(), Wirespec.getType(String.class, null))
      );
    }

    static Wirespec.RawResponse toResponse(Wirespec.Serializer serialization, Response<?> response) {
      if (response instanceof Response200 r) { return new Wirespec.RawResponse(r.getStatus(), java.util.Collections.emptyMap(), serialization.serializeBody(r.body, Wirespec.getType(ApiResponse.class, null))); }
      else { throw new IllegalStateException("Cannot match response with status: " + response.getStatus());}
    }

    static Response<?> fromResponse(Wirespec.Deserializer serialization, Wirespec.RawResponse response) {
      switch (response.statusCode()) {
        case 200: return new Response200(
        serialization.deserializeBody(response.body(), Wirespec.getType(ApiResponse.class, null))
      );
        default: throw new IllegalStateException("Cannot match response with status: " + response.statusCode());
      }
    }

    @org.springframework.web.bind.annotation.PostMapping("/pet/{petId}/uploadImage")
    java.util.concurrent.CompletableFuture<Response<?>> uploadFile(Request request);

    class Handlers implements Wirespec.Server<Request, Response<?>>, Wirespec.Client<Request, Response<?>> {
      @Override public String getPathTemplate() { return "/pet/{petId}/uploadImage"; }
      @Override public String getMethod() { return "POST"; }
      @Override public Wirespec.ServerEdge<Request, Response<?>> getServer(Wirespec.Serialization serialization) {
        return new Wirespec.ServerEdge<>() {
          @Override public Request from(Wirespec.RawRequest request) { return fromRequest(serialization, request); }
          @Override public Wirespec.RawResponse to(Response<?> response) { return toResponse(serialization, response); }
        };
      }
      @Override public Wirespec.ClientEdge<Request, Response<?>> getClient(Wirespec.Serialization serialization) {
        return new Wirespec.ClientEdge<>() {
          @Override public Wirespec.RawRequest to(Request request) { return toRequest(serialization, request); }
          @Override public Response<?> from(Wirespec.RawResponse response) { return fromResponse(serialization, response); }
        };
      }
    }
  }
}
