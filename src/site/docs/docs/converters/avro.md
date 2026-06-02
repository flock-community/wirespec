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

Scalar default values are preserved in both directions. Reading an Avro schema maps a field's
`default` to a Wirespec field default, and emitting a schema writes the default back. A default is
only written for non-nullable fields, since Avro requires a union default to match its first
branch (`null` for nullable fields).

```wirespec
type Pet {
  name: String = "Rex",
  age: Integer = 3,
  active: Boolean = true
}
```

## Limitations

- **Defaults**: Only scalar defaults (string, integer, number, boolean) are supported; complex
  defaults (arrays, maps, records) are not preserved.
- **Unions**: There are some restrictions on Unions involving multiple simple types.

## Playground

You can try the conversion online: [Playground convert Avro](https://playground.wirespec.io/?emitter=avro&specification=wirespec    )
