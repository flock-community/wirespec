use std::collections::HashMap;
pub trait Model {
    fn validate(&self) -> Vec<String>;
}
pub trait Enum {
    fn label(&self) -> String;
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
pub trait BodySerializer {
    fn serialize_body<T>(&self, t: &T, r#type: &str) -> Vec<u8>;
}
pub trait BodyDeserializer {
    fn deserialize_body<T>(&self, raw: &[u8], r#type: &str) -> T;
}
pub trait BodySerialization: BodySerializer + BodyDeserializer {}
pub trait PathSerializer {
    fn serialize_path<T>(&self, t: &T, r#type: &str) -> String;
}
pub trait PathDeserializer {
    fn deserialize_path<T>(&self, raw: &str, r#type: &str) -> T;
}
pub trait PathSerialization: PathSerializer + PathDeserializer {}
pub trait ParamSerializer {
    fn serialize_param<T>(&self, value: &T, r#type: &str) -> Vec<String>;
}
pub trait ParamDeserializer {
    fn deserialize_param<T>(&self, values: &[String], r#type: &str) -> T;
}
pub trait ParamSerialization: ParamSerializer + ParamDeserializer {}
pub trait Serializer: BodySerializer + PathSerializer + ParamSerializer {}
pub trait Deserializer: BodyDeserializer + PathDeserializer + ParamDeserializer {}
pub trait Serialization: Serializer + Deserializer {}
#[derive(Debug, Clone, PartialEq, serde::Serialize, serde::Deserialize)]
pub struct RawRequest {
    pub method: String,
    pub path: Vec<String>,
    pub queries: HashMap<String, Vec<String>>,
    pub headers: HashMap<String, Vec<String>>,
    pub body: Option<Vec<u8>>,
}
#[derive(Debug, Clone, PartialEq, serde::Serialize, serde::Deserialize)]
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
