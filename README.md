# Wirespec
Type safe wires made easy

## Introduction
Wirespec is a typesafe language to specify data transfer models which are exchanged between services. These models can be transformed into bindings for a specific language (Typescript, Java, Kotlin, Scala).

## Dependencies
* JDK 17
* Node 16
* Docker
## Quick Start
On *nix systems run:
```shell
make all
```
to compile the project and test the Wirespec compiler with definitions found in
`types`. Locate the result in `types/out`

# Install
Instructions on how to install different components

## Cli

### Linux
```
curl -L https://github.com/flock-community/wirespec/releases/latest/download/linuxX64.kexe -o wirespec
```

### macOS
```
curl -L https://github.com/flock-community/wirespec/releases/latest/download/macosX64.kexe -o wirespec
```

### macOS Arm
```
curl -L https://github.com/flock-community/wirespec/releases/latest/download/macosArm64.kexe -o wirespec
```

```
chmod +x wirespec
sudo mv ./wirespec /usr/local/bin/wirespec
```

## Maven
Example how to use the maven plugin  
For a full example click [here](examples/spring-boot-maven-plugin)
```xml
<project>
    ...
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
                            <sourceDirectory>${project.basedir}/src/main/wirespec</sourceDirectory>
                            <targetDirectory>${project.build.directory}/generated-sources</targetDirectory>
                        </configuration>
                    </execution>
                    <execution>
                        <id>typescript</id>
                        <goals>
                            <goal>typescript</goal>
                        </goals>
                        <configuration>
                            <sourceDirectory>${project.basedir}/src/main/wirespec</sourceDirectory>
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
For a full example click [here](examples/spring-boot-gradle-plugin)
```kotlin
plugins {
    ...
    id("community.flock.wirespec.plugin.gradle") version "0.0.0"
}

wirespec {
    sourceDirectory = "$projectDir/src/main/wirespec"
    kotlin {
        targetDirectory = "$buildDir/generated/main/kotlin"
    }
    typescript {
        targetDirectory = "$projectDir/src/main/frontend/generated"
    }
}
```

## Release process
A release can be made using github the UI. 
Go to https://github.com/flock-community/wirespec/releases/new

![release](images/release.png)

- Create a tag according to the following pattern `v*.*.*`
- Enter the release title `Release *.*.*`
- Click `Publish release`
