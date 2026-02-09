package community.flock.wirespec.java.serde;

import community.flock.wirespec.java.Wirespec;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Function;

public interface DefaultSerialization {

    static DefaultSerialization create() {
        return new DefaultSerialization() {
        };
    }

    Map<Class<?>, Function<String, Object>> PRIMITIVE_TYPES_CONVERSION = Map.of(
            String.class, s -> s,
            Integer.class, Integer::parseInt,
            Long.class, Long::parseLong,
            Double.class, Double::parseDouble,
            Float.class, Float::parseFloat,
            Boolean.class, Boolean::parseBoolean,
            Character.class, s -> s.charAt(0),
            Byte.class, Byte::parseByte,
            Short.class, Short::parseShort
    );

    static List<?> deserializeList(List<String> values, Type type) {
        Type elementType = getElementType(type);
        Class<?> rawElementType = getRawType(elementType);

        if (isWirespecEnum(rawElementType)) {
            return values.stream()
                    .map(value -> findEnumByLabel(rawElementType, value))
                    .toList();
        }
        return deserializePrimitiveList(values, rawElementType);
    }

    static Optional<?> deserializeOptional(List<String> values, Type type) {
        Type elementType = getElementType(type);
        Class<?> rawElementType = getRawType(elementType);

        if (isWirespecEnum(rawElementType)) {
            return values.stream()
                    .map(value -> findEnumByLabel(rawElementType, value))
                    .findFirst();
        }
        return deserializePrimitiveList(values, rawElementType).stream().findFirst();
    }

    static Object deserializePrimitive(List<String> values, Class<?> clazz) {
        String value = values.stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No value provided for type: " + clazz.getSimpleName()));

        Function<String, Object> converter = PRIMITIVE_TYPES_CONVERSION.get(clazz);
        if (converter == null) {
            throw new IllegalArgumentException("Unsupported primitive type: " + clazz.getSimpleName());
        }
        return converter.apply(value);
    }

    static List<Object> deserializePrimitiveList(List<String> values, Class<?> elementClass) {
        Function<String, Object> converter = PRIMITIVE_TYPES_CONVERSION.get(elementClass);
        if (converter == null) {
            throw new IllegalArgumentException("Unsupported list element type: " + elementClass.getSimpleName());
        }
        return values.stream()
                .map(converter)
                .toList();
    }

    static Object deserializeEnum(List<String> values, Class<?> enumClass) {
        String value = values.stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No enum value provided for type: " + enumClass.getSimpleName()));
        return findEnumByLabel(enumClass, value);
    }

    static Object findEnumByLabel(Class<?> enumClass, String label) {
        return Arrays.stream(enumClass.getEnumConstants())
                .filter(e -> ((Wirespec.Enum) e).getLabel().equals(label))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invalid enum value '" + label + "' for type: " + enumClass.getSimpleName()));
    }

    static boolean isList(Type type) {
        if (type instanceof ParameterizedType parameterizedType) {
            return List.class.isAssignableFrom((Class<?>) parameterizedType.getRawType());
        }
        return type instanceof Class<?> && List.class.isAssignableFrom((Class<?>) type);
    }

    static boolean isOptional(Type type) {
        if (type instanceof ParameterizedType parameterizedType) {
            return Optional.class.isAssignableFrom((Class<?>) parameterizedType.getRawType());
        }
        return type instanceof Class<?> && Optional.class.isAssignableFrom((Class<?>) type);
    }

    static boolean isWirespecRefined(Class<?> clazz) {
        return Arrays.stream(clazz.getInterfaces())
                .anyMatch(iface -> iface == Wirespec.Refined.class);
    }

    static boolean isWirespecEnum(Class<?> clazz) {
        return Arrays.stream(clazz.getInterfaces())
                .anyMatch(iface -> iface == Wirespec.Enum.class);
    }

    static Type getElementType(Type type) {
        if (type instanceof ParameterizedType parameterizedType) {
            Type[] typeArguments = parameterizedType.getActualTypeArguments();
            if (typeArguments.length == 1) {
                return typeArguments[0];
            }
        }
        throw new IllegalArgumentException("Cannot determine list element type");
    }

    static Class<?> getRawType(Type type) {
        if (type instanceof Class<?>) {
            return (Class<?>) type;
        }
        if (type instanceof ParameterizedType parameterizedType) {
            return (Class<?>) parameterizedType.getRawType();
        }
        throw new IllegalArgumentException("Invalid type: " + type);
    }
}