//! JSON serialization for the generated code, built on `serde_json`.
//!
//! The generated models have no serde derives: Wirespec hands every body and parameter to the
//! serializer as a plain `T: 'static`. `Json` finds out which Petstore type `T` is, and converts it
//! with that type's `JsonValue` impl.

use std::any::{Any, TypeId, type_name};
use std::collections::HashMap;
use std::fmt::{Debug, Display};
use std::str::FromStr;

use serde_json::Value;

use crate::generated::model::{
    api_response::ApiResponse, category::Category, order::Order, order_status::OrderStatus,
    pet::Pet, pet_status::PetStatus, tag::Tag, user::User,
};
use crate::generated::wirespec::*;

pub struct Json;

impl Serialization for Json {}
impl Serializer for Json {}
impl Deserializer for Json {}

impl BodySerializer for Json {
    fn serialize_body<T: 'static>(&self, body: &T, _: TypeId) -> Vec<u8> {
        to_json(body).to_string().into_bytes()
    }
}

impl BodyDeserializer for Json {
    fn deserialize_body<T: 'static>(&self, raw: &[u8], _: TypeId) -> T {
        serde_json::from_slice(raw)
            .ok()
            .and_then(|json| from_json(&json))
            .unwrap_or_else(|| panic!("body is not a valid {}", type_name::<T>()))
    }
}

impl PathSerializer for Json {
    fn serialize_path<T: Display>(&self, value: &T, _: TypeId) -> String {
        value.to_string()
    }
}

impl PathDeserializer for Json {
    fn deserialize_path<T: FromStr>(&self, raw: &str, _: TypeId) -> T
    where
        T::Err: Debug,
    {
        raw.parse().expect("invalid path parameter")
    }
}

/// Query parameters and headers are sent as strings: one per item of a list.
impl ParamSerializer for Json {
    fn serialize_param<T: 'static>(&self, value: &T, _: TypeId) -> Vec<String> {
        match to_json(value) {
            Value::Array(items) => items.iter().map(text).collect(),
            item => vec![text(&item)],
        }
    }
}

/// The strings do not say what they hold, so try them as a list, a string, and a literal such as `42`.
impl ParamDeserializer for Json {
    fn deserialize_param<T: 'static>(&self, values: &[String], _: TypeId) -> T {
        let first = values.first().map(String::as_str).unwrap_or_default();
        from_json(&values.iter().cloned().map(Value::String).collect())
            .or_else(|| from_json(&Value::from(first)))
            .or_else(|| from_json(&serde_json::from_str(first).ok()?))
            .unwrap_or_else(|| panic!("{values:?} is not a valid {}", type_name::<T>()))
    }
}

fn text(json: &Value) -> String {
    json.as_str().map_or_else(|| json.to_string(), String::from)
}

/// Every type the Petstore sends or receives as a body or a parameter.
macro_rules! petstore_types {
    ($($type:ty),* $(,)?) => {
        fn to_json<T: 'static>(value: &T) -> Value {
            let value: &dyn Any = value;
            $(if let Some(value) = value.downcast_ref::<$type>() {
                return value.to_json();
            })*
            panic!("{} is not a Petstore type", type_name::<T>())
        }

        fn from_json<T: 'static>(json: &Value) -> Option<T> {
            let mut result: Option<T> = None;
            let slot: &mut dyn Any = &mut result;
            $(if let Some(slot) = slot.downcast_mut::<Option<$type>>() {
                *slot = <$type>::from_json(json);
                return result;
            })*
            panic!("{} is not a Petstore type", type_name::<T>())
        }
    };
}

petstore_types!(
    Pet,
    Vec<Pet>,
    Order,
    User,
    Vec<User>,
    ApiResponse,
    HashMap<String, i32>,
    String,
    Vec<String>,
    i32,
);

trait JsonValue: Sized {
    fn to_json(&self) -> Value;
    fn from_json(json: &Value) -> Option<Self>;
}

/// Wirespec renames fields to snake case, so every field is mapped to its name in the contract.
macro_rules! object {
    ($type:ident { $($field:ident: $name:literal),* $(,)? }) => {
        impl JsonValue for $type {
            fn to_json(&self) -> Value {
                [$(($name, self.$field.to_json())),*]
                    .into_iter()
                    .filter(|(_, value)| !value.is_null())
                    .collect()
            }

            fn from_json(json: &Value) -> Option<Self> {
                Some($type { $($field: JsonValue::from_json(&json[$name])?),* })
            }
        }
    };
}

object!(Pet {
    id: "id",
    category: "category",
    name: "name",
    photo_urls: "photoUrls",
    tags: "tags",
    status: "status"
});
object!(Category {
    id: "id",
    name: "name"
});
object!(Tag {
    id: "id",
    name: "name"
});
object!(Order {
    id: "id",
    pet_id: "petId",
    quantity: "quantity",
    ship_date: "shipDate",
    status: "status",
    complete: "complete"
});
object!(User {
    id: "id",
    username: "username",
    first_name: "firstName",
    last_name: "lastName",
    email: "email",
    password: "password",
    phone: "phone",
    user_status: "userStatus"
});
object!(ApiResponse {
    code: "code",
    r#type: "type",
    message: "message"
});

/// Generated enums implement `Enum`, which converts between a variant and its label in the contract.
macro_rules! enumeration {
    ($($type:ident),*) => {$(
        impl JsonValue for $type {
            fn to_json(&self) -> Value {
                self.label().into()
            }

            fn from_json(json: &Value) -> Option<Self> {
                Self::from_label(json.as_str()?)
            }
        }
    )*};
}

enumeration!(PetStatus, OrderStatus);

macro_rules! scalar {
    ($type:ty, $read:ident) => {
        impl JsonValue for $type {
            fn to_json(&self) -> Value {
                self.clone().into()
            }

            fn from_json(json: &Value) -> Option<Self> {
                json.$read()?.try_into().ok()
            }
        }
    };
}

scalar!(String, as_str);
scalar!(bool, as_bool);
scalar!(i32, as_i64);
scalar!(i64, as_i64);

impl<T: JsonValue> JsonValue for Option<T> {
    fn to_json(&self) -> Value {
        self.as_ref().map_or(Value::Null, T::to_json)
    }

    fn from_json(json: &Value) -> Option<Self> {
        match json {
            Value::Null => Some(None),
            json => T::from_json(json).map(Some),
        }
    }
}

impl<T: JsonValue> JsonValue for Vec<T> {
    fn to_json(&self) -> Value {
        self.iter().map(T::to_json).collect()
    }

    fn from_json(json: &Value) -> Option<Self> {
        json.as_array()?.iter().map(T::from_json).collect()
    }
}

impl<T: JsonValue> JsonValue for HashMap<String, T> {
    fn to_json(&self) -> Value {
        self.iter()
            .map(|(key, value)| (key.clone(), value.to_json()))
            .collect()
    }

    fn from_json(json: &Value) -> Option<Self> {
        json.as_object()?
            .iter()
            .map(|(key, value)| Some((key.clone(), T::from_json(value)?)))
            .collect()
    }
}
