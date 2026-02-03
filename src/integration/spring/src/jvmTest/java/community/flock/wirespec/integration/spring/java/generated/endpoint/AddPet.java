package community.flock.wirespec.integration.spring.java.generated.endpoint;

import community.flock.wirespec.java.Wirespec;

import community.flock.wirespec.integration.spring.java.generated.model.Pet;

public interface AddPet extends Wirespec.Endpoint {
  public static record Path () implements Wirespec.Path {
  }
  public static record Queries () implements Wirespec.Queries {
  }
  public static record RequestHeaders () implements Wirespec.Request.Headers {
  }
  public static record Request (
    Path path,
    Wirespec.Method method,
    Queries queries,
    RequestHeaders headers,
    Pet body
  ) implements Wirespec.Request<Pet> {
    public Request(Pet body) {
      this(new Path(), Wirespec.Method.POST, new Queries(), new RequestHeaders(), body);
    }
  }
  public sealed interface Response<T> extends Wirespec.Response<T> {
  }
  public sealed interface Response2XX<T> extends Response<T> {
  }
  public sealed interface Response4XX<T> extends Response<T> {
  }
  public sealed interface ResponsePet extends Response<Pet> {
  }
  public sealed interface ResponseVoid extends Response<Void> {
  }
  public static record Response200 (
    Integer status,
    Headers headers,
    Pet body
  ) implements Response2XX<Pet>, ResponsePet {
    public Response200(java.util.Optional<Integer> XRateLimit, Pet body) {
      this(200, new Headers(XRateLimit), body);
    }
    public static record Headers (
      java.util.Optional<Integer> XRateLimit
    ) implements Wirespec.Response.Headers {
    }
  }
  public static record Response405 (
    Integer status,
    Headers headers,
    Void body
  ) implements Response4XX<Void>, ResponseVoid {
    public Response405() {
      this(405, new Headers(), null);
    }
    public static record Headers () implements Wirespec.Response.Headers {
    }
  }
  public interface Handler extends Wirespec.Handler {
    static public Wirespec.RawRequest toRequest(Wirespec.Serializer serialization, Request request) {
      return new Wirespec.RawRequest(
        request.method().name(),
        java.util.List.of("pet"),
        java.util.Collections.emptyMap(),
        java.util.Collections.emptyMap(),
        serialization.serializeBody(request.body(), Wirespec.getType(Pet.class, null))
      );
    }
    static public Request fromRequest(Wirespec.Deserializer serialization, Wirespec.RawRequest request) {
      return new Request(serialization.deserializeBody(request.body(), Wirespec.getType(Pet.class, null)));
    }
    static public Wirespec.RawResponse toResponse(Wirespec.Serializer serialization, Response<?> response) {
      if (response instanceof Response200 r) {
        return new Wirespec.RawResponse(
          r.status(),
          java.util.Map.ofEntries(java.util.Map.entry("X-Rate-Limit", serialization.serializeParam(r.headers().XRateLimit(), Wirespec.getType(Integer.class, java.util.Optional.class)))),
          serialization.serializeBody(r.body(), Wirespec.getType(Pet.class, null))
        );
      } else if (response instanceof Response405 r) {
        return new Wirespec.RawResponse(
          r.status(),
          java.util.Collections.emptyMap(),
          null
        );
      } else {
        throw new IllegalStateException(("Cannot match response with status: " + response.status()));
      }
    }
    static public Response<?> fromResponse(Wirespec.Deserializer serialization, Wirespec.RawResponse response) {
      switch (response.statusCode()) {
          case 200 -> {
            return new Response200(
              serialization.deserializeParam(response.headers().getOrDefault("X-Rate-Limit", java.util.Collections.emptyList()), Wirespec.getType(Integer.class, java.util.Optional.class)),
              serialization.deserializeBody(response.body(), Wirespec.getType(Pet.class, null))
            );
          }
          case 405 -> {
            return new Response405();
          }
          default -> {
            throw new IllegalStateException(("Cannot match response with status: " + response.statusCode()));
          }
      }
    }
    @org.springframework.web.bind.annotation.PostMapping("/pet")
        public java.util.concurrent.CompletableFuture<Response<?>> addPet(Request request);
    public static record Handlers () implements Wirespec.Server<Request, Response<?>>, Wirespec.Client<Request, Response<?>> {
      @Override
      public String getPathTemplate() {
        return "/pet";
      }
      @Override
      public String getMethod() {
        return "POST";
      }
      @Override
      public Wirespec.ServerEdge<Request, Response<?>> getServer(Wirespec.Serialization serialization) {
        return new Wirespec.ServerEdge<>() {
        @Override public Request from(Wirespec.RawRequest request) {
          return fromRequest(serialization, request);
        }
        @Override public Wirespec.RawResponse to(Response<?> response) {
          return toResponse(serialization, response);
        }};
      }
      @Override
      public Wirespec.ClientEdge<Request, Response<?>> getClient(Wirespec.Serialization serialization) {
        return new Wirespec.ClientEdge<>() {
        @Override public Wirespec.RawRequest to(Request request) {
          return toRequest(serialization, request);
        }
        @Override public Response<?> from(Wirespec.RawResponse response) {
          return fromResponse(serialization, response);
        }};
      }
    }
  }
}