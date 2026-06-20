# Wirespec Maven Avro example (IR mode)

This example converts an Avro schema (`src/main/avro/test_avro_001.avsc`) into Wirespec and
generates Java sources with the Wirespec IR emitter. The Avro schema + converter classes
(`<Type>Avro`) are added by the `AvroExtension`, a Wirespec `IrExtension` registered on the
emitter via `extensionClasses`.

## Wirespec Maven Plugin Configuration

```xml
<plugin>
    <groupId>community.flock.wirespec.plugin.maven</groupId>
    <artifactId>wirespec-maven-plugin</artifactId>
    <executions>
        <execution>
            <id>test_avro_001</id>
            <goals>
                <goal>convert</goal>
            </goals>
            <configuration>
                <input>${project.basedir}/src/main/avro/test_avro_001.avsc</input>
                <output>${project.build.directory}/generated-sources</output>
                <format>Avro</format>
                <languages>
                    <language>Java</language>
                </languages>
                <ir>true</ir>
                <extensionClasses>
                    <extensionClass>community.flock.wirespec.integration.avro.extension.AvroExtension</extensionClass>
                </extensionClasses>
                <packageName>com.eventloopsoftware.kafka</packageName>
            </configuration>
        </execution>
    </executions>
</plugin>
```

The `avro-jvm` integration (which contains `AvroExtension`) is on the project's compile
classpath, so the plugin's class loader can resolve the extension. `AvroExtension` works for
both the Java and Kotlin IR emitters: it detects the target language from the IR and emits the
matching `<Type>Avro` source.

## Generated output

For every record and enum the extension emits a `<Type>Avro` class in the
`<package>.avro` sub-package, next to the generated model. Each one parses its Avro `Schema`
at class-load time and converts between the generated model and Avro
`GenericData.Record` / `EnumSymbol` values:

```java
var avro = TestAvroRecordAvro.to(message);                 // model -> Avro record
var message = TestAvroRecordAvro.from(record);             // Avro record -> model
```

See [`AvroExampleService`](src/main/java/community/flock/wirespec/examples/maven/avro/AvroExampleService.java)
for the Kafka producer/consumer wiring.
