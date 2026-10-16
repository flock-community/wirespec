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
back when Wirespec is converted into that format. [Annotations](../language/annotations.mdx) lists the annotations of
all converters.
