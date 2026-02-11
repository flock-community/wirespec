package community.flock.wirespec.java;
import java.lang.reflect.Type;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Map;
public interface Wirespec {
    public interface Enum {
        String label();
    }
    public interface Endpoint {
    }
    public interface Channel {
    }
    public interface Refined<T> {
        T value();
    }
    public interface Path {
    }
    public interface Queries {
    }
    public interface Headers {
    }
    public interface Handler {
    }
    public enum Method {
        GET,
        PUT,
        POST,
        DELETE,
        OPTIONS,
        HEAD,
        PATCH,
        TRACE
    }  public interface Request<T> {
        Path path();
        Method method();
        Queries queries();
        Headers headers();
        T body();
        public interface Headers {
        }
    }
    public interface Response<T> {
        Integer status();
        Headers headers();
        T body();
        public interface Headers {
        }
    }
    public interface Serialization extends Serializer, Deserializer {
    }
    public interface Serializer extends BodySerializer, PathSerializer, ParamSerializer {
    }
    public interface Deserializer extends BodyDeserializer, PathDeserializer, ParamDeserializer {
    }
    public interface BodySerialization extends BodySerializer, BodyDeserializer {
    }
    public interface BodySerializer {
        public <T> byte[] serializeBody(T t, Type type);
    }
    public interface BodyDeserializer {
        public <T> T deserializeBody(byte[] raw, Type type);
    }
    public interface PathSerialization extends PathSerializer, PathDeserializer {
    }
    public interface PathSerializer {
        public <T> String serializePath(T t, Type type);
    }
    public interface PathDeserializer {
        public <T> T deserializePath(String raw, Type type);
    }
    public interface ParamSerialization extends ParamSerializer, ParamDeserializer {
    }
    public interface ParamSerializer {
        public <T> java.util.List<String> serializeParam(T value, Type type);
    }
    public interface ParamDeserializer {
        public <T> T deserializeParam(java.util.List<String> values, Type type);
    }
    public static record RawRequest (
            String method,
            java.util.List<String> path,
            java.util.Map<String, java.util.List<String>> queries,
            java.util.Map<String, java.util.List<String>> headers,
            java.util.Optional<byte[]> body
    ) {
    }
    public static record RawResponse (
            Integer statusCode,
            java.util.Map<String, java.util.List<String>> headers,
            java.util.Optional<byte[]> body
    ) {
    }
    public interface ServerEdge<Req extends Request<?>, Res extends Response<?>> {
        public Req from(RawRequest request);
        public RawResponse to(Res response);
    }
    public interface ClientEdge<Req extends Request<?>, Res extends Response<?>> {
        public RawRequest to(Req request);
        public Res from(RawResponse response);
    }
    public interface Client<Req extends Request<?>, Res extends Response<?>> {
        public String getPathTemplate();
        public String getMethod();
        public ClientEdge<Req, Res> getClient(Serialization serialization);
    }
    public interface Server<Req extends Request<?>, Res extends Response<?>> {
        public String getPathTemplate();
        public String getMethod();
        public ServerEdge<Req, Res> getServer(Serialization serialization);
    }
    public static Type getType(final Class<?> actualTypeArguments, final Class<?> rawType) {
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