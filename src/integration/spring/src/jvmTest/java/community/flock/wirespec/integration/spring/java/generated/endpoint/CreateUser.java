package community.flock.wirespec.integration.spring.java.generated.endpoint;

import community.flock.wirespec.java.Wirespec;

import community.flock.wirespec.integration.spring.java.generated.model.User;

public interface CreateUser extends Wirespec.Endpoint {
  static class Path implements Wirespec.Path {}

  static class Queries implements Wirespec.Queries {}

  static class RequestHeaders implements Wirespec.Request.Headers {}

  record Request (
    Path path,
    Wirespec.Method method,
    Queries queries,
    RequestHeaders headers,
    User body
  ) implements Wirespec.Request<User> {
    public Request(User body) {
      this(new Path(), Wirespec.Method.POST, new Queries(), new RequestHeaders(), body);
    }
  }

  sealed interface Response<T> extends Wirespec.Response<T> {}
  sealed interface ResponsedXX<T> extends Response<T> {}
  sealed interface ResponseUser extends Response<User> {}

  record ResponseDefault(
    int status,
    Headers headers,
    User body
  ) implements ResponsedXX<User>, ResponseUser {
    public ResponseDefault(User body) {
      this(200, new Headers(), body);
    }
    static class Headers implements Wirespec.Response.Headers {}
  }
        
  static interface Adapter extends Wirespec.Adapter<Request, Response<?>>{
    public static String pathTemplate = "/user";
    public static String method = "POST";
  static Wirespec.RawRequest toRawRequest(Wirespec.Serializer serialization, Request request) {
    return new Wirespec.RawRequest(
      request.method().name(),
      java.util.List.of("user"),
      java.util.Collections.emptyMap(),
      java.util.Collections.emptyMap(),
      serialization.serializeBody(request.body(), Wirespec.getType(User.class, null))
    );
  }

  static Request fromRawRequest(Wirespec.Deserializer serialization, Wirespec.RawRequest request) {
    return new Request(
        serialization.deserializeBody(request.body(), Wirespec.getType(User.class, null))
      );
  }

  static Wirespec.RawResponse toRawResponse(Wirespec.Serializer serialization, Response<?> response) {
      if (response instanceof ResponseDefault r) { return new Wirespec.RawResponse(r.status(), java.util.Collections.emptyMap(), serialization.serializeBody(r.body, Wirespec.getType(User.class, null))); }
    else { throw new IllegalStateException("Cannot match response with status: " + response.status());}
  }

  static Response<?> fromRawResponse(Wirespec.Deserializer serialization, Wirespec.RawResponse response) {
    switch (response.statusCode()) {

      default: throw new IllegalStateException("Cannot match response with status: " + response.statusCode());
    }
  }
}

  interface Handler extends Wirespec.Handler {
    @org.springframework.web.bind.annotation.PostMapping("/user")
    java.util.concurrent.CompletableFuture<Response<?>> createUser(Request request);

  }
}
