---
title: Maven
slug: /plugins/maven
sidebar_position: 3
---

# Wirespec Maven Plugin

![Maven Central](https://img.shields.io/maven-central/v/community.flock.wirespec.plugin.maven/wirespec-maven-plugin)

The Wirespec Maven plugin allows you to seamlessly integrate Wirespec compilation into your Maven build process. This
enables you to automatically generate code from your Wirespec definitions whenever you build your project, ensuring that
your implementation code is always up-to-date with your specifications.

## Installation

To use the Wirespec Maven plugin, you need to add it to your `pom.xml` file. Here's how:

1. **Add the Plugin Dependency:**

```xml
<build>
    <plugins>
        <plugin>
            <groupId>community.flock.wirespec.plugin.maven</groupId>
            <artifactId>wirespec-maven-plugin</artifactId>
            <version>{{WIRESPEC_VERSION}}</version>
            <executions>
                <execution>
                    <id>typescript</id>
                    <goals>
                        <goal>compile</goal>
                    </goals>
                    <configuration>
                        <input>${project.basedir}/src/main/wirespec</input>
                        <output>${project.basedir}/src/main/typescript/generated</output>
                        <languages>
                            <language>Kotlin</language>
                        </languages>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

2. **Run maven:**

```bash
mvn wirespec:compile
```

## Compile

The `compile` goal is the primary functionality of the Wirespec Maven plugin. It compiles Wirespec definition files into code in your target languages.

### Usage

The compile mojo is already shown in the installation section above. Here's a more detailed example:

```xml
<plugin>
    <groupId>community.flock.wirespec.plugin.maven</groupId>
    <artifactId>wirespec-maven-plugin</artifactId>
    <version>{{WIRESPEC_VERSION}}</version>
    <executions>
        <execution>
            <id>wirespec-compile</id>
            <goals>
                <goal>compile</goal>
            </goals>
            <configuration>
                <input>${project.basedir}/src/main/wirespec</input>
                <output>${project.build.directory}/generated-sources/wirespec</output>
                <languages>
                    <language>Java</language>
                    <language>Kotlin</language>
                    <language>TypeScript</language>
                </languages>
                <packageName>com.example.generated</packageName>
                <strict>true</strict>
                <shared>true</shared>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### Parameters

The compile mojo supports the following parameters:

- **input** (required): Specifies the input files or directories. Multiple paths can be provided, separated by commas. Files can also be loaded from the classpath using the 'classpath:' prefix (e.g., 'classpath:wirespec/petstore.ws').
- **output** (required): Specifies the output directory where generated code will be placed.
- **languages**: List of target languages to compile to. Supported languages are:
  - `Java`: Generate Java code
  - `Kotlin`: Generate Kotlin code
  - `TypeScript`: Generate TypeScript code
  - `Python`: Generate Python code
  - `Wirespec`: Generate Wirespec code (useful for transformations)
  - `OpenAPIV2`: Generate OpenAPI v2 specifications
  - `OpenAPIV3`: Generate OpenAPI v3 specifications
- **packageName**: Package name for the generated code. Default is 'generated'.
- **strict**: Whether to invoke strict mode during compilation. Default is 'true'.
- **shared**: Whether to emit shared Wirespec code. Default is 'true'.
- **emitterClass**: Specifies a custom emitter class to use for code generation.

### Running the Compile Goal

You can run the compile goal directly with:

```bash
mvn wirespec:compile
```

Or it will run automatically as part of the `generate-sources` phase during your normal build process.

## Convert

The `convert` goal allows you to convert files from other formats to Wirespec format and then generate code in your target languages. This is particularly useful for working with existing API specifications like OpenAPI.

### Usage

To use the convert mojo, add it to your `pom.xml` file with the `convert` goal:

```xml
<plugin>
    <groupId>community.flock.wirespec.plugin.maven</groupId>
    <artifactId>wirespec-maven-plugin</artifactId>
    <version>{{WIRESPEC_VERSION}}</version>
    <executions>
        <execution>
            <id>openapi</id>
            <goals>
                <goal>convert</goal>
            </goals>
            <configuration>
                <input>${project.basedir}/src/main/openapi/specification.json</input>
                <output>${project.build.directory}/generated-sources/openapi</output>
                <format>OpenAPIV3</format>
                <languages>
                    <language>Java</language>
                    <language>Kotlin</language>
                </languages>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### Parameters

In addition to the common parameters (input, output, languages), the convert mojo has the following specific parameters:

- **format** (required): Specifies the format to convert from. Supported formats are:
    - `OpenAPIV2`: OpenAPI Specification version 2.0 (formerly Swagger)
    - `OpenAPIV3`: OpenAPI Specification version 3.0
    - `Avro`: Apache Avro schema format

### Running the Convert Goal

You can run the convert goal directly with:

```bash
mvn wirespec:convert
```

Or it will run automatically as part of the `generate-sources` phase during your normal build process.

By default, all plugin goals (`compile` and `convert`) are bound to the `generate-sources` phase, and will
automatically run for any maven execution targeting a later phase (e.g. `mvn test` or `mvn install`)

## Advanced Configuration

### Source Directory

The `sourceDirectory` parameter allows you to specify a custom directory containing source files that need to be compiled before processing the Wirespec files. This is useful when you need to include custom preprocessors or other supporting code.

```xml
<configuration>
    <sourceDirectory>${project.basedir}/src/wirespec/kotlin</sourceDirectory>
    <!-- other configuration -->
</configuration>
```

When specified, the plugin will compile all Java and Kotlin files in this directory and add them to the classpath before processing the Wirespec files.

### Pre-Processing

The `preProcessor` parameter allows you to specify a class that preprocesses the input before it's converted by the Wirespec compiler. This class must implement a function from String to String and be available in the project's classpath.

```xml
<configuration>
    <preProcessor>com.example.MyPreProcessor</preProcessor>
    <!-- other configuration -->
</configuration>
```

The preprocessor class can be implemented in either Java or Kotlin:

**Kotlin Example:**
```kotlin
class MyPreProcessor : (String) -> String {
    override fun invoke(input: String): String {
        // Process the input and return the result
        return processedInput
    }
}
```

**Java Example:**
```java
public class MyPreProcessor implements Function<String, String> {
    @Override
    public String apply(String input) {
        // Process the input and return the result
        return processedInput;
    }
}
```

This is particularly useful when working with formats like OpenAPI, where you might want to filter or modify the specification before generating code.

## Bill of materials

Wirespec comes with a bill of materials (BOM). Importing this in the dependency manager of the `pom.xml` makes sure you
have access to a curated set of dependencies:

```xml

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>community.flock.wirespec</groupId>
            <artifactId>bom</artifactId>
            <version>{{WIRESPEC_VERSION}}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

```

:::caution

In a [Bill of Materials](https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html#Bill_of_Materials_.28BOM.29_POMs),
the `pluginManagement` section cannot be defined. As a result, even when using the BOM, you must explicitly specify a
version when adding the `wirespec-maven-plugin`.

:::
