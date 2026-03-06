package community.flock.wirespec.emitters.java

import community.flock.wirespec.compiler.core.emit.DEFAULT_SHARED_PACKAGE_STRING
import community.flock.wirespec.compiler.core.emit.Shared
import community.flock.wirespec.compiler.core.emit.Spacer

data object JavaShared : Shared {
    override val packageString: String = "$DEFAULT_SHARED_PACKAGE_STRING.java"

    override val source =
        """
        |package $packageString;
        |
        |import java.lang.reflect.Type;
        |import java.lang.reflect.ParameterizedType;
        |import java.util.List;
        |import java.util.Map;
        |import java.util.Optional;
        |
        |public interface Wirespec {
        |${Spacer}interface Enum { String label(); }
        |${Spacer}interface Endpoint {}
        |${Spacer}interface Refined<T> { T value(); }
        |${Spacer}interface Path {}
        |${Spacer}interface Queries {}
        |${Spacer}interface Headers {}
        |${Spacer}interface Handler {}
        |${Spacer}interface ServerEdge<Req extends Request<?>, Res extends Response<?>> {
        |${Spacer(2)}Req from(RawRequest request);
        |${Spacer(2)}RawResponse to(Res response);
        |$Spacer}
        |${Spacer}interface ClientEdge<Req extends Request<?>, Res extends Response<?>> {
        |${Spacer(2)}RawRequest to(Req request);
        |${Spacer(2)}Res from(RawResponse response);
        |$Spacer}
        |${Spacer}interface Client<Req extends Request<?>, Res extends Response<?>> {
        |${Spacer(2)}String getPathTemplate();
        |${Spacer(2)}String getMethod();
        |${Spacer(2)}ClientEdge<Req, Res> getClient(Serialization serialization);
        |$Spacer}
        |${Spacer}interface Server<Req extends Request<?>, Res extends Response<?>> {
        |${Spacer(2)}String getPathTemplate();
        |${Spacer(2)}String getMethod();
        |${Spacer(2)}ServerEdge<Req, Res> getServer(Serialization serialization);
        |$Spacer}
        |${Spacer}enum Method { GET, PUT, POST, DELETE, OPTIONS, HEAD, PATCH, TRACE }
        |${Spacer}interface Request<T> { Path path(); Method method(); Queries queries(); Headers headers(); T body(); interface Headers extends Wirespec.Headers {} }
        |${Spacer}interface Response<T> { Integer status(); Headers headers(); T body(); interface Headers extends Wirespec.Headers {} }
        |${Spacer}interface Serialization extends Serializer, Deserializer {}
        |${Spacer}interface Serializer extends BodySerializer, PathSerializer, ParamSerializer {}
        |${Spacer}interface Deserializer extends BodyDeserializer, PathDeserializer, ParamDeserializer {}
        |${Spacer}interface BodySerialization extends BodySerializer, BodyDeserializer {}
        |${Spacer}interface BodySerializer { <T> byte[] serializeBody(T t, Type type); }
        |${Spacer}interface BodyDeserializer { <T> T deserializeBody(byte[] raw, Type type); }
        |${Spacer}interface PathSerialization extends PathSerializer, PathDeserializer {}
        |${Spacer}interface PathSerializer { <T> String serializePath(T t, Type type); }
        |${Spacer}interface PathDeserializer { <T> T deserializePath(String raw, Type type); }
        |${Spacer}interface ParamSerialization extends ParamSerializer, ParamDeserializer {}
        |${Spacer}interface ParamSerializer { <T> List<String> serializeParam(T value, Type type); }
        |${Spacer}interface ParamDeserializer { <T> T deserializeParam(List<String> values, Type type); }
        |${Spacer}record RawRequest(String method, List<String> path, Map<String, List<String>> queries, Map<String, List<String>> headers, Optional<byte[]> body) {}
        |${Spacer}record RawResponse(int statusCode, Map<String, List<String>> headers, Optional<byte[]> body) {}
        |${Spacer}static Type getType(final Class<?> actualTypeArguments, final Class<?> rawType) {
        |${Spacer(2)}if(rawType != null) {
        |${Spacer(3)}return new ParameterizedType() {
        |${Spacer(4)}public Type getRawType() { return rawType; }
        |${Spacer(4)}public Type[] getActualTypeArguments() { return new Class<?>[]{actualTypeArguments}; }
        |${Spacer(4)}public Type getOwnerType() { return null; }
        |${Spacer(3)}};
        |${Spacer(2)}}
        |${Spacer(2)}else { return actualTypeArguments; }
        |$Spacer}
        |}
        |
        """.trimMargin()
}
