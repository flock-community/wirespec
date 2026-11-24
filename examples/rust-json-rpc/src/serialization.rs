//! JSON serialization for the generated code, built on `serde_json`.
//!
//! The RustSerde extension makes every model a serde type, and bounds the bodies of `BodySerializer` and
//! `BodyDeserializer` with serde, so one implementation covers every param, result and error of the rpcs. The
//! generated code writes the JSON-RPC envelope itself.

use std::any::TypeId;
use std::fmt::{Debug, Display};
use std::str::FromStr;

use crate::generated::wirespec::*;

pub struct Json;

impl Serialization for Json {}
impl Serializer for Json {}
impl Deserializer for Json {}

impl BodySerializer for Json {
    fn serialize_body<T: serde::Serialize + 'static>(&self, body: &T, _: TypeId) -> Vec<u8> {
        serde_json::to_vec(body).expect("a value of the contract is always JSON")
    }
}

impl BodyDeserializer for Json {
    fn deserialize_body<T: serde::de::DeserializeOwned + 'static>(
        &self,
        raw: &[u8],
        _: TypeId,
    ) -> Result<T, WirespecError> {
        serde_json::from_slice(raw)
            .map_err(|error| WirespecError::Deserialization(error.to_string()))
    }
}

/// JSON-RPC has no paths and no query parameters or headers of the contract: it only sends bodies.
impl PathSerializer for Json {
    fn serialize_path<T: Display>(&self, value: &T, _: TypeId) -> String {
        value.to_string()
    }
}

impl PathDeserializer for Json {
    fn deserialize_path<T: FromStr>(&self, raw: &str, _: TypeId) -> Result<T, WirespecError>
    where
        T::Err: Debug,
    {
        raw.parse()
            .map_err(|_| WirespecError::Deserialization(raw.to_string()))
    }
}

impl ParamSerializer for Json {
    fn serialize_param<T: 'static>(&self, _: &T, _: TypeId) -> Vec<String> {
        Vec::new()
    }
}

impl ParamDeserializer for Json {
    fn deserialize_param<T: 'static>(
        &self,
        values: &[String],
        _: TypeId,
    ) -> Result<T, WirespecError> {
        Err(WirespecError::Deserialization(format!(
            "JSON-RPC sends no parameters, got {values:?}"
        )))
    }
}
