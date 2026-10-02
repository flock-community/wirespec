# Avro

Wirespec supports converting Avro schemas (.avsc) into Wirespec types.

## Type Conversion

### Primitive Types

| Avro Type | Wirespec Type | Notes |
|---|---|---|
| `string` | `String` | |
| `boolean` | `Boolean` | |
| `int` | `Integer` | 32-bit precision |
| `long` | `Integer` | 64-bit precision |
| `float` | `Number` | 32-bit precision |
| `double` | `Number` | 64-bit precision |
| `bytes` | `Bytes` | |
| `null` | `Unit` | Or used to mark a type as nullable |

### Complex Types

| Avro Type | Wirespec Type | Notes |
|---|---|---|
| `record` | `Type` | Converted to a named Wirespec Type definition |
| `enum` | `Enum` | Converted to a Wirespec Enum |
| `array` | `Type[]` | Converted to an Iterable |
| `map` | `Dict` | Converted to a Dictionary (Map) |
| `union` | `Union` | Converted to a Wirespec Union. Unions with `null` become nullable types. |

## Defaults

Field defaults are converted in both directions, for both Avro schemas (`.avsc`) and Avro IDL (`.avdl`). Reading a
schema turns the `default` of a record field into a Wirespec [default value](../language/types.mdx#default-values), and
emitting a schema writes it back.

```json
{
  "type": "record",
  "name": "Settings",
  "fields": [
    { "name": "name", "type": "string", "default": "anonymous" },
    { "name": "retries", "type": "long", "default": 3 },
    { "name": "nickname", "type": ["null", "string"], "default": null },
    { "name": "title", "type": ["string", "null"], "default": "none" }
  ]
}
```

```wirespec
type Settings {
  name: String = "anonymous",
  retries: Integer = 3,
  nickname: String? = null,
  title: String? = "none"
}
```

Avro checks the default of a union against the union's first branch. When a nullable field has a default other than
`null`, the emitted union puts the field's type first (`["string", "null"]`) instead of `null`.

Wirespec only has defaults for primitive fields and `null` defaults for nullable fields, so the other Avro defaults
(enum symbols, arrays, maps, records and bytes) are dropped when a schema is read. A default that does not match the
field's type is dropped as well.

## Limitations

- **Defaults**: Only defaults that Wirespec supports are converted, see [Defaults](#defaults).
- **Unions**: There are some restrictions on Unions involving multiple simple types.

## Playground

You can try the conversion online: [Playground convert Avro](https://playground.wirespec.io/?emitter=avro&specification=wirespec    )
