package community.flock.wirespec.integration.spring.java.generated.endpoint;

import community.flock.wirespec.java.Wirespec;

import community.flock.wirespec.integration.spring.java.generated.model.User;

public interface CreateUsersWithListInput extends Wirespec.Endpoint {
  static class Path implements Wirespec.Path {}

  static class Queries implements Wirespec.Queries {}

  static class RequestHeaders implements Wirespec.Request.Headers {}

  record Request (
    Path path,
    Wirespec.Method method,
    Queries queries,
    RequestHeaders headers,
    java.util.List<User> body
  ) implements Wirespec.Request<java.util.List<User>> {
    public Request(java.util.List<User> body) {
      this(new Path(), Wirespec.Method.POST, new Queries(), new RequestHeaders(), body);
    }
  }

  sealed interface Response<T> extends Wirespec.Response<T> {}
  sealed interface Response2XX<T> extends Response<T> {}
  sealed interface ResponsedXX<T> extends Response<T> {}
  sealed interface ResponseUser extends Response<User> {}
  sealed interface ResponseVoid extends Response<Void> {}

  record Response200(
    int status,
    Headers headers,
    User body
  ) implements Response2XX<User>, ResponseUser {
    public Response200(User body) {
      this(200, new Headers(), body);
    }
    static class Headers implements Wirespec.Response.Headers {}
  }
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
        
  static interface Adapter extends Wirespec.Adapter<Request, Response<?>>{
    public static String pathTemplate = "/user/createWithList";
    public static String method = "POST";
  static Wirespec.RawRequest toRawRequest(Wirespec.Serializer serialization, Request request) {
    return new Wirespec.RawRequest(
      request.method().name(),
      java.util.List.of("user", "createWithList"),
      java.util.Collections.emptyMap(),
      java.util.Collections.emptyMap(),
      serialization.serializeBody(request.body(), Wirespec.getType(User.class, java.util.List.class))
    );
  }

  static Request fromRawRequest(Wirespec.Deserializer serialization, Wirespec.RawRequest request) {
    return new Request(
        serialization.deserializeBody(request.body(), Wirespec.getType(User.class, java.util.List.class))
      );
  }

  static Wirespec.RawResponse toRawResponse(Wirespec.Serializer serialization, Response<?> response) {
      if (response instanceof Response200 r) { return new Wirespec.RawResponse(r.status(), java.util.Collections.emptyMap(), serialization.serializeBody(r.body, Wirespec.getType(User.class, null))); }
      if (response instanceof ResponseDefault r) { return new Wirespec.RawResponse(r.status(), java.util.Collections.emptyMap(), null); }
    else { throw new IllegalStateException("Cannot match response with status: " + response.status());}
  }

  static Response<?> fromRawResponse(Wirespec.Deserializer serialization, Wirespec.RawResponse response) {
    switch (response.statusCode()) {
        case 200: return new Response200(
        serialization.deserializeBody(response.body(), Wirespec.getType(User.class, null))
      );
      default: throw new IllegalStateException("Cannot match response with status: " + response.statusCode());
    }
  }
}

  interface Handler extends Wirespec.Handler {
    @org.springframework.web.bind.annotation.PostMapping("/user/createWithList")
    java.util.concurrent.CompletableFuture<Response<?>> createUsersWithListInput(Request request);

  }
}
