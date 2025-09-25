package community.flock.wirespec.integration.spring.java.generated.endpoint;

import community.flock.wirespec.java.Wirespec;



public interface DeletePet extends Wirespec.Endpoint {
  public record Path(
    Long petId
  ) implements Wirespec.Path {}

  class Queries implements Wirespec.Queries {}

  public record RequestHeaders(
    java.util.Optional<String> api_key
  ) implements Wirespec.Request.Headers {}

  class Request implements Wirespec.Request<Void> {
    private final Path path;
    private final Wirespec.Method method;
    private final Queries queries;
    private final RequestHeaders headers;
    private final Void body;
    public Request(Long petId, java.util.Optional<String> api_key) {
      this.path = new Path(petId);
      this.method = Wirespec.Method.DELETE;
      this.queries = new Queries();
      this.headers = new RequestHeaders(api_key);
      this.body = null;
    }
    @Override public Path getPath() { return path; }
    @Override public Wirespec.Method getMethod() { return method; }
    @Override public Queries getQueries() { return queries; }
    @Override public RequestHeaders getHeaders() { return headers; }
    @Override public Void getBody() { return body; }
  }

  sealed interface Response<T> extends Wirespec.Response<T> {}
  sealed interface Response4XX<T> extends Response<T> {}
  sealed interface ResponseVoid extends Response<Void> {}

  record Response400() implements Response4XX<Void>, ResponseVoid {
    @Override public int getStatus() { return 400; }
    @Override public Headers getHeaders() { return new Headers(); }
    @Override public Void getBody() { return null; }
    class Headers implements Wirespec.Response.Headers {}
  }

  interface Handler extends Wirespec.Handler {

    static Wirespec.RawRequest toRequest(Wirespec.Serializer serialization, Request request) {
      return new Wirespec.RawRequest(
        request.method.name(),
        java.util.List.of("pet", serialization.serializePath(request.path.petId, Wirespec.getType(Long.class, null))),
        java.util.Collections.emptyMap(),
        java.util.Map.ofEntries(java.util.Map.entry("api_key", serialization.serializeParam(request.headers.api_key, Wirespec.getType(String.class, java.util.Optional.class)))),
        null
      );
    }

    static Request fromRequest(Wirespec.Deserializer serialization, Wirespec.RawRequest request) {
      return new Request(
        serialization.deserializePath(request.path().get(1), Wirespec.getType(Long.class, null)),
        serialization.deserializeParam(request.headers().getOrDefault("api_key", java.util.Collections.emptyList()), Wirespec.getType(String.class, java.util.Optional.class))
      );
    }

    static Wirespec.RawResponse toResponse(Wirespec.Serializer serialization, Response<?> response) {
      if (response instanceof Response400 r) { return new Wirespec.RawResponse(r.getStatus(), java.util.Collections.emptyMap(), null); }
      else { throw new IllegalStateException("Cannot match response with status: " + response.getStatus());}
    }

    static Response<?> fromResponse(Wirespec.Deserializer serialization, Wirespec.RawResponse response) {
      switch (response.statusCode()) {
        case 400: return new Response400();
        default: throw new IllegalStateException("Cannot match response with status: " + response.statusCode());
      }
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/pet/{petId}")
    java.util.concurrent.CompletableFuture<Response<?>> deletePet(Request request);

    class Handlers implements Wirespec.Server<Request, Response<?>>, Wirespec.Client<Request, Response<?>> {
      @Override public String getPathTemplate() { return "/pet/{petId}"; }
      @Override public String getMethod() { return "DELETE"; }
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
