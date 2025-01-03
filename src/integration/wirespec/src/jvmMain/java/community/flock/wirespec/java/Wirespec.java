package community.flock.wirespec.java;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
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
    interface Serialization<RAW> extends Serializer<RAW>, Deserializer<RAW> {}
    interface QueryParamSerializer { <T> List<String> serializeQuery(T value, Type type); }
    interface Serializer<RAW> extends QueryParamSerializer { <T> RAW serialize(T t, Type type); }
    interface QueryParamDeserializer { <T> T deserializeQuery(List<String> values, Type type); }
    interface Deserializer<RAW> extends QueryParamDeserializer { <T> T deserialize(RAW raw, Type type); }
    record RawRequest(String method, List<String> path, Map<String, List<String>> queries, Map<String, String> headers, String body) {}
    record RawResponse(int statusCode, Map<String, String> headers, String body) {}
    static Type getType(final Class<?> type, final boolean isIterable) {
        if(isIterable) {
            return new ParameterizedType() {
                public Type getRawType() { return java.util.List.class; }
                public Type[] getActualTypeArguments() { return new Class<?>[]{type}; }
                public Type getOwnerType() { return null; }
            };
        }
        else { return type; }
    }
}
