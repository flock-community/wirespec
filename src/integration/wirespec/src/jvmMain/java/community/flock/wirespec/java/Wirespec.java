package community.flock.wirespec.java;

import java.lang.reflect.Type;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface Wirespec {
    interface Enum { String getLabel(); }
    interface Endpoint {}
    interface Refined<T> { T getValue(); }
    interface Path {}
    interface Queries {}
    interface Headers {}
    interface Handler {}
    interface ServerEdge<Req extends Request<?>, Res extends Response<?>> {
        Req from(RawRequest request);
        RawResponse to(Res response);
    }
    interface ClientEdge<Req extends Request<?>, Res extends Response<?>> {
        RawRequest to(Req request);
        Res from(RawResponse response);
    }
    interface Client<Req extends Request<?>, Res extends Response<?>> {
        String getPathTemplate();
        String getMethod();
        ClientEdge<Req, Res> getClient(Serialization serialization);
    }
    interface Server<Req extends Request<?>, Res extends Response<?>> {
        String getPathTemplate();
        String getMethod();
        ServerEdge<Req, Res> getServer(Serialization serialization);
    }
    enum Method { GET, PUT, POST, DELETE, OPTIONS, HEAD, PATCH, TRACE }
    interface Request<T> { Path path(); Method method(); Queries queries(); Headers headers(); T body(); interface Headers extends Wirespec.Headers {} }
    interface Response<T> { int status(); Headers headers(); T body(); interface Headers extends Wirespec.Headers {} }
    interface Serialization extends Serializer, Deserializer {}
    interface Serializer extends BodySerializer, PathSerializer, ParamSerializer {}
    interface Deserializer extends BodyDeserializer, PathDeserializer, ParamDeserializer {}
    interface BodySerialization extends BodySerializer, BodyDeserializer {}
    interface BodySerializer { <T> byte[] serializeBody(T t, Type type); }
    interface BodyDeserializer { <T> T deserializeBody(byte[] raw, Type type); }
    interface PathSerialization extends PathSerializer, PathDeserializer {}
    interface PathSerializer { <T> String serializePath(T t, Type type); }
    interface PathDeserializer { <T> T deserializePath(String raw, Type type); }
    interface ParamSerialization extends ParamSerializer, ParamDeserializer {}
    interface ParamSerializer { <T> List<String> serializeParam(T value, Type type); }
    interface ParamDeserializer { <T> T deserializeParam(List<String> values, Type type); }
    record RawRequest(String method, List<String> path, Map<String, List<String>> queries, Map<String, List<String>> headers, byte[] body) {}
    record RawResponse(int statusCode, Map<String, List<String>> headers, byte[] body) {}
    interface Transportation { CompletableFuture<RawResponse> transport(RawRequest request); }
    static Type getType(final Class<?> actualTypeArguments, final Class<?> rawType) {
        if(rawType != null) {
            return new ParameterizedType() {
                public Type getRawType() { return rawType; }
                public Type[] getActualTypeArguments() { return new Class<?>[]{actualTypeArguments}; }
                public Type getOwnerType() { return null; }
            };
        }
        else { return actualTypeArguments; }
    }
}
