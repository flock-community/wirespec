# Converters

Wirespec offers the capability to convert from existing specification formats, facilitating a smooth transition to Wirespec. Currently, it supports conversion from OpenAPI Specification (OAS) and Avro. Be aware that converting to Wirespec is a destructive.

## OpenAPI Specification (OAS)

Supports conversion form OAS v2 and v3. In the conversion all speciefied paths are converted to endpoints with the coresponding schemas which are converted to Wirespec types. Some notes about the conversion

* **allOf**: spreads all properties into one type
* **anyOf**: converts to a Wirespec union
* **oneOf**: converts to a Wirespec union

Not converted:

* Example
* Link
* Discriminator
* XML
* Security

[Playground convert OAS](https://playground.wirespec.io/converter?format=)

## Avro

Wire
