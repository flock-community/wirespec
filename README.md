[![Maven Central](https://img.shields.io/maven-central/v/community.flock.wirespec.compiler/lib)](https://mvnrepository.com/artifact/community.flock.wirespec.compiler/core-jvm)
![GitHub Workflow Status (with event)](https://img.shields.io/github/actions/workflow/status/flock-community/wirespec/build.yml)
[![Apache 2.0 License](https://img.shields.io/badge/License-Apache%202.0-orange)](LICENSE.d)

# Wirespec
Readable contracts and typesafe wires made easy

## Introduction
Wirespec is a typesafe language to specify endpoints and data transfer models that are exchanged between services.
These models and endpoints can then be compiled into bindings for a specific language (TypeScript, Java, Kotlin, Scala). 

![compile](images/wirespec-compile.png)

Wirespec can read and convert OpenApiSpecification (OAS) files.

![convert](images/wirespec-convert.png)

## Syntax
Wirespec knows four definitions: `refined`, `enum`, `type`, `endpoint`.

```
refined DEFINITION /REGEX/g

enum DEFINITION {
    ENTRY, ENTRY, ...
}

type DEFINITION {
    IDENTIFIER: REFERENCE
}

endpoint DEFINITION METHOD [INPUT_REFERENCE] PATH [? QUERY] [# HEADER] -> {
    STATUS -> REFERENCE
}

```

## Example
`todo.ws`
```wirespec
refined UUID /^[0-9a-fA-F]{8}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{12}$/g

type Todo {
    id: UUID,
    name: String,
    done: Boolean
}

type TodoInput {
    name: String,
    done: Boolean
}

type Error {
    code: String,
    description: String
}

endpoint GetTodoById GET /todos/{ id: UUID } -> {
    200 -> Todo[]
    404 -> Error
}

endpoint GetTodos GET /todos?{ done: Boolean? }#{ limit: Integer, offset: Integer } -> {    
    200 -> Todo[]
    404 -> Error
}

endpoint CreateTodo POST TodoInput /todos -> {
    200 -> Todo
    404 -> Error
}

endpoint UpdateTodo PUT TodoInput /todos/{id:UUID} -> {
    200 -> Todo
    404 -> Error
}

endpoint DeleteTodo DELETE /todos/{id:UUID} -> {
    200 -> Todo
    404 -> Error
}
```

Other examples can be found [here](examples/README.md)

## Usage
Wirespec files can be compiled into language specific binding by using the cli

```shell
wirespec ./todo.ws -o ./tmp -l Kotlin
```

## Dependencies
* JDK 17
* Node 16
* Docker

## Quick Start
On *nix systems run:

```shell
make all
```

to compile the project and test the Wirespec compiler with definitions found in `types`. Locate the result in `types/out`

# CLI
## Install
### Linux
```shell
curl -L https://github.com/flock-community/wirespec/releases/latest/download/linuxX64.kexe -o wirespec
chmod +x wirespec
sudo mv ./wirespec /usr/local/bin/wirespec
```

### macOS
```shell
curl -L https://github.com/flock-community/wirespec/releases/latest/download/macosX64.kexe -o wirespec
chmod +x wirespec
sudo mv ./wirespec /usr/local/bin/wirespec
```

### macOS Arm
```shell
curl -L https://github.com/flock-community/wirespec/releases/latest/download/macosArm64.kexe -o wirespec
chmod +x wirespec
sudo mv ./wirespec /usr/local/bin/wirespec
```
## Use
```
Usage: wirespec options_list
Arguments: 
    input -> Input file { String }
Options: 
    --debug, -d [false] -> Debug mode 
    --output, -o -> Output directory { String }
    --language, -l [Kotlin] -> Language type { Value should be one of [Java, Kotlin, Scala, TypeScript, Wirespec, OpenApiV2, OpenApiV3] }
    --format, -f [Wirespec] -> Input format { Value should be one of [wirespec, openapiv2, openapiv3] }
    --packageName, -p [community.flock.wirespec.generated] -> Package name { String }
    --strict, -s [true] -> Strict mode 
    --help, -h -> Usage info 

```
## Plugins
### Maven
Example how to use the maven plugin  
For a full example click [here](examples/spring-boot-maven-plugin/README.md)
It is also possible to create your custom emitter and run with the plugin [here](examples/spring-boot-custom-maven-plugin/README.md)
```xml
<project>
    ...
    <build>
        <plugins>
            ...
            <plugin>
                <groupId>community.flock.wirespec.plugin.maven</groupId>
                <artifactId>wirespec-maven-plugin</artifactId>
                <version>0.0.1-SNAPSHOT</version>
                <executions>
                    <execution>
                        <id>kotlin</id>
                        <goals>
                            <goal>generate</goal>
                        </goals>
                        <configuration>
                            <input>${project.basedir}/src/main/wirespec</input>
                            <output>${project.build.directory}/generated-sources</output>
                            <languages>
                                <language>Kotlin</language>
                            </languages>
                        </configuration>
                    </execution>
                    <execution>
                        <id>typescript</id>
                        <goals>
                            <goal>generate</goal>
                        </goals>
                        <configuration>
                            <input>${project.basedir}/src/main/wirespec</input>
                            <output>${project.basedir}/src/main/frontend/generated</output>
                            <languages>
                                <language>TypeScript</language>
                            </languages>
                        </configuration>
                    </execution>
                    <execution>
                        <id>custom</id>
                        <goals>
                            <goal>custom</goal>
                        </goals>
                        <configuration>
                            <input>${project.basedir}/src/main/wirespec</input>
                            <output>${project.build.directory}/generated-sources</output>
                            <emitterClass>community.flock.wirespec.emit.CustomEmitter</emitterClass>
                            <extention>java</extention>
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

### Gradle
Example how to use the gradle plugin  
For a full example click [here](examples/spring-boot-gradle-plugin)
```kotlin
plugins {
    id("community.flock.wirespec.plugin.gradle") version "0.0.0"
}

wirespec {
    input = "$projectDir/src/main/wirespec"
    kotlin {
        output = "$buildDir/generated/main/kotlin"
    }
    typescript {
        output = "$projectDir/src/main/frontend/generated"
    }
}
```

## Integration
Some notes on how Wirespec integrates with different libraries and frameworks

### Jackson (json object mapper)
For some languages Wirespec is sanitizing enums names because of usage of preserved keywords and forbidden characters.
This results into problems with serialization. In Jackson the following configuration can be used to fix this.

```kotlin
ObjectMapper()
  .enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING)
  .enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)
```

## Releases
A release can be made using GitHub the UI. 
Go to https://github.com/flock-community/wirespec/releases/new

![release](images/release.png)

- Create a tag according to the following pattern `v*.*.*`
- Enter the release title `Release *.*.*`
- Click `Publish release`
