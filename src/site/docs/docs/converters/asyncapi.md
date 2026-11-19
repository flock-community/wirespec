# AsyncAPI

Wirespec converts [AsyncAPI](https://www.asyncapi.com/) documents into Wirespec, and Wirespec back into AsyncAPI. Both
AsyncAPI 2.x and 3.x documents can be converted, written in JSON.

The conversion is lossless: converting a document to Wirespec and back gives the document it came from, in the same
AsyncAPI version. Everything in the document is kept, either as a native Wirespec construct or, where Wirespec has no
syntax for it, as an annotation. Channels become [channels](../language/channels.mdx) and schemas become types, enums,
unions and refined types, so the Wirespec can be used to generate code. Servers, operations, message details, bindings,
security schemes, extensions (`x-...`) and the JSON Schema keywords Wirespec does not write itself are kept in
annotations.

## Channels

Every channel becomes a Wirespec `channel` that carries the payload of its message. A channel with more than one
message carries a union of their payloads, named `<Channel>Message`.

```json
{
  "asyncapi": "3.0.0",
  "info": { "title": "Todo events", "version": "1.0.0" },
  "channels": {
    "todoUpdates": {
      "address": "todos/{todoId}/updates",
      "description": "Every change to a todo",
      "messages": {
        "todoUpdated": { "payload": { "$ref": "#/components/schemas/Todo" } }
      }
    }
  },
  "operations": {
    "publishTodoUpdate": {
      "action": "send",
      "channel": { "$ref": "#/channels/todoUpdates" }
    }
  },
  "components": {
    "schemas": {
      "Todo": {
        "type": "object",
        "required": ["id", "done"],
        "properties": {
          "id": { "type": "string" },
          "done": { "type": "boolean" }
        }
      }
    }
  }
}
```

converts to

```wirespec
@spec:AsyncAPIDocument(asyncapi: "3.0.0")
@spec:AsyncAPIInfo(title: "Todo events", version: "1.0.0")
@spec:AsyncAPIComponents

@Description("Every change to a todo")
@AsyncAPIChannel(address: "todos/{todoId}/updates")
@AsyncAPIMessage("todoUpdated", payload: "Todo")
@AsyncAPISend("publishTodoUpdate")
channel TodoUpdates -> Todo

type Todo {
  id: String,
  done: Boolean
}
```

- The channel is named after its key (`todoUpdates` becomes `TodoUpdates`), or after its address in AsyncAPI 2. A key
  that does not read back from the name, such as the address `streetlights/{id}/turn`, is kept in `@AsyncAPIName`.
- `@AsyncAPIChannel` holds the fields of the channel: its address, parameters, servers, bindings and so on. The
  description is a `@Description`, and the messages each get an `@AsyncAPIMessage`.
- Every operation on the channel is an `@AsyncAPISend` or `@AsyncAPIReceive`, with the operation id first. It holds the
  fields of the operation, such as its summary, bindings and reply. The channel it is on is implied, and its messages are
  named by their key in the channel: `messages: ["todoUpdated"]`. An operation that is no plain operation on a channel,
  such as a `$ref` to `components.operations`, belongs to the [document](#the-document): it is a `@spec:AsyncAPISend`
  or `@spec:AsyncAPIReceive`.
- AsyncAPI 2 describes operations from the side of the client: a client subscribes to what the application sends. A
  `subscribe` operation is therefore an `@AsyncAPISend` and a `publish` operation an `@AsyncAPIReceive`, as in AsyncAPI
  3. Its `message` names the message by its key, or the messages it holds in a `oneOf` as an array.

## Messages

An `@AsyncAPIMessage` holds a message, with its key first. A message that refers to another message holds that
reference, `"$ref": "#/components/messages/TodoUpdated"`; the messages of `components.messages` belong to the
[document](#the-document), as `@spec:AsyncAPIMessage`. The payload and the headers of a message are schemas, which
convert to Wirespec: `payload: "Todo"` names the Wirespec type the payload is.

- A payload that is an object of its own becomes a type named after the message, and its headers a type named
  `<Message>Headers`.
- A payload that its Wirespec type does not give back exactly, such as an array with `maxItems`, or a payload in
  another schema format than JSON Schema, such as Avro or Protobuf, is kept in `@AsyncAPIMessage` as it is. The channel
  still carries the Wirespec type it converts to, or `Any` when there is none.

## Schemas

`components.schemas` and the schemas in messages are JSON Schema. A component schema keeps its name, and a schema that
is declared inline is named after where it is used: the property `assignee` of `Todo` becomes the type `TodoAssignee`.

| JSON Schema                                          | Wirespec                                                                                                                                        |
|------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------|
| `object` with `properties`                           | `type`; a property that is not `required` is nullable                                                                                           |
| `object` with only `additionalProperties`            | a dictionary, `{ String }`                                                                                                                      |
| `array`                                              | `Type[]`                                                                                                                                        |
| `enum`                                               | `enum`                                                                                                                                          |
| `oneOf`, `anyOf` of objects or references            | a union, `type Shape = Circle \| Square`                                                                                                        |
| `allOf`                                              | a `type` with the properties of all its schemas                                                                                                 |
| `string`                                             | `String`; `format: binary` becomes `Bytes`, and `date`, `date-time`, `time`, `email` and `uuid` [string formats](../language/types.mdx#refined) |
| `integer`                                            | `Integer`; `format: int32` becomes `Integer32`                                                                                                  |
| `number`                                             | `Number`; `format: float` becomes `Number32`                                                                                                    |
| `boolean`                                            | `Boolean`                                                                                                                                       |
| a component schema of a primitive type               | a refined type with its `pattern`, `minimum` and `maximum`                                                                                      |
| `"type": ["string", "null"]`, or `oneOf` with `null` | a nullable reference, `String?`                                                                                                                 |
| `{}`, a reference to another file and anything else  | `Any`                                                                                                                                           |

The `description` of a schema or property is a `@Description`, and a `default` a Wirespec
[default value](../language/types.mdx#default-values) where the field can hold it.

Everything else that a schema has, Wirespec keeps in `@AsyncAPISchema` on the definition or field: the keywords to add,
such as `format`, `minLength`, `examples` or `title`, and in `omit` the keywords that Wirespec writes but the schema
leaves out, such as a missing `type`. A keyword called `default` or `omit` is in the positional dictionary.

```wirespec
type Todo {
  createdAt: String?(date-time),
  @AsyncAPISchema(format: "uri")
  homepage: String?,
  @AsyncAPISchema({ default: [ ] }, uniqueItems: "true")
  tags: String[]?,
  @AsyncAPIName("x-trace.id")
  x-trace_id: String?
}
```

A component schema that is no definition of its own in Wirespec, such as an array, a `oneOf` with `null` or `true`, is
kept in `@AsyncAPIComponents`. A schema that refers to it carries the Wirespec type it converts to.

## Annotations

| Annotation                                                          | On                            | Carries                                                                        |
|---------------------------------------------------------------------|-------------------------------|--------------------------------------------------------------------------------|
| `@Description("...")`                                               | channels, definitions, fields | the description of a channel, schema or property                               |
| `@Description("...")`                                               | `@spec:`                      | the description of the document, `info.description`                            |
| `@AsyncAPIInfo(title: "...", version: "...")`                       | `@spec:`                      | the rest of `info` (see [Info, servers, security and tags](#info-servers-security-and-tags)) |
| `@AsyncAPIServer("production", host: "...", protocol: "kafka")`     | `@spec:`                      | a server, by its key                                                           |
| `@AsyncAPISecurityScheme("saslScram", type: "scramSha256")`         | `@spec:`                      | a security scheme in `components.securitySchemes`, by its key                  |
| `@AsyncAPITag("todo", description: "...")`                          | `@spec:`                      | a tag of the document                                                          |
| `@AsyncAPIDocument(asyncapi: "3.0.0", id: "...")`                   | `@spec:`                      | the fields of the document besides the ones the annotations above hold         |
| `@AsyncAPIComponents(parameters: {...}, ...)`                       | `@spec:`                      | the components besides the schemas, messages and security schemes             |
| `@AsyncAPIChannel(address: "...", bindings: {...})`                 | channels                      | the fields of the channel besides its description and messages                 |
| `@AsyncAPIMessage("key", name: "...", payload: "Todo")`             | channels, `@spec:`            | a message of the channel, or of `components.messages`                          |
| `@AsyncAPISend("id", ...)`, `@AsyncAPIReceive("id", ...)`           | channels, `@spec:`            | an operation: `send` or `subscribe`, `receive` or `publish`                    |
| `@AsyncAPIName("key")`                                              | channels, definitions, fields | the key of a channel, schema or property that is not the Wirespec name         |
| `@AsyncAPISchema(format: "uri", omit: ["type"])`                    | definitions, fields           | the JSON Schema keywords to add to, and to leave out of, what Wirespec writes  |
| `@AsyncAPIInline`                                                   | definitions                   | a schema that is declared where it is used, instead of in `components.schemas` |

_Definitions_ are types, enums, unions and refined types, and _fields_ are the fields of a type. `@spec:` marks what
belongs to the document as a whole.

### The document

The document as a whole has no Wirespec definition, so what belongs to it is kept in `@spec:` annotations. They are
written in the file header: the annotations at the top of a file, followed by an empty line, which describe the whole
specification instead of one definition.

```wirespec
@spec:AsyncAPIDocument(asyncapi: "3.0.0")
@spec:AsyncAPIInfo(title: "Todo events", version: "1.0.0")
@spec:AsyncAPIMessage("TodoUpdated", payload: "Todo")

channel TodoUpdates -> Todo
```

The files of a specification together make one document, so the `@spec:` annotations of all of them count, in the order
of the files. A document has one `@spec:AsyncAPIDocument`, `@spec:AsyncAPIComponents`, `@spec:AsyncAPIInfo` and
`@spec:Description`, and each server, security scheme, tag, component message and operation once, by its key:
declaring one twice, in one file or in two, is an error, as for every [spec annotation](../language/language.mdx#the-file-header). So is any of these annotations on a definition, such as `@AsyncAPIDocument` or
`@AsyncAPIComponents` where Wirespec converted by earlier versions has them, and `@file:AsyncAPI…` annotations: an
AsyncAPI document has no metadata per file.

### Info, servers, security and tags

The info, servers, security schemes and tags of a document each have an annotation of their own, so they read as what
they are instead of as fields of `@spec:AsyncAPIDocument`:

```wirespec
@spec:AsyncAPIDocument(asyncapi: "3.0.0", defaultContentType: "application/json")
@spec:Description("Everything that happens to todos")
@spec:AsyncAPIInfo(title: "Todo events", version: "1.0.0", contact: { name: "Todo team", email: "todo@example.com" })
@spec:AsyncAPITag("todo", description: "Todo events")
@spec:AsyncAPIServer("production", host: "broker.example.com:9092", protocol: "kafka", security: ["saslScram"])
@spec:AsyncAPISecurityScheme("saslScram", type: "scramSha256", description: "SASL/SCRAM")

channel TodoUpdates -> Todo
```

writes, besides the channel, its operation and the schemas:

```json
{
  "asyncapi": "3.0.0",
  "defaultContentType": "application/json",
  "info": {
    "title": "Todo events",
    "version": "1.0.0",
    "contact": { "name": "Todo team", "email": "todo@example.com" },
    "description": "Everything that happens to todos",
    "tags": [{ "name": "todo", "description": "Todo events" }]
  },
  "servers": {
    "production": {
      "host": "broker.example.com:9092",
      "protocol": "kafka",
      "security": [{ "$ref": "#/components/securitySchemes/saslScram" }]
    }
  },
  "components": {
    "securitySchemes": { "saslScram": { "type": "scramSha256", "description": "SASL/SCRAM" } }
  }
}
```

- `@spec:Description` is the description of the document, as it is for the [OpenAPI emitters](openapi.md#info).
- `@spec:AsyncAPIInfo` takes the other fields of the [Info Object](https://www.asyncapi.com/docs/reference/specification/v3.0.0#infoObject),
  such as `title`, `version`, `termsOfService`, `contact`, `license` and `externalDocs`.
- `@spec:AsyncAPITag` takes the name of the tag first, then its other fields. The tags go in `info.tags` in AsyncAPI 3
  and in the document itself in AsyncAPI 2.
- `@spec:AsyncAPIServer` takes the key of the server first, then the fields of the
  [Server Object](https://www.asyncapi.com/docs/reference/specification/v3.0.0#serverObject): `host`, `protocol`,
  `pathname` and so on in AsyncAPI 3, `url` and `protocol` in AsyncAPI 2. Its `security` names the security schemes the
  server needs, `security: ["saslScram"]`, which are written as a reference to each scheme in AsyncAPI 3 and as a
  requirement without scopes in AsyncAPI 2. Security that is more than that, such as a requirement with scopes, is
  written as it is.
- `@spec:AsyncAPISecurityScheme` takes the key of the security scheme first, then the fields of the
  [Security Scheme Object](https://www.asyncapi.com/docs/reference/specification/v3.0.0#securitySchemeObject).

A server that names a security scheme no `@spec:AsyncAPISecurityScheme` or `@spec:AsyncAPIComponents` declares is an
error, and so is declaring the same info, server or security scheme in one of these annotations and in
`@spec:AsyncAPIDocument` or `@spec:AsyncAPIComponents` as well. A build can set the title and version instead of the
ones in `@spec:AsyncAPIInfo`, see [the title and version of the spec](../plugins/plugins.md#the-title-and-version-of-the-spec).

Converting a document writes these annotations wherever the document has their shape: an `info` object, tags with a
name, and servers and security schemes that are objects. Anything else stays in `@spec:AsyncAPIDocument` or
`@spec:AsyncAPIComponents`, so the conversion stays lossless.

### Parameters

The named parameters of an annotation are the fields of the AsyncAPI object it holds:

- `@AsyncAPIDocument`, `@AsyncAPIComponents`, `@AsyncAPIInfo` and `@AsyncAPIChannel` take the fields of the document,
  its components, its info and the channel.
- `@AsyncAPIServer`, `@AsyncAPISecurityScheme` and `@AsyncAPITag` take the key of the server or security scheme, or
  the name of the tag, first, then its other fields.
- `@AsyncAPIMessage` takes the key of the message first, then the fields of the message. Its `payload` and `headers`
  name a Wirespec type, such as `payload: "Todo"` or `payload: "Todo[]"`, or hold the schema itself.
- `@AsyncAPISend` and `@AsyncAPIReceive` take the key of the operation first, or its `operationId` in AsyncAPI 2, then
  the fields of the operation. On a channel, its `channel` is left out, and `messages` names the messages by their key
  in the channel. An operation whose `action` is no `send` or `receive` is an `@AsyncAPISend` that keeps its `action`.
- `@AsyncAPIName` takes the key.
- `@AsyncAPISchema` takes the JSON Schema keywords to set. Its first parameter is a dictionary of the keywords called
  `default` or `omit`, or the whole schema when it is no object, such as `true`. `omit` lists the keywords to leave out.
- `@Description` takes the description, and `@AsyncAPIInline` takes nothing.

### JSON in annotations

An annotation holds JSON in its parameters. An object is a dictionary, such as `info: { title: "Todo events" }`, and
an array of strings, numbers or booleans an array, such as `examples: ["0f3a9b1c"]`. Any other array is written as its
JSON text, such as `tags: "[{\"name\":\"todo\"}]"`. A key that is not an identifier, such as `$ref` or `x-trace.id`, is
written between quotes.

A string is written as it is, but a number, boolean or `null` is written as its JSON text: `partitions: "3"` is the
number `3` and `address: "null"` is `null`. A string that reads as JSON itself is written as a JSON string literal, so
the string `"3.5"` is `protocolVersion: "\"3.5\""`.

## Wirespec to AsyncAPI

Wirespec converted from AsyncAPI, which has an `@spec:AsyncAPIDocument`, gives back the document it came from. Any
other Wirespec gets an AsyncAPI 3.0 document:

- Every channel gets one message, which carries the type of the channel as payload and is declared in
  `components.messages`, named after the type. Its key is the name of the channel starting with a lowercase letter,
  `todoUpdates`, which is its address too.
- Every channel gets a `send` operation, `sendTodoUpdates`, unless it has an `@AsyncAPISend` or `@AsyncAPIReceive`.
- `@AsyncAPIChannel`, `@AsyncAPIMessage` and the operation annotations add to, or take the place of, what is written
  by default, so a channel can be given its address. `@AsyncAPIName`, `@AsyncAPISchema` and `@AsyncAPIInline` work as
  they do on Wirespec converted from AsyncAPI:

```wirespec
@AsyncAPIChannel(address: "todos/updates")
@AsyncAPISend("publishTodoUpdate")
channel TodoUpdates -> Todo
```

- `@spec:AsyncAPIComponents`, `@spec:AsyncAPIMessage` and the operation annotations in the file header add to the
  components and operations of the document, and `@spec:Description`, `@spec:AsyncAPIInfo`, `@spec:AsyncAPIServer`,
  `@spec:AsyncAPISecurityScheme` and `@spec:AsyncAPITag` to its info, servers, security schemes and tags.
- Types, enums, unions and refined types become `components.schemas`. A comment or `@Description` becomes the
  description.
- Endpoints and rpcs are left out, because AsyncAPI has nothing like them.

The CLI, Maven and Gradle plugins write AsyncAPI with the language `AsyncAPI`. A build can set the `info.title` and
`info.version` of the document, of a converted document as well as of any other, for example to stamp its own version
on it: see [the title and version of the spec](../plugins/plugins.md#the-title-and-version-of-the-spec).

## Limitations

- **JSON only**: documents written in YAML have to be converted to JSON first.
- **Formatting**: the order of the keys in an object is not kept, and neither is how the JSON was formatted.
- **Definitions**: Wirespec needs at least one definition next to the file header, so a document without channels or
  schemas cannot be converted.
- **Native types**: a schema reached through a reference that is not to `components.schemas`, such as
  `#/components/messages/TodoUpdated/payload` or another file, is kept in `@AsyncAPISchema` as it is and converts to
  `Any`.

## Playground

You can try the conversion online: [Playground convert AsyncAPI](https://playground.wirespec.io/?specification=asyncapi&emitter=wirespec),
and the other way around: [Playground convert to AsyncAPI](https://playground.wirespec.io/?specification=wirespec&emitter=asyncapi)
