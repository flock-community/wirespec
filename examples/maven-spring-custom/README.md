# Example: How to use a custom Emitter with Wirespec

The custom emitter in the `emitter` module is built on the Wirespec IR
pipeline: it extends the standard `JavaIrEmitter` and replaces the IR `File`
produced for every Wirespec definition with a minimal custom class. Because it
works on the language-neutral IR, a custom emitter can reshape, extend, or
completely replace the generated tree before the code generator turns it into
source.

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
                <output>${project.build.directory}/generated-sources/java/hello</output>
                <emitterClass>community.flock.wirespec.example.maven.custom.emit.CustomEmitter</emitterClass>
            </configuration>
        </execution>
    </executions>
    <dependencies>
        <dependency>
            <groupId>community.flock.wirespec.example.maven</groupId>
            <artifactId>emitter</artifactId>
            <version>${project.version}</version>
        </dependency>
    </dependencies>
</plugin>
```

Find the [actual pom.xml](app/pom.xml) in the `app` module and the
[custom emitter](emitter/src/main/java/community/flock/wirespec/example/maven/custom/emit/CustomEmitter.java)
in the `emitter` module.
