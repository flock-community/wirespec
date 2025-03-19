# Examples

Here you can find examples of how to use:

* [The Gradle Plugin](gradle-ktor/README.md)
* [The Maven Plugin](maven-spring-compile/README.md)
* [And convert an OpenAPI Specification](maven-spring-convert/README.md)
* [A custom Emitter](maven-spring-custom/README.md)
* [The Spring integration](../src/integration/spring/README.md)

## Integration

Some notes on how Wirespec integrates with different libraries and frameworks

### Jackson (json object mapper)

For some languages Wirespec is sanitizing enums names because of usage of preserved keywords and forbidden characters.
This results into problems with serialization. In Jackson the following configuration can be used to fix this.

```kotlin
ObjectMapper()
    .enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING)
    .enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)
```
