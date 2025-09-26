package community.flock.wirespec.java.serde;

import community.flock.wirespec.java.Wirespec;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static community.flock.wirespec.java.serde.DefaultSerialization.*;

public interface DefaultParamSerialization extends Wirespec.ParamSerialization {

    @Override
    default <T> List<String> serializeParam(T value, Type type) {
        if (isList(type)) {
            return ((List<?>) value).stream()
                    .map(Object::toString)
                    .toList();
        }
        if (isOptional(type)) {
            return ((Optional<?>) value)
                    .map(it -> Collections.singletonList(it.toString()))
                    .orElseGet(Collections::emptyList);
        }
        return Collections.singletonList(value.toString());
    }

    @Override
    @SuppressWarnings("unchecked")
    default <T> T deserializeParam(List<String> values, Type type) {
        if (isList(type)) {
            return (T) deserializeList(values, type);
        }
        if (isOptional(type)) {
            return (T) deserializeOptional(values, type);
        }
        Class<?> rawType = getRawType(type);
        if (isWirespecEnum(rawType)) {
            return (T) deserializeEnum(values, rawType);
        }
        return (T) deserializePrimitive(values, rawType);
    }


}