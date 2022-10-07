# WireSpec
Version 0.0.1
## Dependencies
* JDK 17
* Node 16
* Docker
## Quick Start
On *nix systems run:
```shell
make all
```
to compile the project and test the WireSpec compiler with definitions found in
`types`. Locate the result in `types/out`

## Maven
Example how to use the maven plugin
```xml
<project>
    <build>
        <plugins>
            ...
            <plugin>
                <groupId>community.flock.wirespec.plugin.maven</groupId>
                <artifactId>maven</artifactId>
                <version>0.0.1-SNAPSHOT</version>
                <executions>
                    <execution>
                        <id>kotlin</id>
                        <goals>
                            <goal>kotlin</goal>
                        </goals>
                        <configuration>
                            <sourceDirectory>${project.basedir}/src/main/wire-spec</sourceDirectory>
                            <targetDirectory>${project.build.directory}/generated-sources</targetDirectory>
                        </configuration>
                    </execution>
                    <execution>
                        <id>typescript</id>
                        <goals>
                            <goal>typescript</goal>
                        </goals>
                        <configuration>
                            <sourceDirectory>${project.basedir}/src/main/wire-spec</sourceDirectory>
                            <targetDirectory>${project.basedir}/src/main/frontend/generated</targetDirectory>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
            <plugin>
                <groupId>org.codehaus.mojo</groupId>
                <artifactId>build-helper-maven-plugin</artifactId>
                <version>3.0.0</version>
                <executions>
                    <execution>
                        <id>add-source</id>
                        <phase>generate-sources</phase>
                        <goals>
                            <goal>add-source</goal>
                        </goals>
                        <configuration>
                            <sources>
                                <source>${project.build.directory}/generated-sources</source>
                            </sources>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
            ...
        </plugins>
    </build>
</project>
```

## Gradle
Example how to use the gradle plugin
```
plugins {
    ...
    id("community.flock.wirespec.plugin.gradle") version "0.0.1-SNAPSHOT"
}

wirespec {
	kotlin {
		sourceDirectory = "$projectDir/src/main/wire-spec"
		targetDirectory = "$buildDir/generated/main/kotlin"
	}
	typescript {
		sourceDirectory = "$projectDir/src/main/wire-spec"
		targetDirectory = "$projectDir/src/main/frontend/generated"
	}
}
```