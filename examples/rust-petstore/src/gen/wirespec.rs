use std::any::TypeId;
use std::collections::HashMap;
pub trait Model {
    fn validate(&self) -> Vec<String>;
}
pub trait Enum: Sized {
    fn label(&self) -> &str;
    fn from_label(s: &str) -> Option<Self>;
}
pub trait Endpoint {}
pub trait Channel {}
pub trait Refined<T> {
    fn value(&self) -> &T;
    fn validate(&self) -> bool;
}
pub trait Path {}
pub trait Queries {}
pub trait Headers {}
pub trait Handler {}
#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
pub enum Method {
    #[default]
    GET,
    PUT,
    POST,
    DELETE,
    OPTIONS,
    HEAD,
    PATCH,
    TRACE,
}
pub trait Request<T> {
    fn path(&self) -> &dyn Path;
    fn method(&self) -> &Method;
    fn queries(&self) -> &dyn Queries;
    fn headers(&self) -> &dyn RequestHeaders;
    fn body(&self) -> &T;
}
pub trait RequestHeaders: Headers {}
pub trait Response<T> {
    fn status(&self) -> i32;
    fn headers(&self) -> &dyn ResponseHeaders;
    fn body(&self) -> &T;
}
pub trait ResponseHeaders: Headers {}
pub trait Serialize {
    fn serialize(&self) -> Vec<u8>;
}
pub trait Deserialize: Sized {
    fn deserialize(bytes: &[u8]) -> Result<Self, String>;
}
pub trait BodySerializer {
    fn serialize_body<T: Serialize>(&self, t: &T, r#type: TypeId) -> Vec<u8>;
}
pub trait BodyDeserializer {
    fn deserialize_body<T: Deserialize>(&self, raw: &[u8], r#type: TypeId) -> T;
}
pub trait BodySerialization: BodySerializer + BodyDeserializer {}
pub trait PathSerializer {
    fn serialize_path<T: std::fmt::Display>(&self, t: &T, r#type: TypeId) -> String;
}
pub trait PathDeserializer {
    fn deserialize_path<T: std::str::FromStr>(&self, raw: &str, r#type: TypeId) -> T where T::Err: std::fmt::Debug;
}
pub trait PathSerialization: PathSerializer + PathDeserializer {}
pub trait ParamSerializer {
    fn serialize_param<T: Serialize>(&self, value: &T, r#type: TypeId) -> Vec<String>;
}
pub trait ParamDeserializer {
    fn deserialize_param<T: Deserialize>(&self, values: &[String], r#type: TypeId) -> T;
}
pub trait ParamSerialization: ParamSerializer + ParamDeserializer {}
pub trait Serializer: BodySerializer + PathSerializer + ParamSerializer {}
pub trait Deserializer: BodyDeserializer + PathDeserializer + ParamDeserializer {}
pub trait Serialization: Serializer + Deserializer {}
#[derive(Debug, Clone, PartialEq)]
pub struct RawRequest {
    pub method: String,
    pub path: Vec<String>,
    pub queries: HashMap<String, Vec<String>>,
    pub headers: HashMap<String, Vec<String>>,
    pub body: Option<Vec<u8>>,
}
#[derive(Debug, Clone, PartialEq)]
pub struct RawResponse {
    pub status_code: i32,
    pub headers: HashMap<String, Vec<String>>,
    pub body: Option<Vec<u8>>,
}
pub trait Transportation {
    fn transport(&self, request: &RawRequest) -> RawResponse;
}
pub trait Client {
    type Transport: Transportation;
    type Ser: Serialization;
    fn transport(&self) -> &Self::Transport;
    fn serialization(&self) -> &Self::Ser;
}
pub trait Server {
    type Req;
    type Res;
    fn path_template(&self) -> &'static str;
    fn method(&self) -> Method;
}
pub fn null_default<'de, D, T>(deserializer: D) -> Result<T, D::Error>
where
    D: serde::Deserializer<'de>,
    T: Default + serde::Deserialize<'de>,
{
    use serde::Deserialize;
    Option::<T>::deserialize(deserializer).map(|opt| opt.unwrap_or_default())
}
