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

    static Wirespec.RawRequest toRequest(Wirespec.Serializer<String> serialization, Request request) {
      var queries = java.util.Collections.<String,java.util.List<String>>emptyMap();
      var headers = new java.util.HashMap<String, java.util.List<String>>() {{
        request.headers.api_key.ifPresent(it -> put("api_key", serialization.serializeParam(it, Wirespec.getType(String.class, false))));
      }};
      
      return new Wirespec.RawRequest(
        request.method.name(),
        java.util.List.of("pet", serialization.serialize(request.path.petId, Wirespec.getType(Long.class, false))),
        queries,
        headers,
        serialization.serialize(request.getBody(), Wirespec.getType(Void.class, false))
      );
    }

    static Request fromRequest(Wirespec.Deserializer<String> serialization, Wirespec.RawRequest request) {
      return new Request(
        serialization.<Long>deserialize(request.path().get(1), Wirespec.getType(Long.class, false)),
        java.util.Optional.ofNullable(request.headers().get("api_key")).map(it -> serialization.<String>deserializeParam(it, Wirespec.getType(String.class, false)))
      );
    }

    static Wirespec.RawResponse toResponse(Wirespec.Serializer<String> serialization, Response<?> response) {
      if (response instanceof Response400 r) { return new Wirespec.RawResponse(r.getStatus(), java.util.Collections.emptyMap(), null); }
      else { throw new IllegalStateException("Cannot match response with status: " + response.getStatus());}
    }

    static Response<?> fromResponse(Wirespec.Deserializer<String> serialization, Wirespec.RawResponse response) {
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
      @Override public Wirespec.ServerEdge<Request, Response<?>> getServer(Wirespec.Serialization<String> serialization) {
        return new Wirespec.ServerEdge<>() {
          @Override public Request from(Wirespec.RawRequest request) { return fromRequest(serialization, request); }
          @Override public Wirespec.RawResponse to(Response<?> response) { return toResponse(serialization, response); }
        };
      }
      @Override public Wirespec.ClientEdge<Request, Response<?>> getClient(Wirespec.Serialization<String> serialization) {
        return new Wirespec.ClientEdge<>() {
          @Override public Wirespec.RawRequest to(Request request) { return toRequest(serialization, request); }
          @Override public Response<?> from(Wirespec.RawResponse response) { return fromResponse(serialization, response); }
        };
      }
    }
  }
}
