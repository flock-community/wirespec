# AsyncAPI

Wirespec converts [AsyncAPI](https://www.asyncapi.com/) documents into Wirespec, and Wirespec back into AsyncAPI. Both
AsyncAPI 2.x and 3.x documents can be converted, written in JSON. Wirespec writes AsyncAPI 3.0.

Every channel becomes a Wirespec [channel](../language/channels.mdx) that carries the payload of its messages, and the
schemas become types, enums, unions and refined types. What a Wirespec channel has no syntax for, the address of the
channel and the operations on it, is kept in annotations, so the channels can be written back.

## Channels

| AsyncAPI                                      | Wirespec                                                          |
|-----------------------------------------------|-------------------------------------------------------------------|
| a channel                                     | a `channel`, named after its key (3.x) or its address (2.x)       |
| the address of a channel                      | `@AsyncAPIAddress("todos/{todoId}/updates")`                     |
| the description of a channel                  | `@Description("...")`                                             |
| a `send` operation                            | `@AsyncAPISend("publishTodoUpdate")`, with the operation id       |
| a `receive` operation                         | `@AsyncAPIReceive("onTodoUpdate")`, with the operation id         |
| the message of a channel                      | the type the channel carries: the message payload                 |
| several messages on one channel               | a union of their payloads, named `...Message`                     |

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
@Description("Every change to a todo")
@AsyncAPIAddress("todos/{todoId}/updates")
@AsyncAPISend("publishTodoUpdate")
channel TodoUpdates -> Todo

type Todo {
  id: String,
  done: Boolean
}
```

AsyncAPI 2 describes operations from the side of the client: a client subscribes to the messages that the application
sends, and publishes the messages that the application receives. A `subscribe` operation therefore becomes
`@AsyncAPISend` and a `publish` operation `@AsyncAPIReceive`, as in AsyncAPI 3.

A message payload that is an object of its own becomes a type named after the message. A payload that is written in
another schema format than JSON Schema, such as Avro or Protobuf, becomes `Any`.

## Schemas

Message payloads and `components.schemas` are JSON Schema. A component schema keeps its name, and a schema that is
declared inline is named after where it is used: the property `assignee` of `Todo` becomes the type `TodoAssignee`.

| JSON Schema                                         | Wirespec                                                       |
|-----------------------------------------------------|----------------------------------------------------------------|
| `object` with `properties`                          | `type`; a property that is not `required` is nullable          |
| `object` with only `additionalProperties`           | a dictionary, `{ String }`                                     |
| `array`                                             | `Type[]`                                                       |
| `enum`                                              | `enum`                                                         |
| `oneOf`, `anyOf` of objects or references           | a union, `type Shape = Circle \| Square`                       |
| `allOf`                                             | a `type` with the properties of all its schemas                |
| `string`                                            | `String`; `format: binary` becomes `Bytes`                     |
| `integer`                                           | `Integer`; `format: int32` becomes `Integer32`                 |
| `number`                                            | `Number`; `format: float` becomes `Number32`                   |
| `boolean`                                           | `Boolean`                                                      |
| a component schema of a primitive type              | a refined type, keeping its `pattern`, `minimum` and `maximum` |
| `"type": ["string", "null"]`, or `oneOf` with `null`| a nullable reference, `String?`                                |
| `{}` and other schemas Wirespec cannot express      | `Any`                                                          |

The `description` of a schema or property becomes `@Description`, and a `default` becomes a Wirespec
[default value](../language/types.mdx#default-values) when the field can hold it.

## Wirespec to AsyncAPI

Wirespec converts back into a single AsyncAPI 3.0 document:

- Every channel becomes a channel with one message, which carries the channel's type as payload. The message is
  declared under `components.messages`, named after the type.
- The address is taken from `@AsyncAPIAddress`, or is the name of the channel.
- The operations are taken from `@AsyncAPISend` and `@AsyncAPIReceive`. An annotation without an operation id gets
  `send` or `receive` followed by the name of the channel, and a channel without these annotations gets a single
  `send` operation, `sendTodoUpdates`.
- Types, enums, unions and refined types become `components.schemas`. A comment or `@Description` becomes the
  description.
- A field with a description or default that refers to another type is written as `allOf` with a single `$ref`, as
  JSON Schema ignores what is written next to a `$ref`.
- Endpoints and rpcs are left out, because AsyncAPI has nothing like them.

AsyncAPI converted to Wirespec and back keeps the addresses, descriptions and operations of its channels and the
schemas of their payloads.

## Limitations

- **JSON only**: documents written in YAML have to be converted to JSON first.
- **References**: only references within the document are resolved, not references to other files or URLs.
- **Messages**: the headers, correlation ids, traits, examples and bindings of a message are not converted, and neither
  are channel parameters, servers, security schemes and the `reply` of an operation.
- **Message names**: a channel refers to the payload of its message, so the name of a message whose payload refers to a
  component schema is not kept.

## Playground

You can try the conversion online: [Playground convert AsyncAPI](https://playground.wirespec.io/?specification=asyncapi&emitter=wirespec),
and the other way around: [Playground convert to AsyncAPI](https://playground.wirespec.io/?specification=wirespec&emitter=asyncapi)
