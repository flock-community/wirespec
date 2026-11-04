[![Maven Central](https://img.shields.io/maven-central/v/community.flock.wirespec.compiler/core)](https://mvnrepository.com/artifact/community.flock.wirespec.compiler/core-jvm)
[![Apache 2.0 License](https://img.shields.io/badge/License-Apache_2.0-blue)](LICENSE)
![GitHub Workflow Status (with event)](https://img.shields.io/github/actions/workflow/status/flock-community/wirespec/build.yml)

<div align="center">
<h1>
  <br>
  <a href="https://wirespec.io"><img src="./images/wirespec-logo.svg" alt="" width="200"/></a>
  <br>
    Wirespec
  <br>
</h1>
<p>
Simplify your API development workflows, accelerate implementation, and guarantee strict adherence to defined contract specifications
</p>
<p>
<a href="https://wirespec.io">Website</a> ·
<a href="https://wirespec.io/docs/getting-started">Documentation</a> ·
<a href="https://playground.wirespec.io">Playground</a> ·
<a href="https://wirespec.io/blog">Blog</a>
</p>
</div>

## What is Wirespec?

Wirespec is a small, human-readable language for describing APIs. You write the contract once, in a `.ws` file, and it
becomes the single source of truth for every team that builds or calls the API:

```wirespec
type Todo {
  id: Integer,
  title: String,
  done: Boolean
}

type ApiError {
  message: String
}

endpoint GetTodo GET /todos/{id: Integer} -> {
  200 -> Todo
  404 -> ApiError
}
```

From the contract, Wirespec generates typesafe, dependency-free code for each side of the API: TypeScript for the
frontend, and Kotlin, Java, Python, Rust or Scala for the backend. It can also publish the contract as OpenAPI or Avro,
and turn an existing OpenAPI, AsyncAPI, Avro or GraphQL specification into Wirespec.

Wirespec is built around three ideas, each explained in the documentation:

- **[Contract first](https://wirespec.io/docs/intro/intro-contract)**: agree on the interface before anyone implements
  it, independent of language or framework.
- **[Generate code](https://wirespec.io/docs/intro/intro-generate)**: derive models, endpoints and clients from the
  contract instead of writing them by hand.
- **[Validate the implementation](https://wirespec.io/docs/intro/intro-validate)**: test against the same contract
  with generated test data and mocks.

## Get started

Try Wirespec in the [Playground](https://playground.wirespec.io), no installation needed. To use it in a project,
[Getting Started](https://wirespec.io/docs/getting-started) has a guide for frontend engineers, backend engineers, API
designers and teams that already have a specification.

## Documentation

| Topic                                                 | What you will find                                                                 |
|-------------------------------------------------------|------------------------------------------------------------------------------------|
| [Language](https://wirespec.io/docs/language)         | The syntax: types, endpoints, channels, RPCs, annotations and default values       |
| [Emitters](https://wirespec.io/docs/emitters)         | The code and specifications generated for each target                              |
| [Plugins](https://wirespec.io/docs/plugins)           | Installing and configuring the CLI, Gradle, Maven and npm plugins; IR extensions   |
| [Converters](https://wirespec.io/docs/converters)     | Converting existing specifications to Wirespec                                     |
| [Integration](https://wirespec.io/docs/integration)   | Spring, Jackson, kotlinx.serialization, Avro, WireMock, Kotest and model builders  |
| [IDE](https://wirespec.io/docs/ide)                   | The IntelliJ IDEA and VS Code plugins, and the language server for other editors   |
| [Architecture](https://wirespec.io/docs/architecture) | How the compiler works, for contributors and extension authors                     |

## Examples

The [examples](examples) directory holds complete projects that use Wirespec with Maven, Gradle, npm, Cargo and sbt.

## Contributing

Contributions are welcome: [CONTRIBUTING.md](CONTRIBUTING.md) explains how to propose a feature, build the project and
open a pull request.
