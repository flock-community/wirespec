# Examples

Here you can find examples of how to use:

* [A custom Emitter](spring-boot-custom-maven-plugin/README.md)
* [The Gradle Plugin](spring-boot-gradle-plugin/README.md)
* [The Maven Plugin](spring-boot-maven-plugin/README.md)
* [And convert an OpenAPI Specification](spring-boot-openapi-maven-plugin/README.md)

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
