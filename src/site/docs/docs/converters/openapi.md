# OpenAPI Specification (OAS)

Wirespec supports conversion from OAS v2 and v3. During conversion, all specified paths are converted to endpoints, with
corresponding schemas transformed into Wirespec types.

## Type Conversion

Primitve types are mapped as follows:

| OAS Type | Wirespec Type | Notes |
|---|---|---|
| `string` | `String` | Pattern constraints are preserved as RegExp |
| `integer` | `Integer` | `int32` maps to 32-bit, `int64` (and default) maps to 64-bit |
| `number` | `Number` | `float` maps to 32-bit, `double` (and default) maps to 64-bit |
| `boolean` | `Boolean` | |
| `array` | `Type[]` | Converted to an Iterable of the item type |

## Composition

Wirespec handles OAS composition keywords:

- **allOf**: Spreads all properties from the combined schemas into a single flattened Wirespec type.
- **anyOf**: Converts to a Wirespec `Union` type.
- **oneOf**: Converts to a Wirespec `Union` type.

## Unsupported Constructs

The following OpenAPI constructs are currently **not** converted or ignored during the process:

- **Example**
- **Link**
- **Discriminator**
- **XML**
- **Security** definitions

## Descriptions

When generating OpenAPI from Wirespec, you can add descriptions to your types and fields using the `@Description` annotation.

```wirespec
@Description("This is a user")
type User {
  @Description("The user's name")
  name: String
}
```

This will be converted to the `description` field in the generated OpenAPI specification.

## Playground

You can try the conversion online: [Playground convert OAS](https://playground.wirespec.io/?emitter=open_api_v3&specification=wirespec)
