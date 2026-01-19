package community.flock.wirespec.integration.spring.java.generated.endpoint;

import community.flock.wirespec.java.Wirespec;

import community.flock.wirespec.integration.spring.java.generated.model.User;

public interface CreateUsersWithListInput extends Wirespec.Endpoint {
  class Path implements Wirespec.Path {}

  class Queries implements Wirespec.Queries {}

  class RequestHeaders implements Wirespec.Request.Headers {}

  class Request implements Wirespec.Request<java.util.List<User>> {
    private final Path path;
    private final Wirespec.Method method;
    private final Queries queries;
    private final RequestHeaders headers;
    private final java.util.List<User> body;
    public Request(java.util.List<User> body) {
      this.path = new Path();
      this.method = Wirespec.Method.POST;
      this.queries = new Queries();
      this.headers = new RequestHeaders();
      this.body = body;
    }
    @Override public Path getPath() { return path; }
    @Override public Wirespec.Method getMethod() { return method; }
    @Override public Queries getQueries() { return queries; }
    @Override public RequestHeaders getHeaders() { return headers; }
    @Override public java.util.List<User> getBody() { return body; }
  }

  sealed interface Response<T> extends Wirespec.Response<T> {}
  sealed interface Response2XX<T> extends Response<T> {}
  sealed interface ResponsedXX<T> extends Response<T> {}
  sealed interface ResponseUser extends Response<User> {}
  sealed interface ResponseVoid extends Response<Void> {}

  record Response200(User body) implements Response2XX<User>, ResponseUser {
    @Override public int getStatus() { return 200; }
    @Override public Headers getHeaders() { return new Headers(); }
    @Override public User getBody() { return body; }
    class Headers implements Wirespec.Response.Headers {}
  }
  record ResponseDefault() implements ResponsedXX<Void>, ResponseVoid {
    @Override public int getStatus() { return 200; }
    @Override public Headers getHeaders() { return new Headers(); }
    @Override public Void getBody() { return null; }
    class Headers implements Wirespec.Response.Headers {}
  }

  interface Handler extends Wirespec.Handler {

    static Wirespec.RawRequest toRequest(Wirespec.Serializer serialization, Request request) {
      return new Wirespec.RawRequest(
        request.method.name(),
        java.util.List.of("user", "createWithList"),
        java.util.Collections.emptyMap(),
        java.util.Collections.emptyMap(),
        serialization.serializeBody(request.getBody(), Wirespec.getType(User.class, java.util.List.class))
      );
    }

    static Request fromRequest(Wirespec.Deserializer serialization, Wirespec.RawRequest request) {
      return new Request(
        serialization.deserializeBody(request.body(), Wirespec.getType(User.class, java.util.List.class))
      );
    }

    static Wirespec.RawResponse toResponse(Wirespec.Serializer serialization, Response<?> response) {
      if (response instanceof Response200 r) { return new Wirespec.RawResponse(r.getStatus(), java.util.Collections.emptyMap(), serialization.serializeBody(r.body, Wirespec.getType(User.class, null))); }
      if (response instanceof ResponseDefault r) { return new Wirespec.RawResponse(r.getStatus(), java.util.Collections.emptyMap(), null); }
      else { throw new IllegalStateException("Cannot match response with status: " + response.getStatus());}
    }

    static Response<?> fromResponse(Wirespec.Deserializer serialization, Wirespec.RawResponse response) {
      switch (response.statusCode()) {
        case 200: return new Response200(
        serialization.deserializeBody(response.body(), Wirespec.getType(User.class, null))
      );
        default: throw new IllegalStateException("Cannot match response with status: " + response.statusCode());
      }
    }

    @org.springframework.web.bind.annotation.PostMapping("/user/createWithList")
    java.util.concurrent.CompletableFuture<Response<?>> createUsersWithListInput(Request request);

    class Handlers implements Wirespec.Server<Request, Response<?>>, Wirespec.Client<Request, Response<?>> {
      @Override public String getPathTemplate() { return "/user/createWithList"; }
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
