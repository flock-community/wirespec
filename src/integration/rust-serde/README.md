# Rust Serde integration

An [IR extension](https://wirespec.io/docs/plugins#ir-extensions) for the Rust emitter that makes every generated
model a [serde](https://serde.rs) type, in the JSON form of the contract.

## Usage

The extension is bundled with every Wirespec plugin as `RustSerde`:

```shell
wirespec compile -i ./wirespec -o ./src/generated -l Rust -p '' --shared -e RustSerde
```

The crate that holds the generated code needs `serde` with its `derive` feature, and a data format such as
`serde_json`:

```toml
[dependencies]
regex = "1"
serde = { version = "1", features = ["derive"] }
serde_json = "1"
```

## What it adds

| Model   | Generated                                                              | JSON                                  |
|---------|------------------------------------------------------------------------|---------------------------------------|
| type    | `Serialize` and `Deserialize`, every field renamed to its Wirespec name | `{"itemId":"abc","role":"ASSISTANT"}` |
| refined | `#[serde(transparent)]`                                                | `"abc"`                               |
| enum    | written as its label                                                   | `"ASSISTANT"`                         |
| union   | `#[serde(untagged)]`                                                   | the JSON of the member                |

In the shared `wirespec.rs`, `BodySerializer` and `BodyDeserializer` get a serde bound on the body, so one
implementation hands every body to serde.

See the [documentation](https://wirespec.io/docs/integration/rust-serde) for the generated code, an example serializer and
the limits.
