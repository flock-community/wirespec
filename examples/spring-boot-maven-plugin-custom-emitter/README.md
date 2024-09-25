# Example: How to use a custom Emitter with Wirespec
## Wirespec Maven Plugin Configuration
```xml
<plugin>
    <groupId>community.flock.wirespec.plugin.maven</groupId>
    <artifactId>wirespec-maven-plugin</artifactId>
    <version>${wirespec-maven-plugin.version}</version>
    <executions>
        <execution>
            <id>custom</id>
            <goals>
                <goal>custom</goal>
            </goals>
            <configuration>
                <input>${project.basedir}/src/main/wirespec</input>
                <output>${project.build.directory}/generated-sources/java/hello</output>
                <emitterClass>community.flock.wirespec.emit.CustomEmitter</emitterClass>
                <extention>java</extention>
                <split>true</split>
            </configuration>
        </execution>
    </executions>
    <dependencies>
        <dependency>
            <groupId>community.flock.wirespec.example.maven_plugin_custom</groupId>
            <artifactId>emitter</artifactId>
            <version>x.x.x</version>
        </dependency>
    </dependencies>
</plugin>
```
Find the [actual pom.xml](app/pom.xml) in the `app` module and the
[custom emitter](emitter/src/main/java/community/flock/wirespec/emit/CustomEmitter.java) in the `emitter` module
