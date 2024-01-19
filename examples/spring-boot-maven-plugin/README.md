# Example: How to use the Wirespec Maven Plugin

## Wirespec Maven Plugin Configuration

```xml

<plugin>
    <groupId>community.flock.wirespec.plugin.maven</groupId>
    <artifactId>wirespec-maven-plugin</artifactId>
    <version>0.0.0-SNAPSHOT</version>
    <executions>
        <execution>
            <id>typescript</id>
            <goals>
                <goal>generate</goal>
            </goals>
            <configuration>
                <input>${project.basedir}/src/main/wirespec</input>
                <output>${project.basedir}/src/main/typescript/generated</output>
                <languages>
                    <language>TypeScript</language>
                </languages>
            </configuration>
        </execution>
        <execution>
            <id>java</id>
            <goals>
                <goal>generate</goal>
            </goals>
            <configuration>
                <input>${project.basedir}/src/main/wirespec</input>
                <output>${project.build.directory}/generated-sources</output>
                <packageName>community.flock.wirespec.generated.java</packageName>
                <languages>
                    <language>Java</language>
                </languages>
            </configuration>
        </execution>
    </executions>
</plugin>
```

According to the [actual pom.xml](pom.xml) file.
