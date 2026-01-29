package community.flock.wirespec.integration.spring.java.generated.endpoint;

import community.flock.wirespec.java.Wirespec;

import community.flock.wirespec.integration.spring.java.generated.model.UploadFileRequestBody;
import community.flock.wirespec.integration.spring.java.generated.model.ApiResponse;

public interface UploadFile extends Wirespec.Endpoint {
  public record Path(
    Long petId
  ) implements Wirespec.Path {}

  public record Queries(
    java.util.Optional<String> additionalMetadata
  ) implements Wirespec.Queries {}

  static class RequestHeaders implements Wirespec.Request.Headers {}

  record Request (
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

  sealed interface Response<T> extends Wirespec.Response<T> {}
  sealed interface Response2XX<T> extends Response<T> {}
  sealed interface ResponseApiResponse extends Response<ApiResponse> {}

  record Response200(
    int status,
    Headers headers,
    ApiResponse body
  ) implements Response2XX<ApiResponse>, ResponseApiResponse {
    public Response200(ApiResponse body) {
      this(200, new Headers(), body);
    }
    static class Headers implements Wirespec.Response.Headers {}
  }
        
  static interface Adapter extends Wirespec.Adapter<Request, Response<?>>{
    public static String pathTemplate = "/pet/{petId}/uploadImage";
    public static String method = "POST";
  static Wirespec.RawRequest toRawRequest(Wirespec.Serializer serialization, Request request) {
    return new Wirespec.RawRequest(
      request.method().name(),
      java.util.List.of("pet", serialization.serializePath(request.path().petId(), Wirespec.getType(Long.class, null)), "uploadImage"),
      java.util.Map.ofEntries(java.util.Map.entry("additionalMetadata", serialization.serializeParam(request.queries().additionalMetadata(), Wirespec.getType(String.class, java.util.Optional.class)))),
      java.util.Collections.emptyMap(),
      serialization.serializeBody(request.body(), Wirespec.getType(UploadFileRequestBody.class, null))
    );
  }

  static Request fromRawRequest(Wirespec.Deserializer serialization, Wirespec.RawRequest request) {
    return new Request(
        serialization.deserializePath(request.path().get(1), Wirespec.getType(Long.class, null)),
        serialization.deserializeParam(request.queries().getOrDefault("additionalMetadata", java.util.Collections.emptyList()), Wirespec.getType(String.class, java.util.Optional.class)),
        serialization.deserializeBody(request.body(), Wirespec.getType(UploadFileRequestBody.class, null))
      );
  }

  static Wirespec.RawResponse toRawResponse(Wirespec.Serializer serialization, Response<?> response) {
      if (response instanceof Response200 r) { return new Wirespec.RawResponse(r.status(), java.util.Collections.emptyMap(), serialization.serializeBody(r.body, Wirespec.getType(ApiResponse.class, null))); }
    else { throw new IllegalStateException("Cannot match response with status: " + response.status());}
  }

  static Response<?> fromRawResponse(Wirespec.Deserializer serialization, Wirespec.RawResponse response) {
    switch (response.statusCode()) {
        case 200: return new Response200(
        serialization.deserializeBody(response.body(), Wirespec.getType(ApiResponse.class, null))
      );
      default: throw new IllegalStateException("Cannot match response with status: " + response.statusCode());
    }
  }
}

  interface Handler extends Wirespec.Handler {
    @org.springframework.web.bind.annotation.PostMapping("/pet/{petId}/uploadImage")
    java.util.concurrent.CompletableFuture<Response<?>> uploadFile(Request request);

  }
}
