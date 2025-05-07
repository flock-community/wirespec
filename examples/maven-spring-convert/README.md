# Example: How to convert an OpenAPI Specification with Wirespec

## Wirespec Maven Plugin Configuration

```xml

<plugin>
    <groupId>community.flock.wirespec.plugin.maven</groupId>
    <artifactId>wirespec-maven-plugin</artifactId>
    <version>0.0.0-SNAPSHOT</version>
    <executions>
        <execution>
            <id>kotlin-v2</id>
            <goals>
                <goal>convert</goal>
            </goals>
            <configuration>
                <input>${project.basedir}/src/main/openapi/petstorev2.json</input>
                <output>${project.build.directory}/generated-sources</output>
                <packageName>community.flock.wirespec.generated.kotlin.v2</packageName>
                <format>OpenAPIV2</format>
                <languages>
                    <language>Kotlin</language>
                </languages>
                <shared>true</shared>
            </configuration>
        </execution>
        <execution>
            <id>kotlin-v3</id>
            <goals>
                <goal>convert</goal>
            </goals>
            <configuration>
                <input>${project.basedir}/src/main/openapi/petstorev3.json</input>
                <output>${project.build.directory}/generated-sources</output>
                <packageName>community.flock.wirespec.generated.kotlin.v3</packageName>
                <format>OpenAPIV3</format>
                <languages>
                    <language>Kotlin</language>
                </languages>
                <shared>false</shared>
            </configuration>
        </execution>
        <execution>
            <id>java-v2</id>
            <goals>
                <goal>convert</goal>
            </goals>
            <configuration>
                <input>${project.basedir}/src/main/openapi/petstorev2.json</input>
                <output>${project.build.directory}/generated-sources</output>
                <packageName>community.flock.wirespec.generated.java.v2</packageName>
                <format>OpenAPIV2</format>
                <languages>
                    <language>Java</language>
                </languages>
                <shared>false</shared>
            </configuration>
        </execution>
        <execution>
            <id>java-v3</id>
            <goals>
                <goal>convert</goal>
            </goals>
            <configuration>
                <input>${project.basedir}/src/main/openapi/petstorev3.json</input>
                <output>${project.build.directory}/generated-sources</output>
                <packageName>community.flock.wirespec.generated.java.v3</packageName>
                <format>OpenAPIV3</format>
                <languages>
                    <language>Java</language>
                </languages>
                <shared>false</shared>
            </configuration>
        </execution>
    </executions>
</plugin>
```

According to the [actual pom.xml](pom.xml) file.

## Using the Preprocessor Feature

The Wirespec Maven Plugin supports preprocessing of input files before conversion. This allows you to modify the input content programmatically before it is processed by the converter.

### How to Use the Preprocessor

1. Create a class that implements a method that takes a String and returns a String. This method will be used to preprocess the input content.
   ```java
   public class MyPreprocessor {
       public String process(String input) {
           // Modify the input content
           return modifiedInput;
       }
   }
   ```

2. Add the `preProcessor` parameter to your plugin configuration, specifying the fully qualified name of your preprocessor class:
   ```xml
   <configuration>
       <input>${project.basedir}/src/main/openapi/api.json</input>
       <output>${project.build.directory}/generated-sources</output>
       <preProcessor>com.example.MyPreprocessor</preProcessor>
       <!-- Other configuration parameters -->
   </configuration>
   ```

3. The preprocessor class will be automatically compiled before the Wirespec Maven Plugin is executed, as the plugin now operates in the process-classes phase which comes after compilation.

### Example Preprocessor

An example preprocessor implementation is provided in this project:
[ExamplePreprocessor.java](src/main/java/community/flock/wirespec/example/maven/ExamplePreprocessor.java)

This example preprocessor adds a `__preprocessed` field to JSON input files to demonstrate the preprocessing capability.
