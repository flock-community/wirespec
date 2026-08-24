package community.flock.wirespec.integration.spring.java.generated.endpoint;
import community.flock.wirespec.java.Wirespec;
import community.flock.wirespec.integration.spring.java.generated.model.Pet;
public interface FindPetsByTags extends Wirespec.Endpoint {
  public static record Path () implements Wirespec.Path {
  };
  public static record Queries (
    java.util.Optional<java.util.List<String>> tags
  ) implements Wirespec.Queries {
  };
  public static record RequestHeaders () implements Wirespec.Request.Headers {
  };
  public static record Request (
    Path path,
    Wirespec.Method method,
    Queries queries,
    RequestHeaders headers,
    Void body
  ) implements Wirespec.Request<Void> {
    public Request(java.util.Optional<java.util.List<String>> tags) {
      this(new Path(), Wirespec.Method.GET, new Queries(tags), new RequestHeaders(), null);
    }
  };
  public sealed interface Response<T> extends Wirespec.Response<T> permits Response2XX, Response4XX, ResponseListPet, ResponseUnit {}
  public sealed interface Response2XX<T> extends Response<T> permits Response200 {}
  public sealed interface Response4XX<T> extends Response<T> permits Response400 {}
  public sealed interface ResponseListPet extends Response<java.util.List<Pet>> permits Response200 {}
  public sealed interface ResponseUnit extends Response<Void> permits Response400 {}
  public static record Response200Headers () implements Wirespec.Response.Headers {
  };
  public static record Response200 (
    Integer status,
    Response200Headers headers,
    java.util.List<Pet> body
  ) implements Response2XX<java.util.List<Pet>>, ResponseListPet {
    public Response200(java.util.List<Pet> body) {
      this(200, new Response200Headers(), body);
    }
  };
  public static record Response400Headers () implements Wirespec.Response.Headers {
  };
  public static record Response400 (
    Integer status,
    Response400Headers headers,
    Void body
  ) implements Response4XX<Void>, ResponseUnit {
    public Response400() {
      this(400, new Response400Headers(), null);
    }
  };
  public static Wirespec.RawRequest toRawRequest(Wirespec.Serializer serialization, Request request) {
    return new Wirespec.RawRequest(
      request.method().name(),
      java.util.List.of("pet", "findByTags"),
      java.util.Map.ofEntries(java.util.Map.entry("tags", request.queries().tags().map(it -> serialization.<java.util.List<String>>serializeParam(it, Wirespec.getType(String.class, java.util.List.class))).orElse(java.util.List.<String>of()))),
      java.util.Collections.emptyMap(),
      java.util.Optional.empty()
    );
  }
  public static Request fromRawRequest(Wirespec.Deserializer serialization, Wirespec.RawRequest request) {
    return new Request(java.util.Optional.ofNullable(request.queries().get("tags")).map(it -> serialization.<java.util.List<String>>deserializeParam(it, Wirespec.getType(String.class, java.util.List.class))));
  }
  public static Wirespec.RawResponse toRawResponse(Wirespec.Serializer serialization, Response<?> response) {
    if (response instanceof Response200 r) {
      return new Wirespec.RawResponse(
        r.status(),
        java.util.Collections.emptyMap(),
        java.util.Optional.of(serialization.serializeBody(r.body(), Wirespec.getType(Pet.class, java.util.List.class)))
      );
    } else if (response instanceof Response400 r) {
      return new Wirespec.RawResponse(
        r.status(),
        java.util.Collections.emptyMap(),
        java.util.Optional.empty()
      );
    } else {
      throw new IllegalStateException(("Cannot match response with status: " + response.status()));
    }
  }
  public static Response<?> fromRawResponse(Wirespec.Deserializer serialization, Wirespec.RawResponse response) {
    switch (response.statusCode()) {
        case 200 -> {
          return new Response200(response.body().map(it -> serialization.<java.util.List<Pet>>deserializeBody(it, Wirespec.getType(Pet.class, java.util.List.class))).orElseThrow(() -> new IllegalStateException("body is null")));
        }
        case 400 -> {
          return new Response400();
        }
        default -> {
          throw new IllegalStateException(("Cannot match response with status: " + response.statusCode()));
        }
    }
  }
  public interface Handler extends Wirespec.Handler {
    @org.springframework.web.bind.annotation.GetMapping("/pet/findByTags")  public java.util.concurrent.CompletableFuture<Response<?>> findPetsByTags(Request request);
    public static record Handlers () implements Wirespec.Server<Request, Response<?>>, Wirespec.Client<Request, Response<?>> {
      @Override
      public String getPathTemplate() {
        return "/pet/findByTags";
      }
      @Override
      public String getMethod() {
        return "GET";
      }
      @Override
      public Wirespec.ServerEdge<Request, Response<?>> getServer(Wirespec.Serialization serialization) {
        return new Wirespec.ServerEdge<>() {
        @Override public Request from(Wirespec.RawRequest request) {
            return fromRawRequest(serialization, request);
        }
        @Override public Wirespec.RawResponse to(Response<?> response) {
            return toRawResponse(serialization, response);
        }
        };
      }
      @Override
      public Wirespec.ClientEdge<Request, Response<?>> getClient(Wirespec.Serialization serialization) {
        return new Wirespec.ClientEdge<>() {
        @Override public Wirespec.RawRequest to(Request request) {
            return toRawRequest(serialization, request);
        }
        @Override public Response<?> from(Wirespec.RawResponse response) {
            return fromRawResponse(serialization, response);
        }
        };
      }
    };
  }
  public interface Call extends Wirespec.Call {
    public java.util.concurrent.CompletableFuture<Response<?>> findPetsByTags(java.util.Optional<java.util.List<String>> tags);
  }
  Handler.Handlers api = new Handler.Handlers();
}
