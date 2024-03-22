package community.flock.wirespec.compiler.core.emit

import community.flock.wirespec.compiler.core.fixture.ClassModelFixture
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class JavaEmitterTest {

    private val emitter = JavaEmitter()

    @Test
    fun testEmitterType() {
        val expected = """
            |public record Todo(
            |  String name,
            |  java.util.Optional<String> description,
            |  java.util.List<String> notes,
            |  Boolean done
            |){
            |};
        """.trimMargin()

        val res = emitter.emit(ClassModelFixture.type)
        res shouldBe expected
    }

    @Test
    fun testEmitterRefined() {
        val expected = """
            |public record UUID (String value) implements Wirespec.Refined {
            |  public static boolean validate(UUID record) {
            |    return java.util.regex.Pattern.compile(^[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12}${'$'}).matcher(record.value).find();
            |  }
            |  @Override
            |  public String getValue() { return value; }
            |}
        """.trimMargin()

        val res = emitter.emit(ClassModelFixture.refined)
        res shouldBe expected
    }

    @Test
    fun testEmitterEnum() {
        val expected = """
            |public enum TodoStatus implements Wirespec.Enum {
            |  OPEN("OPEN"),
            |  IN_PROGRESS("IN_PROGRESS"),
            |  CLOSE("CLOSE");
            |  public final String label;
            |  TodoStatus(String label) {
            |    this.label = label;
            |  }
            |  @Override
            |  public String toString() {
            |    return label;
            |  }
            |}
        """.trimMargin()

        val res = emitter.emit(ClassModelFixture.enum)
        res shouldBe expected
    }

    @Test
    fun testEmitterEndpoint() {

        val expected = """
            |public interface AddPetEndpoint extends Wirespec.Endpoint {
            |  static String PATH = "/pet";
            |  static String METHOD = "POST";
            |
            |  sealed interface Request<T> extends Wirespec.Request<T> {
            |  }
            |
            |  final class RequestApplicationXml implements Request<Pet> {
            |    private final String path;
            |    private final Wirespec.Method method;
            |    private final java.util.Map<String, java.util.List<Object>> query;
            |    private final java.util.Map<String, java.util.List<Object>> headers;
            |    private final Wirespec.Content<Pet> content;
            |
            |    public RequestApplicationXml(
            |      String path,
            |      Wirespec.Method method,
            |      java.util.Map<String, java.util.List<Object>> query,
            |      java.util.Map<String, java.util.List<Object>> headers,
            |      Wirespec.Content<Pet> content
            |    ) {
            |      this.path = path;
            |      this.method = method;
            |      this.query = query;
            |      this.headers = headers;
            |      this.content = content;
            |    }
            |
            |    public RequestApplicationXml(
            |      Pet body
            |    ) {
            |      this.path = "/" + "pet";
            |      this.method = Wirespec.Method.POST;
            |      this.query = java.util.Map.ofEntries();
            |      this.headers = java.util.Map.ofEntries();
            |      this.content = new Wirespec.Content("application/xml", body);
            |    }
            |
            |    @Override
            |    public String getPath() {
            |      return path;
            |    }
            |
            |    @Override
            |    public Wirespec.Method getMethod() {
            |      return method;
            |    }
            |
            |    @Override
            |    public java.util.Map<String, java.util.List<Object>> getQuery() {
            |      return query;
            |    }
            |
            |    @Override
            |    public java.util.Map<String, java.util.List<Object>> getHeaders() {
            |      return headers;
            |    }
            |
            |    @Override
            |    public Wirespec.Content<Pet> getContent() {
            |      return content;
            |    }
            |  }
            |
            |  final class RequestApplicationJson implements Request<Pet> {
            |    private final String path;
            |    private final Wirespec.Method method;
            |    private final java.util.Map<String, java.util.List<Object>> query;
            |    private final java.util.Map<String, java.util.List<Object>> headers;
            |    private final Wirespec.Content<Pet> content;
            |
            |    public RequestApplicationJson(
            |      String path,
            |      Wirespec.Method method,
            |      java.util.Map<String, java.util.List<Object>> query,
            |      java.util.Map<String, java.util.List<Object>> headers,
            |      Wirespec.Content<Pet> content
            |    ) {
            |      this.path = path;
            |      this.method = method;
            |      this.query = query;
            |      this.headers = headers;
            |      this.content = content;
            |    }
            |
            |    public RequestApplicationJson(
            |      Pet body
            |    ) {
            |      this.path = "/" + "pet";
            |      this.method = Wirespec.Method.POST;
            |      this.query = java.util.Map.ofEntries();
            |      this.headers = java.util.Map.ofEntries();
            |      this.content = new Wirespec.Content("application/json", body);
            |    }
            |
            |    @Override
            |    public String getPath() {
            |      return path;
            |    }
            |
            |    @Override
            |    public Wirespec.Method getMethod() {
            |      return method;
            |    }
            |
            |    @Override
            |    public java.util.Map<String, java.util.List<Object>> getQuery() {
            |      return query;
            |    }
            |
            |    @Override
            |    public java.util.Map<String, java.util.List<Object>> getHeaders() {
            |      return headers;
            |    }
            |
            |    @Override
            |    public Wirespec.Content<Pet> getContent() {
            |      return content;
            |    }
            |  }
            |
            |  final class RequestApplicationXWwwFormUrlencoded implements Request<Pet> {
            |    private final String path;
            |    private final Wirespec.Method method;
            |    private final java.util.Map<String, java.util.List<Object>> query;
            |    private final java.util.Map<String, java.util.List<Object>> headers;
            |    private final Wirespec.Content<Pet> content;
            |
            |    public RequestApplicationXWwwFormUrlencoded(
            |      String path,
            |      Wirespec.Method method,
            |      java.util.Map<String, java.util.List<Object>> query,
            |      java.util.Map<String, java.util.List<Object>> headers,
            |      Wirespec.Content<Pet> content
            |    ) {
            |      this.path = path;
            |      this.method = method;
            |      this.query = query;
            |      this.headers = headers;
            |      this.content = content;
            |    }
            |
            |    public RequestApplicationXWwwFormUrlencoded(
            |      Pet body
            |    ) {
            |      this.path = "/" + "pet";
            |      this.method = Wirespec.Method.POST;
            |      this.query = java.util.Map.ofEntries();
            |      this.headers = java.util.Map.ofEntries();
            |      this.content = new Wirespec.Content("application/x-www-form-urlencoded", body);
            |    }
            |
            |    @Override
            |    public String getPath() {
            |      return path;
            |    }
            |
            |    @Override
            |    public Wirespec.Method getMethod() {
            |      return method;
            |    }
            |
            |    @Override
            |    public java.util.Map<String, java.util.List<Object>> getQuery() {
            |      return query;
            |    }
            |
            |    @Override
            |    public java.util.Map<String, java.util.List<Object>> getHeaders() {
            |      return headers;
            |    }
            |
            |    @Override
            |    public Wirespec.Content<Pet> getContent() {
            |      return content;
            |    }
            |  }
            |
            |  sealed interface Response<T> extends Wirespec.Response<T> {
            |  };
            |
            |  sealed interface Response2XX<T> extends Response<T> {
            |  };
            |
            |  sealed interface Response4XX<T> extends Response<T> {
            |  };
            |
            |  sealed interface Response200<T> extends Response2XX<T> {
            |  };
            |
            |  sealed interface Response405<T> extends Response4XX<T> {
            |  };
            |
            |  final class Response200ApplicationXml implements Response200<Pet> {
            |    private final int status;
            |    private final java.util.Map<String, java.util.List<Object>> headers;
            |    private final Wirespec.Content<Pet> content;
            |
            |    public Response200ApplicationXml(int status, java.util.Map<String, java.util.List<Object>> headers, Wirespec.Content<Pet> content) {
            |      this.status = status;
            |      this.headers = headers;
            |      this.content = content;
            |    }
            |
            |    public Response200ApplicationXml(
            |
            |    ) {
            |      this.status = 200;
            |      this.headers = java.util.Map.ofEntries();
            |      this.content = null;
            |    }
            |
            |    @Override
            |    public int getStatus() {
            |      return status;
            |    }
            |
            |    @Override
            |    public java.util.Map<String, java.util.List<Object>> getHeaders() {
            |      return headers;
            |    }
            |
            |    @Override
            |    public Wirespec.Content<Pet> getContent() {
            |      return content;
            |    }
            |  }
            |
            |  final class Response200ApplicationJson implements Response200<Pet> {
            |    private final int status;
            |    private final java.util.Map<String, java.util.List<Object>> headers;
            |    private final Wirespec.Content<Pet> content;
            |
            |    public Response200ApplicationJson(int status, java.util.Map<String, java.util.List<Object>> headers, Wirespec.Content<Pet> content) {
            |      this.status = status;
            |      this.headers = headers;
            |      this.content = content;
            |    }
            |
            |    public Response200ApplicationJson(
            |
            |    ) {
            |      this.status = 200;
            |      this.headers = java.util.Map.ofEntries();
            |      this.content = null;
            |    }
            |
            |    @Override
            |    public int getStatus() {
            |      return status;
            |    }
            |
            |    @Override
            |    public java.util.Map<String, java.util.List<Object>> getHeaders() {
            |      return headers;
            |    }
            |
            |    @Override
            |    public Wirespec.Content<Pet> getContent() {
            |      return content;
            |    }
            |  }
            |
            |  final class Response405Unit implements Response405<Void> {
            |    private final int status;
            |    private final java.util.Map<String, java.util.List<Object>> headers;
            |    private final Wirespec.Content<Void> content;
            |
            |    public Response405Unit(int status, java.util.Map<String, java.util.List<Object>> headers, Wirespec.Content<Void> content) {
            |      this.status = status;
            |      this.headers = headers;
            |      this.content = content;
            |    }
            |
            |    public Response405Unit(
            |
            |    ) {
            |      this.status = 405;
            |      this.headers = java.util.Map.ofEntries();
            |      this.content = null;
            |    }
            |
            |    @Override
            |    public int getStatus() {
            |      return status;
            |    }
            |
            |    @Override
            |    public java.util.Map<String, java.util.List<Object>> getHeaders() {
            |      return headers;
            |    }
            |
            |    @Override
            |    public Wirespec.Content<Void> getContent() {
            |      return content;
            |    }
            |  }
            |
            |  static <B, Req extends Request<?>> Function<Wirespec.Request<B>, Req> REQUEST_MAPPER(Wirespec.ContentMapper<B> contentMapper) {
            |    return request -> {
            |      if (request.getContent().type().equals("application/json")) {
            |        Wirespec.Content<Pet> content = contentMapper.read(request.getContent(), Wirespec.getType(Pet.class, false));
            |        return (Req) new RequestApplicationJson(request.getPath(), request.getMethod(), request.getQuery(), request.getHeaders(), content);
            |      }
            |      if (request.getContent().type().equals("application/xml")) {
            |        Wirespec.Content<Pet> content = contentMapper.read(request.getContent(), Wirespec.getType(Pet.class, false));
            |        return (Req) new RequestApplicationXml(request.getPath(), request.getMethod(), request.getQuery(), request.getHeaders(), content);
            |      }
            |      if (request.getContent().type().equals("application/x-www-form-urlencoded")) {
            |        Wirespec.Content<Pet> content = contentMapper.read(request.getContent(), Wirespec.getType(Pet.class, false));
            |        return (Req) new RequestApplicationXWwwFormUrlencoded(request.getPath(), request.getMethod(), request.getQuery(), request.getHeaders(), content);
            |      }
            |      throw new IllegalStateException("Unknown response type");
            |    };
            |  }
            |  static <B, Res extends Response<?>> Function<Wirespec.Response<B>, Res> RESPONSE_MAPPER(Wirespec.ContentMapper<B> contentMapper) {
            |    return response -> {
            |      if (response.getStatus() == 200 && response.getContent().type().equals("application/xml")) {
            |        Wirespec.Content<Pet> content = contentMapper.read(response.getContent(), Wirespec.getType(Pet.class, false));
            |        return (Res) new Response200ApplicationXml(response.getStatus(), response.getHeaders(), content);
            |      }
            |      if (response.getStatus() == 200 && response.getContent().type().equals("application/json")) {
            |        Wirespec.Content<Pet> content = contentMapper.read(response.getContent(), Wirespec.getType(Pet.class, false));
            |        return (Res) new Response200ApplicationJson(response.getStatus(), response.getHeaders(), content);
            |      }
            |      if (response.getStatus() == 405 && response.getContent() == null) {
            |        return (Res) new Response405Unit(response.getStatus(), response.getHeaders(), null);
            |      }
            |      throw new IllegalStateException("Unknown response type");
            |    };
            |  }
            |
            |  public CompletableFuture<Response<?>> addPet(Request<?> request);
            |
            |}
        """.trimMargin()

        val res = emitter.emit(ClassModelFixture.endpoint)
        res shouldBe expected
    }
}
