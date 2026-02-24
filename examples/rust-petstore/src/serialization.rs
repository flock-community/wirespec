use std::any::TypeId;
use crate::gen::wirespec::*;

impl<T: serde::Serialize> Serialize for T {
    fn serialize(&self) -> Vec<u8> {
        serde_json::to_vec(self).unwrap()
    }
}

impl<T: serde::de::DeserializeOwned> Deserialize for T {
    fn deserialize(bytes: &[u8]) -> Result<Self, String> {
        serde_json::from_slice(bytes).map_err(|e| e.to_string())
    }
}

pub struct JsonSerialization;

impl BodySerializer for JsonSerialization {
    fn serialize_body<T: Serialize>(&self, t: &T, _type: TypeId) -> Vec<u8> {
        t.serialize()
    }
}

impl BodyDeserializer for JsonSerialization {
    fn deserialize_body<T: Deserialize>(&self, raw: &[u8], _type: TypeId) -> T {
        T::deserialize(raw).unwrap()
    }
}

impl PathSerializer for JsonSerialization {
    fn serialize_path<T: std::fmt::Display>(&self, t: &T, _type: TypeId) -> String {
        t.to_string()
    }
}

impl PathDeserializer for JsonSerialization {
    fn deserialize_path<T: std::str::FromStr>(&self, raw: &str, _type: TypeId) -> T
    where
        T::Err: std::fmt::Debug,
    {
        raw.parse().unwrap()
    }
}

impl ParamSerializer for JsonSerialization {
    fn serialize_param<T: Serialize>(&self, value: &T, _type: TypeId) -> Vec<String> {
        let bytes = value.serialize();
        let json: serde_json::Value = serde_json::from_slice(&bytes).unwrap();
        match json {
            serde_json::Value::Array(arr) => arr.iter().map(json_value_to_string).collect(),
            v => vec![json_value_to_string(&v)],
        }
    }
}

impl ParamDeserializer for JsonSerialization {
    fn deserialize_param<T: Deserialize>(&self, values: &[String], _type: TypeId) -> T {
        let parsed: Vec<serde_json::Value> = values
            .iter()
            .map(|v| serde_json::from_str(v).unwrap_or(serde_json::Value::String(v.clone())))
            .collect();
        if parsed.len() == 1 {
            let json_bytes = serde_json::to_vec(&parsed[0]).unwrap();
            if let Ok(result) = T::deserialize(&json_bytes) {
                return result;
            }
        }
        let json_bytes = serde_json::to_vec(&serde_json::Value::Array(parsed)).unwrap();
        T::deserialize(&json_bytes).unwrap()
    }
}

impl Serializer for JsonSerialization {}
impl Deserializer for JsonSerialization {}
impl Serialization for JsonSerialization {}

fn json_value_to_string(v: &serde_json::Value) -> String {
    match v {
        serde_json::Value::String(s) => s.clone(),
        serde_json::Value::Bool(b) => b.to_string(),
        serde_json::Value::Number(n) => n.to_string(),
        serde_json::Value::Null => String::new(),
        other => serde_json::to_string(other).unwrap(),
    }
}
