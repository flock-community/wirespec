use std::any::{Any, TypeId};
use crate::gen::wirespec::*;
use crate::gen::model::pet::Pet;
use crate::gen::model::category::Category;
use crate::gen::model::tag::Tag;
use crate::gen::model::pet_status::PetStatus;
use crate::gen::model::order::Order;
use crate::gen::model::order_status::OrderStatus;
use crate::gen::model::user::User;
use crate::gen::model::api_response::ApiResponse;

pub struct JsonSerialization;

impl BodySerializer for JsonSerialization {
    fn serialize_body<T: 'static>(&self, t: &T, _type: TypeId) -> Vec<u8> {
        let any: &dyn Any = t;
        let value = if let Some(v) = any.downcast_ref::<Pet>() {
            pet_to_json(v)
        } else if let Some(v) = any.downcast_ref::<Vec<Pet>>() {
            serde_json::Value::Array(v.iter().map(pet_to_json).collect())
        } else if let Some(v) = any.downcast_ref::<Order>() {
            order_to_json(v)
        } else if let Some(v) = any.downcast_ref::<User>() {
            user_to_json(v)
        } else if let Some(v) = any.downcast_ref::<Vec<User>>() {
            serde_json::Value::Array(v.iter().map(user_to_json).collect())
        } else if let Some(v) = any.downcast_ref::<ApiResponse>() {
            api_response_to_json(v)
        } else if let Some(v) = any.downcast_ref::<String>() {
            serde_json::Value::String(v.clone())
        } else if let Some(v) = any.downcast_ref::<std::collections::HashMap<String, i32>>() {
            let map: serde_json::Map<String, serde_json::Value> = v
                .iter()
                .map(|(k, v)| (k.clone(), serde_json::Value::Number((*v).into())))
                .collect();
            serde_json::Value::Object(map)
        } else {
            panic!("Unsupported body type for serialization: {:?}", _type)
        };
        serde_json::to_vec(&value).unwrap()
    }
}

impl BodyDeserializer for JsonSerialization {
    fn deserialize_body<T: 'static>(&self, raw: &[u8], r#type: TypeId) -> T {
        let value: serde_json::Value = serde_json::from_slice(raw).unwrap();
        let boxed: Box<dyn Any> = if r#type == TypeId::of::<Pet>() {
            Box::new(json_to_pet(&value))
        } else if r#type == TypeId::of::<Vec<Pet>>() {
            Box::new(
                value
                    .as_array()
                    .unwrap()
                    .iter()
                    .map(json_to_pet)
                    .collect::<Vec<Pet>>(),
            )
        } else if r#type == TypeId::of::<Order>() {
            Box::new(json_to_order(&value))
        } else if r#type == TypeId::of::<User>() {
            Box::new(json_to_user(&value))
        } else if r#type == TypeId::of::<Vec<User>>() {
            Box::new(
                value
                    .as_array()
                    .unwrap()
                    .iter()
                    .map(json_to_user)
                    .collect::<Vec<User>>(),
            )
        } else if r#type == TypeId::of::<ApiResponse>() {
            Box::new(json_to_api_response(&value))
        } else if r#type == TypeId::of::<String>() {
            Box::new(value.as_str().unwrap_or_default().to_string())
        } else if r#type == TypeId::of::<std::collections::HashMap<String, i32>>() {
            let map: std::collections::HashMap<String, i32> = value
                .as_object()
                .unwrap()
                .iter()
                .map(|(k, v)| (k.clone(), v.as_i64().unwrap_or(0) as i32))
                .collect();
            Box::new(map)
        } else {
            panic!("Unsupported body type for deserialization: {:?}", r#type)
        };
        *boxed.downcast::<T>().unwrap()
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
    fn serialize_param<T: 'static>(&self, value: &T, _type: TypeId) -> Vec<String> {
        let any: &dyn Any = value;
        if let Some(s) = any.downcast_ref::<String>() {
            vec![s.clone()]
        } else if let Some(v) = any.downcast_ref::<Vec<String>>() {
            v.clone()
        } else if let Some(b) = any.downcast_ref::<bool>() {
            vec![b.to_string()]
        } else if let Some(n) = any.downcast_ref::<i32>() {
            vec![n.to_string()]
        } else if let Some(n) = any.downcast_ref::<i64>() {
            vec![n.to_string()]
        } else {
            panic!("Unsupported param type for serialization: {:?}", _type)
        }
    }
}

impl ParamDeserializer for JsonSerialization {
    fn deserialize_param<T: 'static>(&self, values: &[String], r#type: TypeId) -> T {
        let boxed: Box<dyn Any> = if r#type == TypeId::of::<String>() {
            Box::new(values.first().cloned().unwrap_or_default())
        } else if r#type == TypeId::of::<Vec<String>>() {
            Box::new(values.to_vec())
        } else if r#type == TypeId::of::<bool>() {
            Box::new(
                values
                    .first()
                    .map(|v| v == "true")
                    .unwrap_or(false),
            )
        } else if r#type == TypeId::of::<i32>() {
            Box::new(
                values
                    .first()
                    .and_then(|v| v.parse::<i32>().ok())
                    .unwrap_or(0),
            )
        } else if r#type == TypeId::of::<i64>() {
            Box::new(
                values
                    .first()
                    .and_then(|v| v.parse::<i64>().ok())
                    .unwrap_or(0),
            )
        } else {
            panic!("Unsupported param type for deserialization: {:?}", r#type)
        };
        *boxed.downcast::<T>().unwrap()
    }
}

impl Serializer for JsonSerialization {}
impl Deserializer for JsonSerialization {}
impl Serialization for JsonSerialization {}

// --- Pet ---

fn pet_to_json(pet: &Pet) -> serde_json::Value {
    serde_json::json!({
        "id": pet.id,
        "category": pet.category.as_ref().map(category_to_json),
        "name": pet.name,
        "photoUrls": pet.photo_urls,
        "tags": pet.tags.as_ref().map(|tags| tags.iter().map(tag_to_json).collect::<Vec<_>>()),
        "status": pet.status.as_ref().map(|s| s.label()),
    })
}

fn json_to_pet(v: &serde_json::Value) -> Pet {
    Pet {
        id: v.get("id").and_then(|v| v.as_i64()),
        category: v.get("category").and_then(|v| if v.is_null() { None } else { Some(json_to_category(v)) }),
        name: v.get("name").and_then(|v| v.as_str()).unwrap_or_default().to_string(),
        photo_urls: v
            .get("photoUrls")
            .and_then(|v| v.as_array())
            .map(|a| a.iter().filter_map(|v| v.as_str().map(String::from)).collect())
            .unwrap_or_default(),
        tags: v.get("tags").and_then(|v| {
            if v.is_null() { None } else { v.as_array().map(|a| a.iter().map(json_to_tag).collect()) }
        }),
        status: v
            .get("status")
            .and_then(|v| v.as_str())
            .and_then(PetStatus::from_label),
    }
}

// --- Category ---

fn category_to_json(cat: &Category) -> serde_json::Value {
    serde_json::json!({
        "id": cat.id,
        "name": cat.name,
    })
}

fn json_to_category(v: &serde_json::Value) -> Category {
    Category {
        id: v.get("id").and_then(|v| v.as_i64()),
        name: v.get("name").and_then(|v| v.as_str()).map(String::from),
    }
}

// --- Tag ---

fn tag_to_json(tag: &Tag) -> serde_json::Value {
    serde_json::json!({
        "id": tag.id,
        "name": tag.name,
    })
}

fn json_to_tag(v: &serde_json::Value) -> Tag {
    Tag {
        id: v.get("id").and_then(|v| v.as_i64()),
        name: v.get("name").and_then(|v| v.as_str()).map(String::from),
    }
}

// --- Order ---

fn order_to_json(order: &Order) -> serde_json::Value {
    serde_json::json!({
        "id": order.id,
        "petId": order.pet_id,
        "quantity": order.quantity,
        "shipDate": order.ship_date,
        "status": order.status.as_ref().map(|s| s.label()),
        "complete": order.complete,
    })
}

fn json_to_order(v: &serde_json::Value) -> Order {
    Order {
        id: v.get("id").and_then(|v| v.as_i64()),
        pet_id: v.get("petId").and_then(|v| v.as_i64()),
        quantity: v.get("quantity").and_then(|v| v.as_i64()).map(|n| n as i32),
        ship_date: v.get("shipDate").and_then(|v| v.as_str()).map(String::from),
        status: v
            .get("status")
            .and_then(|v| v.as_str())
            .and_then(OrderStatus::from_label),
        complete: v.get("complete").and_then(|v| v.as_bool()),
    }
}

// --- User ---

fn user_to_json(user: &User) -> serde_json::Value {
    serde_json::json!({
        "id": user.id,
        "username": user.username,
        "firstName": user.first_name,
        "lastName": user.last_name,
        "email": user.email,
        "password": user.password,
        "phone": user.phone,
        "userStatus": user.user_status,
    })
}

fn json_to_user(v: &serde_json::Value) -> User {
    User {
        id: v.get("id").and_then(|v| v.as_i64()),
        username: v.get("username").and_then(|v| v.as_str()).map(String::from),
        first_name: v.get("firstName").and_then(|v| v.as_str()).map(String::from),
        last_name: v.get("lastName").and_then(|v| v.as_str()).map(String::from),
        email: v.get("email").and_then(|v| v.as_str()).map(String::from),
        password: v.get("password").and_then(|v| v.as_str()).map(String::from),
        phone: v.get("phone").and_then(|v| v.as_str()).map(String::from),
        user_status: v.get("userStatus").and_then(|v| v.as_i64()).map(|n| n as i32),
    }
}

// --- ApiResponse ---

fn api_response_to_json(resp: &ApiResponse) -> serde_json::Value {
    serde_json::json!({
        "code": resp.code,
        "type": resp.r#type,
        "message": resp.message,
    })
}

fn json_to_api_response(v: &serde_json::Value) -> ApiResponse {
    ApiResponse {
        code: v.get("code").and_then(|v| v.as_i64()).map(|n| n as i32),
        r#type: v.get("type").and_then(|v| v.as_str()).map(String::from),
        message: v.get("message").and_then(|v| v.as_str()).map(String::from),
    }
}
