package community.flock.wirespec.java.serde;

import community.flock.wirespec.java.Wirespec;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static community.flock.wirespec.java.serde.DefaultSerialization.*;

public interface DefaultPathSerialization extends Wirespec.PathSerialization {

    @Override
    default <T> String serializePath(T t, Type type) {
        return t.toString();
    }

    @Override
    @SuppressWarnings("unchecked")
    default  <T> T deserializePath(String raw, Type type) {
        if(isWirespecEnum(getRawType(type))) {
            return (T) findEnumByLabel(getRawType(type), raw);
        }
        final Class<?> rawType = getRawType(type);
        return (T) PRIMITIVE_TYPES_CONVERSION.get(rawType).apply(raw);
    }
}