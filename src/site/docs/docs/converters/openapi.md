# OpenAPI Specification (OAS)

Wirespec supports conversion from OAS v2 and v3. During conversion, all specified paths are converted to endpoints, with
corresponding schemas transformed into Wirespec types.

Conversion in the other direction covers types, enums, unions, refined types and endpoints. OAS describes HTTP APIs, so
[channels](../language/channels.mdx) and [rpcs](../language/rpc.mdx) have no OpenAPI counterpart and are skipped by the
OpenAPI emitters.

## Type Conversion

Primitve types are mapped as follows:

| OAS Type  | Wirespec Type | Notes                                                                                                                                                         |
|-----------|---------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `string`  | `String`      | A `pattern` is kept as a regular expression; otherwise `date`, `date-time`, `time`, `email` and `uuid` become [string formats](../language/types.mdx#refined) |
| `integer` | `Integer`     | `int32` maps to 32-bit, `int64` (and default) maps to 64-bit                                                                                                  |
| `number`  | `Number`      | `float` maps to 32-bit, `double` (and default) maps to 64-bit                                                                                                 |
| `boolean` | `Boolean`     |                                                                                                                                                               |
| `array`   | `Type[]`      | Converted to an Iterable of the item type                                                                                                                     |

## Composition

Wirespec handles OAS composition keywords:

- **allOf**: Spreads all properties from the combined schemas into a single flattened Wirespec type.
- **anyOf**: Converts to a Wirespec `Union` type.
- **oneOf**: Converts to a Wirespec `Union` type.

## Defaults

The `default` of a property schema is converted to a Wirespec [default value](../language/types.mdx#default-values), and
the OpenAPI emitters write a field's default back to the property schema. This works for OAS v2 and v3.

```json
{
  "type": "object",
  "required": ["name"],
  "properties": {
    "name": { "type": "string", "default": "anonymous" },
    "retries": { "type": "integer", "default": 3 },
    "status": { "type": "string", "enum": ["ACTIVE", "INACTIVE"], "default": "ACTIVE" }
  }
}
```

```wirespec
type Settings {
  name: String = "anonymous",
  retries: Integer? = 3,
  status: SettingsStatus? = ACTIVE
}

enum SettingsStatus {
  ACTIVE, INACTIVE
}
```

Some defaults are not converted:

- Defaults of arrays, objects and references, because Wirespec only supports defaults on primitive and enum fields. The
  default of an inline enum property is converted, as long as it is one of the enum's values.
- Defaults that do not match the property's type, such as `"default": "false"` on a `boolean`.
- Defaults of query, path and header parameters, because Wirespec only allows defaults on the fields of a `type`.
- A `null` default. A property that is not `required` already becomes a nullable field, so the emitters leave
  `default: null` out.
- The default of an enum field, when writing OpenAPI. Wirespec emits an enum field as a `$ref`, and OpenAPI 3.0
  ignores anything next to a `$ref`.

## Unsupported Constructs

The following OpenAPI constructs are currently **not** converted or ignored during the process:

- **Example**
- **Discriminator**
- **XML**

## Annotations

Annotations carry what Wirespec has no syntax for. `@Description` is generic: every converter and emitter reads it.
The annotations that only mean something to OpenAPI start with `OpenAPI`.

| Annotation                                       | On                                        | Carries                                                                            |
|--------------------------------------------------|-------------------------------------------|------------------------------------------------------------------------------------|
| `@Description("...")`                            | endpoints, responses, definitions, fields | the description of an operation, response, schema, property, parameter or header  |
| `@spec:Description("...")`                       | the spec                                  | `info.description`                                                                 |
| `@spec:OpenAPIInfo(title: "...", version: "...")` | the spec                                  | the rest of `info` (see [Info](#info))                                             |
| `@OpenAPIServer("https://...")`                  | the spec, endpoints                       | a server of the document, or of one operation (see [Servers](#servers))            |
| `@spec:OpenAPISecurityScheme("name", ...)`       | the spec                                  | a security scheme in `components.securitySchemes` (see [Security](#security))     |
| `@OpenAPISecurity(...)`                          | the spec, endpoints                       | a security requirement: the default of the document, or of one operation           |
| `@OpenAPINoSecurity`                             | endpoints                                 | `security: []`: an operation anyone may call                                       |
| `@OpenAPITag("...")`                             | the spec, endpoints                       | a tag of an operation, or a tag of the document with its description (see [Tags](#tags)) |
| `@OpenAPILink("GetUser", operationId: "...")`    | responses                                 | a response link of OpenAPI 3, one annotation per link (see [Links](#links))        |

_Definitions_ are types, enums and unions, and _fields_ are the fields of a type, the parameters and headers of an
endpoint and the headers of a response.

_The spec_ means the [file header](../language/language.mdx#the-file-header): an annotation written as `@spec:Name` at
the top of a file applies to the whole specification, every file compiled together. `@file:` annotations, which apply
to one file, mean nothing to OpenAPI: the emitters reject `@file:OpenAPI…` annotations.

:::warning
`@Link` is now called `@OpenAPILink`. Rename it in your specifications: the OpenAPI emitters no longer read `@Link`.
:::

## Descriptions

The description of an operation, response, schema, property, parameter or header becomes a `@Description`, and the
OpenAPI emitters write it back to the `description` field. A type or endpoint without `@Description` gets its comment
as description, and in OpenAPI 3 so does an enum or union.

```wirespec
@Description("This is a user")
type User {
  @Description("The user's name")
  name: String
}
```

The description of the document itself, `info.description`, becomes a `@spec:Description`.

## Metadata of the document

The `info`, `servers`, `security`, `tags` and `components.securitySchemes` of a document describe the API as a whole.
They are written as `@spec:` annotations in the header of any file of the spec. A file may hold nothing but such a
header, which is a good place for them:

```wirespec title="api.ws"
@spec:Description("Everything about customers")
@spec:OpenAPIInfo(title: "Customer API", version: "2.3.0")
@spec:OpenAPIServer("https://api.example.com/v2", description: "Production")
@spec:OpenAPIServer("https://api.test.example.com/v2", description: "Test")
@spec:OpenAPISecurityScheme("bearerAuth", type: "http", scheme: "bearer", bearerFormat: "JWT")
@spec:OpenAPISecurity("bearerAuth")
```

Other files add what belongs to them, such as the tags of their endpoints:

```wirespec title="customers.ws"
@spec:OpenAPITag("Customers", description: "Manage customers")

type Customer {
  id: String,
  name: String
}

@OpenAPITag("Customers")
endpoint GetCustomer GET /customers/{id: String} -> {
  200 -> Customer
}
```

```wirespec title="monitoring.ws"
@spec:OpenAPITag("Monitoring", description: "Health checks")

@OpenAPITag("Monitoring")
@OpenAPINoSecurity
endpoint Health GET /health -> {
  200 -> Unit
}
```

The OpenAPI 3 emitter writes this metadata as:

```json
{
  "openapi": "3.0.0",
  "servers": [
    { "url": "https://api.example.com/v2", "description": "Production" },
    { "url": "https://api.test.example.com/v2", "description": "Test" }
  ],
  "components": {
    "schemas": { "Customer": { "...": "..." } },
    "securitySchemes": {
      "bearerAuth": { "type": "http", "scheme": "bearer", "bearerFormat": "JWT" }
    }
  },
  "info": { "title": "Customer API", "description": "Everything about customers", "version": "2.3.0" },
  "paths": {
    "/customers/{id}": { "get": { "tags": ["Customers"], "operationId": "GetCustomer", "...": "..." } },
    "/health": { "get": { "tags": ["Monitoring"], "operationId": "Health", "security": [], "...": "..." } }
  },
  "security": [{ "bearerAuth": [] }],
  "tags": [
    { "name": "Customers", "description": "Manage customers" },
    { "name": "Monitoring", "description": "Health checks" }
  ]
}
```

`GetCustomer` has no `security` of its own, so it needs the bearer token of the document. `Health` needs nothing.

### Info

`@spec:OpenAPIInfo` takes the fields of the [Info Object](https://spec.openapis.org/oas/v3.0.3#info-object), except
the description, which is a `@spec:Description`:

```wirespec
@spec:OpenAPIInfo(
  title: "Customer API",
  version: "2.3.0",
  termsOfService: "https://example.com/terms",
  contact: { name: "Team", url: "https://example.com", email: "team@example.com" },
  license: { name: "Apache 2.0", url: "https://www.apache.org/licenses/LICENSE-2.0" }
)
```

Without it, the title is `Wirespec` and the version `0.0.0`. A build can set the title and version instead, for example
to stamp its own version on the document, see [the title and version of the spec](../plugins/plugins.md#the-title-and-version-of-the-spec).

### Servers

`@OpenAPIServer` takes the URL and the fields of the [Server Object](https://spec.openapis.org/oas/v3.0.3#server-object).
On the spec it is a server of the document, of which code generators usually take the first as the default base URL.
On an endpoint it is a server of that operation only:

```wirespec
@spec:OpenAPIServer("https://{env}.example.com/v2", description: "Test", variables: { env: { default: "test", enum: ["test", "acc"] } })

@OpenAPIServer("https://files.example.com/v2")
endpoint UploadFile PUT Bytes /files/{name: String} -> {
  201 -> Unit
}
```

### Tags

`@OpenAPITag` on an endpoint adds a tag to its operation; repeat it for several tags. Code generators such as
OpenAPI Generator group the operations of a tag into one API class. `@spec:OpenAPITag` adds a tag to the document, to
give it a description or to put the tags in order; a tag of an operation needs no `@spec:OpenAPITag`.

### Security

`@spec:OpenAPISecurityScheme` declares a [security scheme](https://spec.openapis.org/oas/v3.0.3#security-scheme-object).
Its positional value is the name of the scheme, and its parameters are the fields of the Security Scheme Object:

```wirespec
@spec:OpenAPISecurityScheme("bearerAuth", type: "http", scheme: "bearer", bearerFormat: "JWT")
@spec:OpenAPISecurityScheme("apiKey", type: "apiKey", name: "X-Api-Key", in: "header")
@spec:OpenAPISecurityScheme("oauth", type: "oauth2", flows: {
  clientCredentials: { tokenUrl: "https://auth.example.com/token", scopes: { "customers:write": "Change customers" } }
})
@spec:OpenAPISecurityScheme("oidc", type: "openIdConnect", openIdConnectUrl: "https://auth.example.com/.well-known/openid-configuration")
```

`@OpenAPISecurity` is a security requirement. On the spec it is the default of every operation, on an endpoint it
replaces that default for its operation:

| Annotation                                               | Requirement                                 | Meaning                                      |
|----------------------------------------------------------|---------------------------------------------|----------------------------------------------|
| `@OpenAPISecurity("bearerAuth")`                         | `{ "bearerAuth": [] }`                      | the scheme                                   |
| `@OpenAPISecurity("oauth", scopes: ["customers:write"])` | `{ "oauth": ["customers:write"] }`          | the scheme, with scopes                      |
| `@OpenAPISecurity(["mtls", "bearerAuth"])`               | `{ "mtls": [], "bearerAuth": [] }`          | all of the schemes                           |
| `@OpenAPISecurity({ oauth: ["read"], mtls: [] })`        | `{ "oauth": ["read"], "mtls": [] }`         | all of the schemes, with scopes              |
| `@OpenAPISecurity(...)` repeated                         | one requirement per annotation              | any one of the requirements                  |
| `@OpenAPINoSecurity`                                     | `security: []`                              | no security, even when the spec has a default |

Only `oauth2` and `openIdConnect` schemes have scopes.

### Several files

Spec annotations may come from any number of files. Files are read in the order of their paths, and their spec
annotations combine like this:

| Annotation                    | Across files                                                              |
|-------------------------------|---------------------------------------------------------------------------|
| `@spec:Description`           | once in the whole spec                                                    |
| `@spec:OpenAPIInfo`           | once in the whole spec                                                    |
| `@spec:OpenAPIServer`         | all servers, in file order; a repeated URL must be identical              |
| `@spec:OpenAPISecurityScheme` | merged by name; the same name twice must be identical                     |
| `@spec:OpenAPISecurity`       | from one file only; repeated in that file, any one requirement is enough  |
| `@spec:OpenAPITag`            | merged by name, in file order; the same name twice must be identical      |

The default security comes from one file, so that no file can add an alternative that weakens the security of every
endpoint. Keep the servers in one file as well: the first server is the default base URL of most code generators.

### Validation

The OpenAPI emitters check the annotations before they write anything, and fail with a message that names the endpoint
or the files involved when:

- `@OpenAPISecurity` names a scheme that no `@spec:OpenAPISecurityScheme` declares;
- a requirement gives scopes for a scheme that is not `oauth2` or `openIdConnect`;
- a security scheme has no `type`, an unknown one, or misses what its type needs: `scheme` for `http`, `name` and `in`
  for `apiKey`, `flows` for `oauth2`, `openIdConnectUrl` for `openIdConnect`;
- the spec annotations of several files disagree, as listed above;
- an annotation has the wrong target: `@OpenAPIInfo` or `@OpenAPISecurityScheme` without `spec:`,
  `@spec:OpenAPINoSecurity`, `@spec:OpenAPILink`, or any `@file:OpenAPI…`;
- a server has no URL, a server variable no default, or a tag no name;
- an endpoint has both `@OpenAPISecurity` and `@OpenAPINoSecurity`.

### Swagger 2

The OpenAPI v2 emitter writes the same metadata in the shape of Swagger 2:

- The first server becomes `host` and `basePath`, and the servers with that host and base path give the `schemes`. A
  server variable becomes its default value. Swagger 2 has one host, so other servers are left out, with a warning.
- Security schemes become `securityDefinitions`. Swagger 2 has no bearer scheme, so an `http` scheme other than `basic`
  becomes an API key in the `Authorization` header, with a warning. An `oauth2` scheme keeps one flow: `implicit`,
  `password`, `clientCredentials` (Swagger 2 calls it `application`) or `authorizationCode` (`accessCode`), in that
  order. `openIdConnect` schemes and API keys in a cookie have no Swagger 2 counterpart: they are left out, together with
  the requirements that need them.
- Tags and security requirements stay as they are.

### Converting OpenAPI to Wirespec

The converter writes all of this metadata as annotations, so a document survives the way to Wirespec and back: `info`,
`servers`, `security`, `tags` and `components.securitySchemes` become `@spec:` annotations in the header of the
converted file, and the `tags`, `security` and `servers` of an operation become annotations on its endpoint. A Swagger 2
document's `host`, `basePath` and `schemes` become a server per scheme, and its `securityDefinitions` security schemes.

## Links

OpenAPI v3 [response links](https://spec.openapis.org/oas/v3.1.0#link-object) round-trip through the `@OpenAPILink` annotation on a response variant. One annotation describes one outgoing link.

```wirespec
type User { id: String, name: String }

endpoint CreateUser POST User /users -> {
    @OpenAPILink("GetUser",
          operationId: "GetUserById",
          parameters: {id: "$response.body#/id"},
          description: "Fetch the just-created user")
    @OpenAPILink("DeleteUser",
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
