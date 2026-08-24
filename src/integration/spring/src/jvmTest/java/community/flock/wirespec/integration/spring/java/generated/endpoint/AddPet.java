package community.flock.wirespec.integration.spring.java.generated.endpoint;
import community.flock.wirespec.java.Wirespec;
import community.flock.wirespec.integration.spring.java.generated.model.Pet;
public interface AddPet extends Wirespec.Endpoint {
  public static record Path () implements Wirespec.Path {
  };
  public static record Queries () implements Wirespec.Queries {
  };
  public static record RequestHeaders () implements Wirespec.Request.Headers {
  };
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
  };
  public sealed interface Response<T> extends Wirespec.Response<T> permits Response2XX, Response4XX, ResponsePet, ResponseUnit {}
  public sealed interface Response2XX<T> extends Response<T> permits Response200 {}
  public sealed interface Response4XX<T> extends Response<T> permits Response405 {}
  public sealed interface ResponsePet extends Response<Pet> permits Response200 {}
  public sealed interface ResponseUnit extends Response<Void> permits Response405 {}
  public static record Response200Headers (
    java.util.Optional<Integer> xRateLimit
  ) implements Wirespec.Response.Headers {
  };
  public static record Response200 (
    Integer status,
    Response200Headers headers,
    Pet body
  ) implements Response2XX<Pet>, ResponsePet {
    public Response200(java.util.Optional<Integer> xRateLimit, Pet body) {
      this(200, new Response200Headers(xRateLimit), body);
    }
  };
  public static record Response405Headers () implements Wirespec.Response.Headers {
  };
  public static record Response405 (
    Integer status,
    Response405Headers headers,
    Void body
  ) implements Response4XX<Void>, ResponseUnit {
    public Response405() {
      this(405, new Response405Headers(), null);
    }
  };
  public static Wirespec.RawRequest toRawRequest(Wirespec.Serializer serialization, Request request) {
    return new Wirespec.RawRequest(
      request.method().name(),
      java.util.List.of("pet"),
      java.util.Collections.emptyMap(),
      java.util.Collections.emptyMap(),
      java.util.Optional.of(serialization.<Pet>serializeBody(request.body(), Wirespec.getType(Pet.class, null)))
    );
  }
  public static Request fromRawRequest(Wirespec.Deserializer serialization, Wirespec.RawRequest request) {
    return new Request(request.body().map(it -> serialization.<Pet>deserializeBody(it, Wirespec.getType(Pet.class, null))).orElseThrow(() -> new IllegalStateException("body is null")));
  }
  public static Wirespec.RawResponse toRawResponse(Wirespec.Serializer serialization, Response<?> response) {
    if (response instanceof Response200 r) {
      return new Wirespec.RawResponse(
        r.status(),
        java.util.Map.ofEntries(java.util.Map.entry("X-Rate-Limit", r.headers().xRateLimit().map(it -> serialization.<Integer>serializeParam(it, Wirespec.getType(Integer.class, null))).orElse(java.util.List.<String>of()))),
        java.util.Optional.of(serialization.serializeBody(r.body(), Wirespec.getType(Pet.class, null)))
      );
    } else if (response instanceof Response405 r) {
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
          return new Response200(
            java.util.Optional.ofNullable(response.headers().entrySet().stream().filter(e -> e.getKey().equalsIgnoreCase("X-Rate-Limit")).findFirst().map(java.util.Map.Entry::getValue).orElse(null)).map(it -> serialization.<Integer>deserializeParam(it, Wirespec.getType(Integer.class, null))),
            response.body().map(it -> serialization.<Pet>deserializeBody(it, Wirespec.getType(Pet.class, null))).orElseThrow(() -> new IllegalStateException("body is null"))
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
  public interface Handler extends Wirespec.Handler {
    @org.springframework.web.bind.annotation.PostMapping("/pet")  public java.util.concurrent.CompletableFuture<Response<?>> addPet(Request request);
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
    public java.util.concurrent.CompletableFuture<Response<?>> addPet(Pet body);
  }
  Handler.Handlers api = new Handler.Handlers();
}
