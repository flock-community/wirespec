package community.flock.wirespec.integration.spring.java.generated.endpoint;

import community.flock.wirespec.java.Wirespec;

import community.flock.wirespec.integration.spring.java.generated.model.UploadFileRequestBody;
import community.flock.wirespec.integration.spring.java.generated.model.ApiResponse;

public interface UploadFile extends Wirespec.Endpoint {
  public static record Path (
    Long petId
  ) implements Wirespec.Path {
  }
  public static record Queries (
    java.util.Optional<String> additionalMetadata
  ) implements Wirespec.Queries {
  }
  public static record RequestHeaders () implements Wirespec.Request.Headers {
  }
  public static record Request (
    Path path,
    Wirespec.Method method,
    Queries queries,
    RequestHeaders headers,
    UploadFileRequestBody body
  ) implements Wirespec.Request<UploadFileRequestBody> {
    public Request(Long petId, java.util.Optional<String> additionalMetadata, UploadFileRequestBody body) {
      this(new Path(petId), Wirespec.Method.POST, new Queries(additionalMetadata), new RequestHeaders(), body);
    }
  }
  public sealed interface Response<T> extends Wirespec.Response<T> {
  }
  public sealed interface Response2XX<T> extends Response<T> {
  }
  public sealed interface ResponseApiResponse extends Response<ApiResponse> {
  }
  public static record Response200 (
    Integer status,
    Headers headers,
    ApiResponse body
  ) implements Response2XX<ApiResponse>, ResponseApiResponse {
    public Response200(ApiResponse body) {
      this(200, new Headers(), body);
    }
    public static record Headers () implements Wirespec.Response.Headers {
    }
  }
  static public Wirespec.RawRequest toRawRequest(Wirespec.Serializer serialization, Request request) {
    return new Wirespec.RawRequest(
      request.method().name(),
      java.util.List.of("pet", serialization.serializePath(request.path().petId(), Wirespec.getType(Long.class, null)), "uploadImage"),
      java.util.Map.ofEntries(java.util.Map.entry("additionalMetadata", serialization.serializeParam(request.queries().additionalMetadata(), Wirespec.getType(String.class, java.util.Optional.class)))),
      java.util.Collections.emptyMap(),
      serialization.serializeBody(request.body(), Wirespec.getType(UploadFileRequestBody.class, null))
    );
  }
  static public Request fromRawRequest(Wirespec.Deserializer serialization, Wirespec.RawRequest request) {
    return new Request(
      serialization.deserializePath(request.path().get(1), Wirespec.getType(Long.class, null)),
      serialization.deserializeParam(request.queries().getOrDefault("additionalMetadata", java.util.Collections.emptyList()), Wirespec.getType(String.class, java.util.Optional.class)),
      serialization.deserializeBody(request.body(), Wirespec.getType(UploadFileRequestBody.class, null))
    );
  }
  static public Wirespec.RawResponse toRawResponse(Wirespec.Serializer serialization, Response<?> response) {
    if (response instanceof Response200 r) {
      return new Wirespec.RawResponse(
        r.status(),
        java.util.Collections.emptyMap(),
        serialization.serializeBody(r.body(), Wirespec.getType(ApiResponse.class, null))
      );
    } else {
      throw new IllegalStateException(("Cannot match response with status: " + response.status()));
    }
  }
  static public Response<?> fromRawResponse(Wirespec.Deserializer serialization, Wirespec.RawResponse response) {
    switch (response.statusCode()) {
        case 200 -> {
          return new Response200(serialization.deserializeBody(response.body(), Wirespec.getType(ApiResponse.class, null)));
        }
        default -> {
          throw new IllegalStateException(("Cannot match response with status: " + response.statusCode()));
        }
    }
  }
  public interface Handler extends Wirespec.Handler {
    @org.springframework.web.bind.annotation.PostMapping("/pet/{petId}/uploadImage")
        public java.util.concurrent.CompletableFuture<Response<?>> uploadFile(Request request);
    public static record Handlers () implements Wirespec.Server<Request, Response<?>>, Wirespec.Client<Request, Response<?>> {
      @Override
      public String getPathTemplate() {
        return "/pet/{petId}/uploadImage";
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
        }};
      }
      @Override
      public Wirespec.ClientEdge<Request, Response<?>> getClient(Wirespec.Serialization serialization) {
        return new Wirespec.ClientEdge<>() {
        @Override public Wirespec.RawRequest to(Request request) {
          return toRawRequest(serialization, request);
        }
        @Override public Response<?> from(Wirespec.RawResponse response) {
          return fromRawResponse(serialization, response);
        }};
      }
    }
  }
}