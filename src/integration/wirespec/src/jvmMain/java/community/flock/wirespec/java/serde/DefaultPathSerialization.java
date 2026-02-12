package community.flock.wirespec.java.serde;

import community.flock.wirespec.java.Wirespec;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;

import static community.flock.wirespec.java.serde.DefaultSerialization.*;

public interface DefaultPathSerialization extends Wirespec.PathSerialization {

    @Override
    default <T> String serializePath(T t, Type type) {
        if(t instanceof Wirespec.Refined<?> refined){
            return refined.value().toString();
        }
        if(t instanceof Wirespec.Enum refined){
            return refined.label();
        }
        return t.toString();
    }

    @Override
    @SuppressWarnings("unchecked")
    default  <T> T deserializePath(String raw, Type type) {
        final Class<?> rawType = getRawType(type);
        if(isWirespecRefined(rawType)) {
            try {
                Constructor<?> constructor = rawType.getDeclaredConstructors()[0];
                Class<?> paramType = constructor.getParameterTypes()[0];
                Object value = PRIMITIVE_TYPES_CONVERSION.get(paramType).apply(raw);
                return (T) constructor.newInstance(value);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
        if(isWirespecEnum(rawType)) {
            return (T) findEnumByLabel(rawType, raw);
        }
        return (T) PRIMITIVE_TYPES_CONVERSION.get(rawType).apply(raw);
    }
}