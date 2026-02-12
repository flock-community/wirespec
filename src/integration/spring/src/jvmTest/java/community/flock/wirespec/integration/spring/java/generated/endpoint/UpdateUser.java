package community.flock.wirespec.integration.spring.java.generated.endpoint;

import community.flock.wirespec.integration.spring.java.generated.model.User;
import community.flock.wirespec.java.Wirespec;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;



public interface UpdateUser extends Wirespec.Endpoint {
  public record Path(
    String username
  ) implements Wirespec.Path {}

  static class Queries implements Wirespec.Queries {}

  static class RequestHeaders implements Wirespec.Request.Headers {}

  record Request (
    Path path,
    Wirespec.Method method,
    Queries queries,
    RequestHeaders headers,
    User body
  ) implements Wirespec.Request<User> {
    public Request(String username, User body) {
      this(new Path(username), Wirespec.Method.PUT, new Queries(), new RequestHeaders(), body);
    }
  }

  sealed interface Response<T> extends Wirespec.Response<T> {}
  sealed interface ResponsedXX<T> extends Response<T> {}
  sealed interface ResponseVoid extends Response<Void> {}

  record ResponseDefault(
    int status,
    Headers headers,
    Void body
  ) implements ResponsedXX<Void>, ResponseVoid {
    public ResponseDefault() {
      this(200, new Headers(), null);
    }
    static class Headers implements Wirespec.Response.Headers {}
  }

  interface Handler extends Wirespec.Handler {

    static Wirespec.RawRequest toRequest(Wirespec.Serializer serialization, Request request) {
      return new Wirespec.RawRequest(
        request.method().name(),
        List.of("user", serialization.serializePath(request.path().username(), Wirespec.getType(String.class, null))),
        Collections.emptyMap(),
        Collections.emptyMap(),
        Optional.ofNullable(serialization.serializeBody(request.body(), Wirespec.getType(User.class, null)))
      );
    }

    static Request fromRequest(Wirespec.Deserializer serialization, Wirespec.RawRequest request) {
      return new Request(
        serialization.deserializePath(request.path().get(1), Wirespec.getType(String.class, null)),
        request.body().<User>map(body -> serialization.deserializeBody(body, Wirespec.getType(User.class, null))).orElse(null)
      );
    }

    static Wirespec.RawResponse toResponse(Wirespec.Serializer serialization, Response<?> response) {
      if (response instanceof ResponseDefault r) { return new Wirespec.RawResponse(r.status(), Collections.emptyMap(), Optional.empty()); }
      else { throw new IllegalStateException("Cannot match response with status: " + response.status());}
    }

    static Response<?> fromResponse(Wirespec.Deserializer serialization, Wirespec.RawResponse response) {
      throw new IllegalStateException("Cannot match response with status: " + response.statusCode());
    }

    @org.springframework.web.bind.annotation.PutMapping("/user/{username}")
    CompletableFuture<Response<?>> updateUser(Request request);

    class Handlers implements Wirespec.Server<Request, Response<?>>, Wirespec.Client<Request, Response<?>> {
      @Override public String getPathTemplate() { return "/user/{username}"; }
      @Override public String getMethod() { return "PUT"; }
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
