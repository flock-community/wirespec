## Wirespec Maven Plugin Configuration
```xml
<plugin>
    <groupId>community.flock.wirespec.plugin.maven</groupId>
    <artifactId>wirespec-maven-plugin</artifactId>
    <version>0.0.0-SNAPSHOT</version>
    <dependencies>
        <dependency>
            <groupId>community.flock.wirespec.integration</groupId>
            <artifactId>avro-jvm</artifactId>
            <version>0.0.0-SNAPSHOT</version>
        </dependency>
    </dependencies>
    <executions>
        <execution>
            <id>java</id>
            <goals>
                <goal>custom</goal>
            </goals>
            <configuration>
                <input>${project.basedir}/src/main/wirespec</input>
                <output>${project.build.directory}/generated-sources</output>
                <emitterClass>community.flock.wirespec.integration.avro.java.emit.AvroEmitter</emitterClass>
                <shared>Java</shared>
                <extension>java</extension>
                <packageName>community.flock.wirespec.generated.examples.spring</packageName>
            </configuration>
        </execution>
    </executions>
</plugin>
```

Find the [actual pom.xml](app/pom.xml) in the `app` module and the
[custom emitter](emitter/src/main/java/community/flock/wirespec/emit/CustomEmitter.java) in the `emitter` module
