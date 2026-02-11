package community.flock.wirespec.integration.jackson.java;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.introspect.AnnotatedParameter;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import community.flock.wirespec.emitters.java.JavaIdentifierEmitter;
import community.flock.wirespec.java.Wirespec;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A Jackson module that handles deserialization of all Wirespec.Refined, to ensure
 * collapse / expanse of the wrapper class around the string value.
 *
 * Example
 * ```kt
 * data class Id(value: String): Wirespec.Refined
 * data class Task(id: Id, title: String)
 * ```
 *
 * Having an object such as
 * ```
 * Task{id: Id("123"), title: "improve API contracts"}
 * ```
 * will serialise to:
 * ```json
 * {id:"123", title: "improve API contracts"}
 * ```
 * flattening the Wirespec.Refined as a String. Conversely, such JSON will deserialize back
 * into the original `Task`, expanding the `id` field into a type safe Id data class.
 *
 * @see Wirespec.Refined
 */
public class WirespecModuleJava extends SimpleModule {

    @Override
    public String getModuleName() {
        return "Wirespec Jackson Module for Java";
    }

    public WirespecModuleJava() {
        addSerializer(Wirespec.Refined.class, new RefinedSerializer());
        addSerializer(Wirespec.Enum.class, new EnumSerializer());
        setDeserializerModifier(new WirespecDeserializerModifier());
        setNamingStrategy(new JavaReservedKeywordNamingStrategy());
    }


    /**
     * Serializer that flattens any Wirespec.Refined wrapped String value during serialization.
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
        public void serialize(Wirespec.Refined value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeObject(value.value());
        }
    }

    /**
     * Serializer Wirespec.Enum classes.
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
        public void serialize(Wirespec.Enum value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeString(value.toString());
        }
    }

    /**
     * Deserializer Wirespec.Refined classes.
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
        public Wirespec.Refined deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            JsonNode node = jp.getCodec().readTree(jp);
            Constructor<?> constructor = vc.getDeclaredConstructors()[0];
            Object value = jp.getCodec().treeToValue(node, constructor.getParameterTypes()[0]);
            try {
                return (Wirespec.Refined) constructor.newInstance(value);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Deserializer Wirespec.Enum classes.
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
        public Enum<?> deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            JsonNode node = jp.getCodec().readTree(jp);
            return (Enum<?>) Arrays.stream(vc.getEnumConstants())
                    .filter(it -> {
                        try {
                            return Objects.equals(it.getClass().getDeclaredMethod("toString").invoke(it), node.asText());
                        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .findFirst()
                    .orElse(null);
        }
    }

    /**
     * Jackson modifier intercept the deserialization of Wirespec.Enum and Wirespec.Refined and modifies the deserializer
     *
     * @see Wirespec.Enum
     * @see WirespecModuleJava
     */
    class WirespecDeserializerModifier extends BeanDeserializerModifier {
        @Override
        public JsonDeserializer<?> modifyEnumDeserializer(
                DeserializationConfig config,
                JavaType type,
                BeanDescription beanDesc,
                JsonDeserializer<?> deserializer
        ) {
            if (Wirespec.Enum.class.isAssignableFrom(beanDesc.getBeanClass())) {
                return super.modifyDeserializer(config, beanDesc, new EnumDeserializer(beanDesc.getBeanClass()));
            }
            return super.modifyEnumDeserializer(config, type, beanDesc, deserializer);
        }

        @Override
        public JsonDeserializer<?> modifyDeserializer(
                DeserializationConfig config,
                BeanDescription beanDesc,
                JsonDeserializer<?> deserializer
        ) {
            if (Wirespec.Refined.class.isAssignableFrom(beanDesc.getBeanClass())) {
                return super.modifyDeserializer(config, beanDesc, new RefinedDeserializer(beanDesc.getBeanClass()));
            }
            return super.modifyDeserializer(config, beanDesc, deserializer);
        }
    }

    class JavaReservedKeywordNamingStrategy extends PropertyNamingStrategy {

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
