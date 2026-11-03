# Converters

Wirespec offers the capability to convert from existing specification formats. Currently, it supports conversion from
OpenAPI Specification (OAS), Avro, GraphQL and AsyncAPI.

> **Warning**
> Converting OpenAPI and Avro to Wirespec can lose information. GraphQL and AsyncAPI convert without loss.

## Supported Formats

- [OpenAPI (OAS)](openapi.md) - Support for OAS v2 and v3.
- [Avro](avro.md) - Support for Avro schemas.
- [GraphQL](graphql.md) - Support for GraphQL schemas (SDL), without loss.
- [AsyncAPI](asyncapi.md) - Support for AsyncAPI v2 and v3 documents, without loss.

## Annotations

Where Wirespec has no syntax for something in a document, the converter keeps it in an annotation, so it can be written
back when Wirespec is converted into that format. Each converter lists its annotations:
[OpenAPI](openapi.md#annotations), [GraphQL](graphql.md#annotations) and [AsyncAPI](asyncapi.md#annotations). The Avro
converter has none.

## Name and version

Converting Wirespec into a specification takes a context: the name and the version of the specification. Each format
writes them where it has room for them, and the name also names the file.

| Output   | Language                 | Name                                                      | Version        | File without a name |
|----------|--------------------------|-----------------------------------------------------------|----------------|---------------------|
| OpenAPI  | `OpenAPIV2`, `OpenAPIV3` | `info.title`, and the file `<name>.json`                  | `info.version` | `OpenAPI.json`      |
| AsyncAPI | `AsyncAPI`               | `info.title`, and the file `<name>.json`                  | `info.version` | `asyncapi.json`     |
| Avro     | `Avro`                   | the file `<name>.avsc`                                    | -              | `schema.avsc`       |
| Avro IDL | `AvroIdl`                | the protocol `<Name>Protocol`, and the file `<name>.avdl` | -              | `schema.avdl`       |
| GraphQL  | `GraphQL`                | the file `<name>.graphql`                                 | -              | `schema.graphql`    |

Without a name or version, OpenAPI and AsyncAPI get the title `Wirespec` and the version `0.0.0`, and Avro IDL names
the protocol after the Wirespec file. An AsyncAPI document that was converted to Wirespec keeps its own title and
version, unless the context gives them: then those change, and the rest of its `info` stays as it was.

Every plugin takes them: `--spec-name` and `--spec-version` in the
[CLI](../plugins/plugins-cli.md#specification-name-and-version), `specName` and `specVersion` in the
[Maven](../plugins/plugins-maven.md#parameters) and [Gradle](../plugins/plugins-gradle.md#specification-name-and-version)
plugins, and the last two arguments of `emit` in the [npm package](../plugins/plugins-npm.md#programmatically). They
apply to `compile` and `convert` alike: both emit a specification when the language is one of the above.
