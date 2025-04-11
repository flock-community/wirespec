[![Maven Central](https://img.shields.io/maven-central/v/community.flock.wirespec.compiler/lib)](https://mvnrepository.com/artifact/community.flock.wirespec.compiler/core-jvm)
[![Apache 2.0 License](https://img.shields.io/badge/License-Apache_2.0-blue)](LICENSE)
![GitHub Workflow Status (with event)](https://img.shields.io/github/actions/workflow/status/flock-community/wirespec/build.yml)

<div align="center">
<h1>
  <br>
  <a href="https://github.com/flock-community/wirespec"><img src="./images/wirespec-logo.svg" alt="" width="200"/></a>
  <br>
    Wirespec
  <br>
</h1>
<subtitle>
Simplify your API development workflows, accelerate implementation, and guarantee strict adherence to defined contract specifications
</subtitle>
</div>


## Introduction


Wirespec is a modern tool that enhances software development by streamlining the process of designing, documenting, and implementing APIs.
While the software industry offers numerous solutions for designing contracts between services, Wirespec distinguishes itself by using a simple language model and multi-language compatibility.

Here are some key reasons why you might want to use Wirespec:

1. Simplified API Design through Human-Readable contracts
    * Wirespec provides a clear, structured approach to defining APIs, making it easier to design endpoints, data models, and interactions.
    * By focusing on a consistent specification, it reduces ambiguity in communication between teams.

2. Reduced Development Time
    * With its automated tools and predefined workflows, Wirespec allows developers to focus on business logic rather than repetitive tasks.
    * By catching issues early in the design phase, it reduces costly revisions during later stages of development.
    * By offering platform-native tools, it enables a seamless development experience.

3. Future-Proof Development
    * As software systems grow more complex, having a robust, standardized approach to API development ensures scalability and maintainability.
    * Wirespec facilitates the generation of the required code to enable secure and consistent data transfer across various programming languages. This eliminates the need for extensive API specifications defined as JSON schemas, and minimizes unnecessary back-and-forth between teams, and simplify communication across microservices.
    * Since code generated by Wirespec has no dependencies on other libraries or frameworks, it ensures maximum stability and reliability.

In summary, using Wirespec during software development leads to faster, more reliable, and collaborative API creation while reducing errors and improving overall project efficiency.
## Wirespec playground

You can explore and experiment with Wirespec by using the [Wirespec playground](https://playground.wirespec.io). Whether you are new to Wirespec or a seasoned user, the playground makes is simple to design, test and improve your API specifications- all in one place.

## Wirespec compared

By understanding your project's specific needs and architecture, you can choose the most suitable specification tool to streamline development and improve collaboration.
#

| Feature/Aspect             | **Wirespec**                       | **OpenAPI**         | **AsyncAPI**      | **TypeSpec**               |
|----------------------------|------------------------------------|---------------------|-------------------|----------------------------|
| **Primary Focus**          | Streamlined API design             | RESTful APIs        | Asynchronous APIs | Programmatic API design    |
| **Specification Format**   | Minimal, Wirespec syntax           | YAML/JSON           | YAML/JSON         | TypeScript-like syntax     |
| **Ecosystem Support**      | Emerging                           | Mature and extensive | Growing rapidly   | Emerging                   |
| **Code Generation**        | Built-in, cross-language           | Extensive via tools | Robust via tools  | Flexible and customizable  |
| **Best for Microservices** | Excellent                          | Good                | Excellent         | Good                       |
| **Asynchronous Support**   | Limited                            | Limited             | Excellent         | Limited                    |
| **Ease of Use**            | High (minimalist)                  | Moderate (can be verbose) | Moderate          | Moderate (requires coding) |
| **Technology**             | Multiplatform (JVM Node.js Binary) | JVM                 | Node.js           | Node.js                    |

Wirespec can read and convert OpenAPISpecification (OAS) files.


## Usage

Wirespec files can be compiled into language specific binding by using the cli

```shell
wirespec compile ./todo.ws -o ./tmp -l Kotlin
```

## Plugins

* Maven
* Gradle

## Extentions

* IntelliJ IDEA
* Visual Studio Code

## Integration

Wirespec offers integration libraries with differ libraries.

* [Jackson](src/integration/jackson)

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

```shell
wirespec -h
```

```
Usage: wirespec options_list
Subcommands: 
    compile - Compile Wirespec
    convert - Convert from OpenAPI

Arguments: 
    input -> Input file { String }

Options: 
    --output, -o -> Output directory { String }
    --debug, -d [false] -> Debug mode 
    --languages, -l -> Language type { Value should be one of [Java, Kotlin, Scala, TypeScript, Wirespec] }
    --packageName, -p [community.flock.wirespec.generated] -> Package name { String }
    --strict, -s [false] -> Strict mode 
    --help, -h -> Usage info 
```

```shell
wirespec convert -h
```

```
Usage: wirespec convert options_list
Arguments: 
    input -> Input file { String }
    format -> Input format { Value should be one of [openapiv2, openapiv3] }
```

# Plugins

Other examples can be found [here](examples/README.md)

# Quick Start with this repository

## Dependencies

* JDK 21
* Node 20
* Docker

Clone this repository and run (n *nix systems):

```shell
make all
```

to compile the project and test the Wirespec compiler with definitions found in `types`. Locate the result
in `types/out`

# Releases

A release can be made using GitHub the UI.
Go to https://github.com/flock-community/wirespec/releases/new

![release](images/release.png)

- Create a tag according to the following pattern `v*.*.*`
- Enter the release title `Release *.*.*`
- Click `Publish release`
