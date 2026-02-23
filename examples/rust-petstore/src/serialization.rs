use crate::generated::model::order::Order;
use crate::generated::model::pet::Pet;
use crate::generated::wirespec::*;
use std::collections::HashMap;

// The wirespec Serializer/Deserializer traits use unbounded generics `<T>`,
// which cannot express `T: serde::Serialize` bounds. We use unsafe pointer
// casts dispatched by `std::any::type_name` to bridge the gap. This is safe
// because the generated code guarantees that `T` matches the type indicated
// by the `type` parameter at each monomorphized call site.

pub struct JsonSerialization;

macro_rules! body_serialize_dispatch {
    ($t:expr, $type_str:expr, $($ty:ty),*) => {
        match $type_str {
            $(
                s if s == std::any::type_name::<$ty>() => {
                    let v = unsafe { &*($t as *const _ as *const $ty) };
                    serde_json::to_vec(v).unwrap()
                }
            )*
            _ => panic!("Unsupported body serialize type: {}", $type_str),
        }
    };
}

macro_rules! body_deserialize_dispatch {
    ($raw:expr, $type_str:expr, $($ty:ty),*) => {
        match $type_str {
            $(
                s if s == std::any::type_name::<$ty>() => {
                    let concrete: $ty = serde_json::from_slice($raw).unwrap();
                    let result = unsafe { std::ptr::read(&concrete as *const $ty as *const _) };
                    std::mem::forget(concrete);
                    result
                }
            )*
            _ => panic!("Unsupported body deserialize type: {}", $type_str),
        }
    };
}

impl BodySerializer for JsonSerialization {
    fn serialize_body<T>(&self, t: &T, r#type: &str) -> Vec<u8> {
        body_serialize_dispatch!(t, r#type, Pet, Vec<Pet>, Order, HashMap<String, i32>)
    }
}

impl BodyDeserializer for JsonSerialization {
    fn deserialize_body<T>(&self, raw: &[u8], r#type: &str) -> T {
        body_deserialize_dispatch!(raw, r#type, Pet, Vec<Pet>, Order, HashMap<String, i32>)
    }
}

impl PathSerializer for JsonSerialization {
    fn serialize_path<T>(&self, t: &T, r#type: &str) -> String {
        match r#type {
            s if s == std::any::type_name::<i64>() => {
                let v = unsafe { &*(t as *const T as *const i64) };
                v.to_string()
            }
            s if s == std::any::type_name::<i32>() => {
                let v = unsafe { &*(t as *const T as *const i32) };
                v.to_string()
            }
            s if s == std::any::type_name::<String>() => {
                let v = unsafe { &*(t as *const T as *const String) };
                v.clone()
            }
            _ => panic!("Unsupported path serialize type: {}", r#type),
        }
    }
}

impl PathDeserializer for JsonSerialization {
    fn deserialize_path<T>(&self, raw: &str, r#type: &str) -> T {
        match r#type {
            s if s == std::any::type_name::<i64>() => {
                let concrete: i64 = raw.parse().unwrap();
                unsafe { std::ptr::read(&concrete as *const i64 as *const T) }
            }
            s if s == std::any::type_name::<i32>() => {
                let concrete: i32 = raw.parse().unwrap();
                unsafe { std::ptr::read(&concrete as *const i32 as *const T) }
            }
            s if s == std::any::type_name::<String>() => {
                let concrete: String = raw.to_string();
                let result = unsafe { std::ptr::read(&concrete as *const String as *const T) };
                std::mem::forget(concrete);
                result
            }
            _ => panic!("Unsupported path deserialize type: {}", r#type),
        }
    }
}

impl ParamSerializer for JsonSerialization {
    fn serialize_param<T>(&self, value: &T, r#type: &str) -> Vec<String> {
        match r#type {
            s if s == std::any::type_name::<Vec<String>>() => {
                let v = unsafe { &*(value as *const T as *const Vec<String>) };
                v.clone()
            }
            s if s == std::any::type_name::<String>() => {
                let v = unsafe { &*(value as *const T as *const String) };
                vec![v.clone()]
            }
            s if s == std::any::type_name::<i32>() => {
                let v = unsafe { &*(value as *const T as *const i32) };
                vec![v.to_string()]
            }
            s if s == std::any::type_name::<i64>() => {
                let v = unsafe { &*(value as *const T as *const i64) };
                vec![v.to_string()]
            }
            _ => panic!("Unsupported param serialize type: {}", r#type),
        }
    }
}

impl ParamDeserializer for JsonSerialization {
    fn deserialize_param<T>(&self, values: &[String], r#type: &str) -> T {
        match r#type {
            s if s == std::any::type_name::<Vec<String>>() => {
                let concrete: Vec<String> = values.to_vec();
                let result = unsafe { std::ptr::read(&concrete as *const Vec<String> as *const T) };
                std::mem::forget(concrete);
                result
            }
            s if s == std::any::type_name::<String>() => {
                let concrete: String = values.first().cloned().unwrap_or_default();
                let result = unsafe { std::ptr::read(&concrete as *const String as *const T) };
                std::mem::forget(concrete);
                result
            }
            s if s == std::any::type_name::<i32>() => {
                let concrete: i32 = values.first().map(|v| v.parse().unwrap()).unwrap();
                unsafe { std::ptr::read(&concrete as *const i32 as *const T) }
            }
            s if s == std::any::type_name::<i64>() => {
                let concrete: i64 = values.first().map(|v| v.parse().unwrap()).unwrap();
                unsafe { std::ptr::read(&concrete as *const i64 as *const T) }
            }
            _ => panic!("Unsupported param deserialize type: {}", r#type),
        }
    }
}

impl Serializer for JsonSerialization {}
impl Deserializer for JsonSerialization {}
impl Serialization for JsonSerialization {}
