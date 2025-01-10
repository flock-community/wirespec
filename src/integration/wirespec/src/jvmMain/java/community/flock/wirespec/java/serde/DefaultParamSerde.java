package community.flock.wirespec.java.serde;

import community.flock.wirespec.java.Wirespec;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Function;

public class DefaultParamSerde implements Wirespec.ParamSerialization {

  private final Map<Class<?>, Function<String, Object>> primitiveTypesConversion;

  public DefaultParamSerde() {
    primitiveTypesConversion = new HashMap<>();
    primitiveTypesConversion.put(String.class, s -> s);
    primitiveTypesConversion.put(Integer.class, Integer::parseInt);
    primitiveTypesConversion.put(Long.class, Long::parseLong);
    primitiveTypesConversion.put(Double.class, Double::parseDouble);
    primitiveTypesConversion.put(Float.class, Float::parseFloat);
    primitiveTypesConversion.put(Boolean.class, Boolean::parseBoolean);
    primitiveTypesConversion.put(Character.class, s -> s.charAt(0));
    primitiveTypesConversion.put(Byte.class, Byte::parseByte);
    primitiveTypesConversion.put(Short.class, Short::parseShort);
  }

  @Override
  public <T> List<String> serializeParam(T value, Type type) {
    if (isList(type)) {
      return ((List<?>) value).stream()
          .map(Object::toString)
          .toList();
    }
    return Collections.singletonList(value.toString());
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T deserializeParam(List<String> values, Type type) {
    if (isList(type)) {
      return (T) deserializeList(values, type);
    }
    Class<?> rawType = getRawType(type);
    if (isWirespecEnum(rawType)) {
      return (T) deserializeEnum(values, rawType);
    }
    return (T) deserializePrimitive(values, rawType);
  }

  private List<?> deserializeList(List<String> values, Type type) {
    Type elementType = getListElementType(type);
    Class<?> rawElementType = getRawType(elementType);

    if (isWirespecEnum(rawElementType)) {
      return values.stream()
          .map(value -> findEnumByLabel(rawElementType, value))
          .toList();
    }
    return deserializePrimitiveList(values, rawElementType);
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

  private List<Object> deserializePrimitiveList(List<String> values, Class<?> elementClass) {
    Function<String, Object> converter = primitiveTypesConversion.get(elementClass);
    if (converter == null) {
      throw new IllegalArgumentException("Unsupported list element type: " + elementClass.getSimpleName());
    }
    return values.stream()
        .map(converter)
        .toList();
  }

  private Object deserializeEnum(List<String> values, Class<?> enumClass) {
    String value = values.stream().findFirst()
        .orElseThrow(() -> new IllegalArgumentException("No enum value provided for type: " + enumClass.getSimpleName()));
    return findEnumByLabel(enumClass, value);
  }

  private Object findEnumByLabel(Class<?> enumClass, String label) {
    return Arrays.stream(enumClass.getEnumConstants())
        .filter(e -> ((Wirespec.Enum) e).getLabel().equals(label))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException(
            "Invalid enum value '" + label + "' for type: " + enumClass.getSimpleName()));
  }

  private boolean isList(Type type) {
    if (type instanceof ParameterizedType parameterizedType) {
      return List.class.isAssignableFrom((Class<?>) parameterizedType.getRawType());
    }
    return type instanceof Class<?> && List.class.isAssignableFrom((Class<?>) type);
  }

  private boolean isWirespecEnum(Class<?> clazz) {
    return Arrays.stream(clazz.getInterfaces())
        .anyMatch(iface -> iface == Wirespec.Enum.class);
  }

  private Type getListElementType(Type type) {
    if (type instanceof ParameterizedType parameterizedType) {
      Type[] typeArguments = parameterizedType.getActualTypeArguments();
      if (typeArguments.length == 1) {
        return typeArguments[0];
      }
    }
    throw new IllegalArgumentException("Cannot determine list element type");
  }

  private Class<?> getRawType(Type type) {
    if (type instanceof Class<?>) {
      return (Class<?>) type;
    }
    if (type instanceof ParameterizedType parameterizedType) {
      return (Class<?>) parameterizedType.getRawType();
    }
    throw new IllegalArgumentException("Invalid type: " + type);
  }
}