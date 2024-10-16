package community.flock.wirespec.compiler.core.emit.shared

import community.flock.wirespec.compiler.core.emit.common.DEFAULT_SHARED_PACKAGE_STRING
import community.flock.wirespec.compiler.core.emit.common.Spacer

data object JavaShared : Shared {
    override val packageString: String = "$DEFAULT_SHARED_PACKAGE_STRING.java"

    override val source = """
        |package $packageString;
        |
        |import java.lang.reflect.Type;
        |import java.lang.reflect.ParameterizedType;
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
        |${Spacer(2)}ServerEdge<Req, Res> getServer(Serialization<String> serialization);
        |${Spacer}}
        |${Spacer}enum Method { GET, PUT, POST, DELETE, OPTIONS, HEAD, PATCH, TRACE }
        |${Spacer}interface Request<T> { Path getPath(); Method getMethod(); Queries getQueries(); Headers getHeaders(); T getBody(); interface Headers extends Wirespec.Headers {} }
        |${Spacer}interface Response<T> { int getStatus(); Headers getHeaders(); T getBody(); interface Headers extends Wirespec.Headers {} }
        |${Spacer}interface Serialization<RAW> extends Serializer<RAW>, Deserializer<RAW> {}
        |${Spacer}interface Serializer<RAW> { <T> RAW serialize(T t, Type type); }
        |${Spacer}interface Deserializer<RAW> { <T> T deserialize(RAW raw, Type type); }
        |${Spacer}record RawRequest(String method, java.util.List<String> path, java.util.Map<String, String> queries, java.util.Map<String, String> headers, String body) {} 
        |${Spacer}record RawResponse(int statusCode, java.util.Map<String, String> headers, String body) {}
        |${Spacer}static Type getType(final Class<?> type, final boolean isIterable) {
        |${Spacer(2)}if(isIterable) {
        |${Spacer(3)}return new ParameterizedType() {
        |${Spacer(4)}public Type getRawType() { return java.util.List.class; }
        |${Spacer(4)}public Type[] getActualTypeArguments() { return new Class<?>[]{type}; }
        |${Spacer(4)}public Type getOwnerType() { return null; }
        |${Spacer(3)}};
        |${Spacer(2)}}
        |${Spacer(2)}else { return type; }
        |${Spacer}}
        |}
        |
    """.trimMargin()
}
