package community.flock.wirespec.compiler.core

import community.flock.wirespec.compiler.core.emit.JavaClassEmitter
import community.flock.wirespec.compiler.core.fixture.ClassModelFixture
import kotlin.test.Test
import kotlin.test.assertEquals

class JavaClassEmitterTest {

    @Test
    fun testJavaEmitter() {

        val expected = """
            |public interface AddPetEndpoint extends Wirespec.Endpoint {
            |  static String PATH = "/pet";
            |  static String METHOD = "POST";
            |
            |  sealed interface Request<T> extends Wirespec.Request<T> {
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
            |      this.query = java.util.Map.of();
            |      this.headers = java.util.Map.of();
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
            |  static <B, Req extends Request<?>> Function<Wirespec.Request<B>, Req> REQUEST_MAPPER(Wirespec.ContentMapper<B> contentMapper) {
            |    return request -> {
            |      if (request.getContent().type() == "application/json") {
            |        Wirespec.Content<Pet> content = contentMapper.read(request.getContent(), Wirespec.getType(Pet.class, false));
            |        return (Req) new RequestApplicationJson(request.getPath(), request.getMethod(), request.getQuery(), request.getHeaders(), content);
            |      }
            |      if (request.getContent().type() == "application/xml") {
            |        Wirespec.Content<Pet> content = contentMapper.read(request.getContent(), Wirespec.getType(Pet.class, false));
            |        return (Req) new RequestApplicationXml(request.getPath(), request.getMethod(), request.getQuery(), request.getHeaders(), content);
            |      }
            |      if (request.getContent().type() == "application/x-www-form-urlencoded") {
            |        Wirespec.Content<Pet> content = contentMapper.read(request.getContent(), Wirespec.getType(Pet.class, false));
            |        return (Req) new RequestApplicationXWwwFormUrlencoded(request.getPath(), request.getMethod(), request.getQuery(), request.getHeaders(), content);
            |      }
            |      throw new IllegalStateException("Unknown response type");
            |    }
            |  }
            |}
        """.trimMargin()

        val emitter = JavaClassEmitter()
        val res = emitter.emit(ClassModelFixture.endpointRequest).values.first()
        println(res)

        assertEquals(expected, res)
    }
}