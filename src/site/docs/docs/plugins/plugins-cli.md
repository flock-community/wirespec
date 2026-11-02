---
title: Cli
slug: /plugins/cli
sidebar_position: 1
---

# Wirespec CLI

Wirespec provides a command-line interface (CLI) for various platforms, enabling you to compile Wirespec files and
convert between different formats.

## Installation

Follow the instructions below to install the Wirespec CLI on your operating system:

### Linux

```shell
curl -L https://github.com/flock-community/wirespec/releases/latest/download/linuxX64.kexe -o wirespec
chmod +x wirespec
sudo mv ./wirespec /usr/local/bin/wirespec
```

### macOs(X64)

```shell
curl -L https://github.com/flock-community/wirespec/releases/latest/download/macosX64.kexe -o wirespec
chmod +x wirespec
sudo mv ./wirespec /usr/local/bin/wirespec
```

### macOs(Arm64)

```shell
curl -L https://github.com/flock-community/wirespec/releases/latest/download/macosArm64.kexe -o wirespec
chmod +x wirespec
sudo mv ./wirespec /usr/local/bin/wirespec
```

## Use

After running these commands, the wirespec command will be available in your terminal.
You can verify the installation by running wirespec -h

```shell
Usage: wirespec [<options>] <command> [<args>]...

Options:
  -h, --help  Show this message and exit

Commands:
  compile
  convert

```

### compile

```shell
Usage: wirespec compile [<options>]

Options:
  -i, --input=<text>                                                          Input
  -o, --output=<text>                                                         Output
  -p, --package=<text>                                                        Package name
  --log-level=<text>                                                          Log level: DEBUG, INFO, WARN, ERROR
  --shared                                                                    Generate shared wirespec code
  --strict                                                                    Strict mode
  --ignore-defaults                                                           Leave field default values out of the emitted code
  -l, --language=(Java|Kotlin|TypeScript|Python|Rust|Scala|Wirespec|OpenAPIV2|OpenAPIV3|Avro|AvroIdl|GraphQL|AsyncAPI)
                                                                              Language
  -e, --extension=(Avro|Jackson|JvmBuilder|KotestDsl|KotlinxSerialization|SpringMappingAnnotations|SpringNativeHints)
                                                                              Extension, applied to the languages it supports
  --spec-name=<text>                                                          Name of the specification, and of its file: OpenAPIV2, OpenAPIV3, Avro, AvroIdl, GraphQL and AsyncAPI
  --spec-version=<text>                                                       Version of the specification: OpenAPIV2, OpenAPIV3 and AsyncAPI
  -h, --help                                                                  Show this message and exit
```

```shell
wirespec compile "type SomeType { someField: String }"
```

### convert

```shell
Usage: wirespec convert [<options>] <format>

Options:
  -i, --input=<text>                                                          Input
  -o, --output=<text>                                                         Output
  -p, --package=<text>                                                        Package name
  --log-level=<text>                                                          Log level: DEBUG, INFO, WARN, ERROR
  --shared                                                                    Generate shared wirespec code
  --strict                                                                    Strict mode
  --ignore-defaults                                                           Leave field default values out of the emitted code
  -l, --language=(Java|Kotlin|TypeScript|Python|Rust|Scala|Wirespec|OpenAPIV2|OpenAPIV3|Avro|AvroIdl|GraphQL|AsyncAPI)
                                                                              Language
  -e, --extension=(Avro|Jackson|JvmBuilder|KotestDsl|KotlinxSerialization|SpringMappingAnnotations|SpringNativeHints)
                                                                              Extension, applied to the languages it supports
  --spec-name=<text>                                                          Name of the specification, and of its file: OpenAPIV2, OpenAPIV3, Avro, AvroIdl, GraphQL and AsyncAPI
  --spec-version=<text>                                                       Version of the specification: OpenAPIV2, OpenAPIV3 and AsyncAPI
  -h, --help                                                                  Show this message and exit

Arguments:
  <format>  Input format
```

```shell
wirespec convert OpenAPIV2 "$(cat types/openapi/petstore.json)"
```

## Extensions

The [IR extensions](./plugins.md#ir-extensions) that Wirespec ships are bundled with the CLI. Add `-e` once per
extension; the names are case-insensitive. Each extension applies to the languages it supports, so here the Kotlin
models get Spring and Jackson annotations while the TypeScript is emitted as is:

```shell
wirespec compile -i ./wirespec -o ./out/kotlin -l Kotlin -e SpringMappingAnnotations -e Jackson
wirespec compile -i ./wirespec -o ./out/typescript -l TypeScript -e SpringMappingAnnotations -e Jackson
```

`-l` takes one language per run: `compile` defaults to `Kotlin` and `convert` to `Wirespec`. Run the command once per
language to generate several.

## Specification name and version

When the language is a specification, `--spec-name` and `--spec-version` name it and give it a version. The name is
also the name of the file:

```shell
wirespec compile -i ./wirespec -o ./out -l OpenAPIV3 --spec-name petstore --spec-version 1.2.0
```

This writes `./out/petstore.json`, with `petstore` as `info.title` and `1.2.0` as `info.version`. Without them, the
file is `OpenAPI.json`, with title `Wirespec` and version `0.0.0`. `convert` takes them too, for instance to convert
OpenAPI v2 to v3:

```shell
wirespec convert OpenAPIV2 -i ./petstore.json -o ./out -l OpenAPIV3 --spec-name petstore --spec-version 1.2.0
```

See [Name and version](../converters/converters.md#name-and-version) for what each format does with them.

## Default values

`compile` fails when a Wirespec file has [default values](../language/types.mdx#default-values) and a language that does
not support them is selected. `wirespec` prints the error and exits with status 1:

```shell
$ wirespec compile -i ./wirespec -o ./out -l Java
Java does not support default values, but these fields have one: Settings.name, Settings.retries. Remove the defaults, or leave them out of the generated code with the ignore defaults option: --ignore-defaults for the CLI, or ignoreDefaults in the Gradle and Maven plugins.
```

Add `--ignore-defaults` to generate the code without the defaults:

```shell
wirespec compile -i ./wirespec -o ./out -l Java --ignore-defaults
```

Every run compiles to one language, so the defaults stay for Kotlin when only the Java run ignores them:

```shell
wirespec compile -i ./wirespec -o ./out/kotlin -l Kotlin
wirespec compile -i ./wirespec -o ./out/java -l Java --ignore-defaults
```

`convert` never fails on defaults: languages without default support leave them out. An invalid default in a Wirespec
file is an error, also with `--ignore-defaults`. See [Default values](./plugins.md#default-values) for the details.
