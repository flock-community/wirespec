package community.flock.wirespec.integration.jackson.v3.java;

import community.flock.wirespec.emitters.java.JavaIdentifierEmitter;
import community.flock.wirespec.java.Wirespec;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.DeserializationConfig;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.PropertyNamingStrategy;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.deser.ValueDeserializerModifier;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.introspect.AnnotatedMethod;
import tools.jackson.databind.introspect.AnnotatedParameter;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.StdSerializer;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Jackson 3 variant of the Wirespec Java module. It collapses / expands the wrapper
 * class around `Wirespec.Refined` values and serializes `Wirespec.Enum` via
 * `toString()`, mirroring the Jackson 2 module.
 *
 * Java records expose their components through accessors, so unlike the Kotlin module
 * this module needs no visibility configuration.
 *
 * @see Wirespec.Refined
 */
public class WirespecModuleJava extends SimpleModule {

    @Override
    public String getModuleName() {
        return "Wirespec Jackson 3 Module for Java";
    }

    public WirespecModuleJava() {
        addSerializer(Wirespec.Refined.class, new RefinedSerializer());
        addSerializer(Wirespec.Enum.class, new EnumSerializer());
        setDeserializerModifier(new WirespecDeserializerModifier());
        // Jackson 3 modules cannot register a PropertyNamingStrategy (no SetupContext
        // hook); apply JavaReservedKeywordNamingStrategy on the mapper builder instead.
        // WirespecSerialization does this for you.
    }


    /**
     * Serializer that flattens any Wirespec.Refined wrapped value during serialization.
     *
     * @see Wirespec.Refined
     * @see WirespecModuleJava
     */
    class RefinedSerializer extends StdSerializer<Wirespec.Refined> {
        public RefinedSerializer() {
            this(null);
        }

        public RefinedSerializer(Class<Wirespec.Refined> t) {
            super(t);
        }

        @Override
        public void serialize(Wirespec.Refined value, JsonGenerator gen, SerializationContext ctxt) {
            gen.writePOJO(value.value());
        }
    }

    /**
     * Serializer for Wirespec.Enum classes.
     *
     * @see Wirespec.Enum
     * @see WirespecModuleJava
     */
    class EnumSerializer extends StdSerializer<Wirespec.Enum> {

        public EnumSerializer() {
            this(null);
        }

        public EnumSerializer(Class<Wirespec.Enum> t) {
            super(t);
        }

        @Override
        public void serialize(Wirespec.Enum value, JsonGenerator gen, SerializationContext ctxt) {
            gen.writeString(value.toString());
        }
    }

    /**
     * Deserializer for Wirespec.Refined classes.
     *
     * @see Wirespec.Refined
     * @see WirespecModuleJava
     */
    class RefinedDeserializer extends StdDeserializer<Wirespec.Refined> {
        private final Class<?> vc;

        public RefinedDeserializer(Class<?> vc) {
            super(vc);
            this.vc = vc;
        }

        @Override
        public Wirespec.Refined deserialize(JsonParser jp, DeserializationContext ctxt) {
            JsonNode node = ctxt.readTree(jp);
            Constructor<?> constructor = vc.getDeclaredConstructors()[0];
            Object value = ctxt.readTreeAsValue(node, constructor.getParameterTypes()[0]);
            try {
                return (Wirespec.Refined) constructor.newInstance(value);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Deserializer for Wirespec.Enum classes.
     *
     * @see Wirespec.Enum
     * @see WirespecModuleJava
     */
    class EnumDeserializer extends StdDeserializer<Enum<?>> {
        private final Class<?> vc;

        public EnumDeserializer(Class<?> vc) {
            super(vc);
            this.vc = vc;
        }

        @Override
        public Enum<?> deserialize(JsonParser jp, DeserializationContext ctxt) {
            JsonNode node = ctxt.readTree(jp);
            return (Enum<?>) Arrays.stream(vc.getEnumConstants())
                    .filter(it -> {
                        try {
                            return Objects.equals(it.getClass().getDeclaredMethod("toString").invoke(it), node.asString());
                        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .findFirst()
                    .orElse(null);
        }
    }

    /**
     * Jackson modifier that intercepts the deserialization of Wirespec.Enum and Wirespec.Refined
     * and swaps in the custom deserializers above.
     *
     * @see Wirespec.Enum
     * @see WirespecModuleJava
     */
    class WirespecDeserializerModifier extends ValueDeserializerModifier {
        @Override
        public ValueDeserializer<?> modifyEnumDeserializer(
                DeserializationConfig config,
                JavaType type,
                BeanDescription.Supplier beanDescRef,
                ValueDeserializer<?> deserializer
        ) {
            if (Wirespec.Enum.class.isAssignableFrom(beanDescRef.getBeanClass())) {
                return new EnumDeserializer(beanDescRef.getBeanClass());
            }
            return super.modifyEnumDeserializer(config, type, beanDescRef, deserializer);
        }

        @Override
        public ValueDeserializer<?> modifyDeserializer(
                DeserializationConfig config,
                BeanDescription.Supplier beanDescRef,
                ValueDeserializer<?> deserializer
        ) {
            if (Wirespec.Refined.class.isAssignableFrom(beanDescRef.getBeanClass())) {
                return new RefinedDeserializer(beanDescRef.getBeanClass());
            }
            return super.modifyDeserializer(config, beanDescRef, deserializer);
        }
    }

    public static class JavaReservedKeywordNamingStrategy extends PropertyNamingStrategy {

        private String translate(String key) {

            Set<String> kotlinSet = JavaIdentifierEmitter.Companion.getReservedKeywords();
            List<String> keywords = kotlinSet.stream()
                    .map(keyword -> "_" + keyword)
                    .collect(Collectors.toList());

            if (keywords.contains(key)) {
                return key.substring(1);
            } else {
                return key;
            }
        }

        @Override
        public String nameForGetterMethod(MapperConfig<?> config, AnnotatedMethod method, String defaultName) {
            return translateIfRecord(defaultName, method.getDeclaringClass());
        }

        @Override
        public String nameForSetterMethod(MapperConfig<?> config, AnnotatedMethod method, String defaultName) {
            return translateIfRecord(defaultName, method.getDeclaringClass());
        }

        @Override
        public String nameForConstructorParameter(MapperConfig<?> config, AnnotatedParameter ctorParam, String defaultName) {
            return translateIfRecord(defaultName, ctorParam.getOwner().getRawType());
        }

        private String translateIfRecord(String name, Class<?> clazz) {
            if (Record.class.isAssignableFrom(clazz)) {
                return translate(name);
            }
            return name;
        }
    }
}
