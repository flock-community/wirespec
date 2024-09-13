package community.flock.wirespec.compiler.core.emit.shared

import community.flock.wirespec.compiler.core.emit.common.DEFAULT_SHARED_PACKAGE_STRING
import community.flock.wirespec.compiler.core.emit.common.Spacer

data object JavaShared : Shared {
    override val source = """
        |package $DEFAULT_SHARED_PACKAGE_STRING.java;
        |
        |import java.lang.reflect.Type;
        |import java.util.List;
        |import java.util.Map;
        |
        |public interface Wirespec {
        |${Spacer}interface Enum {}
        |${Spacer}interface Endpoint {}
        |${Spacer}interface Refined { String getValue(); }
        |${Spacer}interface Path {}
        |${Spacer}interface Queries {}
        |${Spacer}interface Headers {}
        |${Spacer}interface Handler {}
        |${Spacer}interface ServerEdge<Req extends Request<?>, Res extends Response<?>> {
        |${Spacer(2)}Req from(RawRequest request);
        |${Spacer(2)}RawResponse to(Res response);
        |${Spacer}}
        |${Spacer}interface ClientEdge<Req extends Request<?>, Res extends Response<?>> {
        |${Spacer(2)}RawRequest to(Req request);
        |${Spacer(2)}Res from(RawResponse response);
        |${Spacer}}
        |${Spacer}interface Client<Req extends Request<?>, Res extends Response<?>> {
        |${Spacer(2)}String getPathTemplate();
        |${Spacer(2)}String getMethod();
        |${Spacer(2)}ClientEdge<Req, Res> getClient(Serialization<String> serialization);
        |${Spacer}}
        |${Spacer}interface Server<Req extends Request<?>, Res extends Response<?>> {
        |${Spacer(2)}String getPathTemplate();
        |${Spacer(2)}String getMethod();
        |${Spacer(2)}ServerEdge<Req, Res> server(Serialization<String> serialization);
        |${Spacer}}
        |${Spacer}enum Method { GET, PUT, POST, DELETE, OPTIONS, HEAD, PATCH, TRACE }
        |${Spacer}interface Request<T> { Path getPath(); Method getMethod(); Queries getQueries(); Headers getHeaders(); T getBody(); interface Headers extends Wirespec.Headers {} }
        |${Spacer}interface Response<T> { int getStatus(); Headers getHeaders(); T getBody(); interface Headers extends Wirespec.Headers {} }
        |${Spacer}interface Serialization<RAW> extends Serializer<RAW>, Deserializer<RAW> {}
        |${Spacer}interface Serializer<RAW> { <T> RAW serialize(T t, Type type); }
        |${Spacer}interface Deserializer<RAW> { <T> T deserialize(RAW raw, Type type); }
        |${Spacer}record RawRequest(String method, List<String> path, Map<String, List<String>> queries, Map<String, List<String>> headers, String body) {} 
        |${Spacer}record RawResponse(int statusCode, Map<String, List<String>> headers, String body) {}
        |}
        |
    """.trimMargin()
}
