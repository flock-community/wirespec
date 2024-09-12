package community.flock.wirespec.java;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public interface Wirespec {
    interface Enum {
    }

    interface Refined {
        String getValue();
    }

    interface Endpoint {
    }

    enum Method {GET, PUT, POST, DELETE, OPTIONS, HEAD, PATCH, TRACE}

    record Content<T>(String type, T body) {
    }

    interface Request<T> {
        String getPath();

        Method getMethod();

        java.util.Map<String, java.util.List<Object>> getQuery();

        java.util.Map<String, java.util.List<Object>> getHeaders();

        Content<T> getContent();
    }

    interface Response<T> {
        int getStatus();

        java.util.Map<String, java.util.List<Object>> getHeaders();

        Content<T> getContent();
    }

    interface ContentMapper<B> {
        <T> Content<T> read(Content<B> content, Type valueType);

        <T> Content<B> write(Content<T> content);
    }

    static Type getType(final Class<?> type, final boolean isIterable) {
        if (isIterable) {
            return new ParameterizedType() {
                public Type getRawType() {
                    return java.util.List.class;
                }

                public Type[] getActualTypeArguments() {
                    Class<?>[] types = {type};
                    return types;
                }

                public Type getOwnerType() {
                    return null;
                }
            };
        } else {
            return type;
        }
    }
}
