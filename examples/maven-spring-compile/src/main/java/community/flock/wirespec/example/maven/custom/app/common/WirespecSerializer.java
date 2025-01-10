package community.flock.wirespec.example.maven.custom.app.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import community.flock.wirespec.example.maven.custom.app.exception.SerializationException;
import community.flock.wirespec.java.Wirespec;
import org.springframework.stereotype.Component;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Component
public class WirespecSerializer implements Wirespec.Serialization<String> {

    private final ObjectMapper objectMapper;
    private final Map<Class<?>, Function<String, Object>> primitiveTypesConversion;

    public WirespecSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.primitiveTypesConversion = initPrimitiveTypesConversion();
    }

    private Map<Class<?>, Function<String, Object>> initPrimitiveTypesConversion() {
        Map<Class<?>, Function<String, Object>> conversion = new HashMap<>();
        conversion.put(String.class, s -> s);
        conversion.put(Integer.class, Integer::valueOf);
        conversion.put(Long.class, Long::valueOf);
        conversion.put(Double.class, Double::valueOf);
        conversion.put(Float.class, Float::valueOf);
        conversion.put(Boolean.class, Boolean::valueOf);
        conversion.put(Character.class, s -> s.charAt(0));
        conversion.put(Byte.class, Byte::valueOf);
        conversion.put(Short.class, Short::valueOf);
        return conversion;
    }

    @Override
    public <T> String serialize(T body, Type type) {
        try {
            if (body instanceof String) {
                return (String) body;
            }
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new SerializationException(e);
        }
    }

    @Override
    public <T> List<String> serializeParam(T value, Type type) {
        if (value == null) {
            return null;
        }
        if (isIterable(type)) {
            return StreamSupport.stream(((Iterable<?>) value).spliterator(), false)
                .map(Object::toString)
                .collect(Collectors.toList());
        }
        return List.of(value.toString());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T deserialize(String raw, Type valueType) {
        if (raw == null) {
            return null;
        }
        try {
            if (valueType == String.class) {
                return (T) raw;
            }
            return objectMapper.readValue(raw, objectMapper.constructType(valueType));
        } catch (JsonProcessingException e) {
            throw new SerializationException(e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T deserializeParam(List<String> values, Type type) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        if (isIterable(type)) {
            return (T) deserializeList(values, getIterableElementType(type));
        }
        if (isWirespecEnum(type)) {
            return (T) deserializeEnum(values, (Class<?>) type);
        }
        return (T) deserializePrimitive(values, (Class<?>) type);
    }

    private List<Object> deserializeList(List<String> values, Type type) {
        if (isWirespecEnum(type)) {
            return values.stream()
                .map(value -> findEnumByLabel((Class<?>) type, value))
                .collect(Collectors.toList());
        }
        return deserializePrimitiveList(values, (Class<?>) type);
    }

    private Object deserializePrimitive(List<String> values, Class<?> clazz) {
        String value = values.stream().findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No value provided for type: " + clazz.getSimpleName()));

        Function<String, Object> converter = primitiveTypesConversion.get(clazz);
        if (converter == null) {
            throw new IllegalArgumentException("Unsupported primitive type: " + clazz.getSimpleName());
        }
        return converter.apply(value);
    }

    private List<Object> deserializePrimitiveList(List<String> values, Class<?> clazz) {
        Function<String, Object> converter = primitiveTypesConversion.get(clazz);
        if (converter == null) {
            throw new IllegalArgumentException("Unsupported list element type: " + clazz.getSimpleName());
        }
        return values.stream()
            .map(converter)
            .collect(Collectors.toList());
    }

    private Object deserializeEnum(List<String> values, Class<?> enumClass) {
        String value = values.stream().findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No enum value provided for type: " + enumClass.getSimpleName()));
        return findEnumByLabel(enumClass, value);
    }

    private Object findEnumByLabel(Class<?> enumClass, String label) {
        for (Object enumConstant : enumClass.getEnumConstants()) {
            if (enumConstant instanceof Wirespec.Enum &&
                ((Wirespec.Enum) enumConstant).getLabel().equals(label)) {
                return enumConstant;
            }
        }
        throw new IllegalArgumentException("Invalid enum value '" + label + "' for type: " + enumClass.getSimpleName());
    }

    private boolean isIterable(Type type) {
        return type instanceof ParameterizedType &&
            Iterable.class.isAssignableFrom((Class<?>) ((ParameterizedType) type).getRawType());
    }

    private boolean isWirespecEnum(Type type) {
        if (type instanceof Class<?>) {
            Class<?> clazz = (Class<?>) type;
            for (Class<?> iface : clazz.getInterfaces()) {
                if (iface == Wirespec.Enum.class) {
                    return true;
                }
            }
        }
        return false;
    }

    private Type getIterableElementType(Type type) {
        if (type instanceof ParameterizedType) {
            Type[] typeArguments = ((ParameterizedType) type).getActualTypeArguments();
            if (typeArguments.length > 0) {
                return typeArguments[0];
            }
        }
        return null;
    }
}