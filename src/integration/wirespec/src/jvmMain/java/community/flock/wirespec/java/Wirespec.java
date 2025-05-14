package community.flock.wirespec.java;

import java.lang.reflect.Type;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Map;

public interface Wirespec {
    interface Enum { String getLabel(); }
    interface Endpoint {}
    interface Refined { String getValue(); }
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
        ClientEdge<Req, Res> getClient(Serialization<String> serialization);
    }
    interface Server<Req extends Request<?>, Res extends Response<?>> {
        String getPathTemplate();
        String getMethod();
        ServerEdge<Req, Res> getServer(Serialization<String> serialization);
    }
    enum Method { GET, PUT, POST, DELETE, OPTIONS, HEAD, PATCH, TRACE }
    interface Request<T> { Path getPath(); Method getMethod(); Queries getQueries(); Headers getHeaders(); T getBody(); interface Headers extends Wirespec.Headers {} }
    interface Response<T> { int getStatus(); Headers getHeaders(); T getBody(); interface Headers extends Wirespec.Headers {} }
    interface ParamSerialization extends ParamSerializer, ParamDeserializer {}
    interface Serialization<RAW> extends Serializer<RAW>, Deserializer<RAW>, ParamSerialization {}
    interface ParamSerializer { <T> List<String> serializeParam(T value, Type type); }
    interface Serializer<RAW> extends ParamSerializer { <T> RAW serialize(T t, Type type); }
    interface ParamDeserializer { <T> T deserializeParam(List<String> values, Type type); }
    interface Deserializer<RAW> extends ParamDeserializer { <T> T deserialize(RAW raw, Type type); }
    record RawRequest(String method, List<String> path, Map<String, List<String>> queries, Map<String, List<String>> headers, String body) {}
    record RawResponse(int statusCode, Map<String, List<String>> headers, String body) {}
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
