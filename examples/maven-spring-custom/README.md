# Example: How to use a custom IR extension with Wirespec

The `extension` module contains a custom `IrExtension`. Before generating code,
Wirespec lowers every definition into a language-neutral intermediate
representation (IR). An extension receives that complete IR together with the
parsed AST and returns the IR that is handed to the code generator, so it can
add, drop, or reshape any element without forking an emitter.

`CustomExtension` appends a minimal `<Definition>Custom` class for every
Wirespec definition next to the Java models the built-in emitter produces. The
plugin instantiates the extension and injects the configured `packageName`
into its constructor.

Extensions are written in Kotlin: the IR is an Arrow `NonEmptyList`, a Kotlin
value class that Java code cannot implement an interface against.

## Wirespec Maven Plugin Configuration

```xml
<plugin>
    <groupId>community.flock.wirespec.plugin.maven</groupId>
    <artifactId>wirespec-maven-plugin</artifactId>
    <version>${wirespec.version}</version>
    <executions>
        <execution>
            <id>custom</id>
            <goals>
                <goal>compile</goal>
            </goals>
            <configuration>
                <input>${project.basedir}/src/main/wirespec</input>
                <output>${project.build.directory}/generated-sources/java</output>
                <packageName>community.flock.wirespec.example.maven.custom.generated</packageName>
                <shared>false</shared>
                <languages>
                    <language>Java</language>
                </languages>
                <extensionClasses>
                    <extensionClass>community.flock.wirespec.example.maven.custom.extension.CustomExtension</extensionClass>
                </extensionClasses>
            </configuration>
        </execution>
    </executions>
    <dependencies>
        <dependency>
            <groupId>community.flock.wirespec.example.maven</groupId>
            <artifactId>extension</artifactId>
            <version>${project.version}</version>
        </dependency>
    </dependencies>
</plugin>
```

Find the [actual pom.xml](app/pom.xml) in the `app` module and the
[custom extension](extension/src/main/kotlin/community/flock/wirespec/example/maven/custom/extension/CustomExtension.kt)
in the `extension` module.
