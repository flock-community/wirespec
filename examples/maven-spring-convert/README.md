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
