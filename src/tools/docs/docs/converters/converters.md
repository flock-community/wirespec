# Converters

Wirespec offers the capability to convert from existing specification formats. Currently, it supports conversion from
OpenAPI Specification (OAS) and Avro. Be aware that converting to Wirespec is a destructive operation.

## OpenAPI Specification (OAS)

Wirespec supports conversion from OAS v2 and v3. During conversion, all specified paths are converted to endpoints, with
corresponding schemas transformed into Wirespec types. Here are some key points about the conversion process:

* **allOf**: spreads all properties into one type
* **anyOf**: converts to a Wirespec union
* **oneOf**: converts to a Wirespec union

The following OpenAPI constructs are **not** converted:

* **Example**
* **Link**
* **Discriminator**
* **XML**
* **Security**

[Playground convert OAS](https://playground.wirespec.io/converter?format=open_api_v3)

## Avro

Avro schemas can be converted to Wirespec types. Please note the following considerations:

* **Defaults**: Default values are not supported

[Playground convert Avro](https://playground.wirespec.io/converter?format=avro)
