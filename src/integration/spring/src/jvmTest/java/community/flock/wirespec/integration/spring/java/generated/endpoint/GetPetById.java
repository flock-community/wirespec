package community.flock.wirespec.integration.spring.java.generated.endpoint;
import community.flock.wirespec.java.Wirespec;
import community.flock.wirespec.integration.spring.java.generated.model.Pet;
public interface GetPetById extends Wirespec.Endpoint {
  public static record Path (
    Long petId
  ) implements Wirespec.Path {
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
    Void body
  ) implements Wirespec.Request<Void> {
    public Request(Long petId) {
      this(new Path(petId), Wirespec.Method.GET, new Queries(), new RequestHeaders(), null);
    }
  };
  public sealed interface Response<T> extends Wirespec.Response<T> permits Response2XX, Response4XX, ResponsePet, ResponseUnit {}
  public sealed interface Response2XX<T> extends Response<T> permits Response200 {}
  public sealed interface Response4XX<T> extends Response<T> permits Response400, Response404 {}
  public sealed interface ResponsePet extends Response<Pet> permits Response200 {}
  public sealed interface ResponseUnit extends Response<Void> permits Response400, Response404 {}
  public static record Response200Headers () implements Wirespec.Response.Headers {
  };
  public static record Response200 (
    Integer status,
    Response200Headers headers,
    Pet body
  ) implements Response2XX<Pet>, ResponsePet {
    public Response200(Pet body) {
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
  public static record Response404Headers () implements Wirespec.Response.Headers {
  };
  public static record Response404 (
    Integer status,
    Response404Headers headers,
    Void body
  ) implements Response4XX<Void>, ResponseUnit {
    public Response404() {
      this(404, new Response404Headers(), null);
    }
  };
  public static Wirespec.RawRequest toRawRequest(Wirespec.Serializer serialization, Request request) {
    return new Wirespec.RawRequest(
      request.method().name(),
      java.util.List.of("pet", serialization.<Long>serializePath(request.path().petId(), Wirespec.getType(Long.class, null))),
      java.util.Collections.emptyMap(),
      java.util.Collections.emptyMap(),
      java.util.Optional.empty()
    );
  }
  public static Request fromRawRequest(Wirespec.Deserializer serialization, Wirespec.RawRequest request) {
    return new Request(serialization.<Long>deserializePath(request.path().get(1), Wirespec.getType(Long.class, null)));
  }
  public static Wirespec.RawResponse toRawResponse(Wirespec.Serializer serialization, Response<?> response) {
    if (response instanceof Response200 r) {
      return new Wirespec.RawResponse(
        r.status(),
        java.util.Collections.emptyMap(),
        java.util.Optional.of(serialization.serializeBody(r.body(), Wirespec.getType(Pet.class, null)))
      );
    } else if (response instanceof Response400 r) {
      return new Wirespec.RawResponse(
        r.status(),
        java.util.Collections.emptyMap(),
        java.util.Optional.empty()
      );
    } else if (response instanceof Response404 r) {
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
          return new Response200(response.body().map(it -> serialization.<Pet>deserializeBody(it, Wirespec.getType(Pet.class, null))).orElseThrow(() -> new IllegalStateException("body is null")));
        }
        case 400 -> {
          return new Response400();
        }
        case 404 -> {
          return new Response404();
        }
        default -> {
          throw new IllegalStateException(("Cannot match response with status: " + response.statusCode()));
        }
    }
  }
  public interface Handler extends Wirespec.Handler {
    @org.springframework.web.bind.annotation.GetMapping("/pet/{petId}")  public java.util.concurrent.CompletableFuture<Response<?>> getPetById(Request request);
    public static record Handlers () implements Wirespec.Server<Request, Response<?>>, Wirespec.Client<Request, Response<?>> {
      @Override
      public String getPathTemplate() {
        return "/pet/{petId}";
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
    public java.util.concurrent.CompletableFuture<Response<?>> getPetById(Long petId);
  }
  Handler.Handlers api = new Handler.Handlers();
}
