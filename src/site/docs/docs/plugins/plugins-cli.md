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
  -l, --language=(Java|Kotlin|TypeScript|Python|Wirespec|OpenAPIV2|OpenAPIV3) Language
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
  -l, --language=(Java|Kotlin|TypeScript|Python|Wirespec|OpenAPIV2|OpenAPIV3) Language
  -h, --help                                                                  Show this message and exit

Arguments:
  <format>  Input format
```

```shell
wirespec convert OpenAPIV2 "$(cat types/openapi/petstore.json)"
```
