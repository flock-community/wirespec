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

## Limitations

- **Defaults**: Avro default values are currently ignored and not preserved in the Wirespec output.
- **Unions**: There are some restrictions on Unions involving multiple simple types.

## Playground

You can try the conversion online: [Playground convert Avro](https://playground.wirespec.io/?emitter=avro&specification=wirespec    )
