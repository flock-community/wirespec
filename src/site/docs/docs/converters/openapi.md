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

## Links

OpenAPI v3 [response links](https://spec.openapis.org/oas/v3.1.0#link-object) round-trip through the `@Link` annotation on a response variant. One annotation describes one outgoing link.

```wirespec
type User { id: String, name: String }

endpoint CreateUser POST User /users -> {
    @Link("GetUser",
          operationId: "GetUserById",
          parameters: {id: "$response.body#/id"},
          description: "Fetch the just-created user")
    @Link("DeleteUser",
          operationId: "DeleteUser",
          parameters: {id: "$response.body#/id"})
    201 -> User
}
```

| Parameter      | Required | Description                                                      |
|----------------|----------|------------------------------------------------------------------|
| (positional)   | yes      | The link's name — the key in the response's `links` map.         |
| `operationId`  | one of   | The `operationId` of the target operation.                       |
| `operationRef` | these    | A URI reference to an operation, including in another spec.      |
| `parameters`   | no       | Map of parameter name → OpenAPI runtime expression.              |
| `requestBody`  | no       | OpenAPI runtime expression for the body of the next request.     |
| `description`  | no       | Free-text description of the link.                               |
| `server`       | no       | URL of the server the next call should target.                   |

Runtime expressions (`$response.body#/...`, `$request.path.id`, etc.) are kept verbatim — Wirespec does not validate or rewrite them. The annotation is preserved on parse and re-emitted on the way back to OpenAPI; language emitters (Kotlin, Java, TypeScript, Python, Rust) ignore it.

## Playground

You can try the conversion online: [Playground convert OAS](https://playground.wirespec.io/?emitter=open_api_v3&specification=wirespec)
