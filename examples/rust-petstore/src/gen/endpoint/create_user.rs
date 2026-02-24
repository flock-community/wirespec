use super::super::wirespec::*;
use regex;
use super::super::model::user::User;
#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
pub struct Path;
#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
pub struct Queries;
#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
pub struct RequestHeaders;
#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
#[serde(default)]
pub struct Request {
    #[serde(default, deserialize_with = "null_default")]
    pub path: Path,
    #[serde(default, deserialize_with = "null_default")]
    pub method: Method,
    #[serde(default, deserialize_with = "null_default")]
    pub queries: Queries,
    #[serde(default, deserialize_with = "null_default")]
    pub headers: RequestHeaders,
    #[serde(default, deserialize_with = "null_default")]
    pub body: User,
}
impl Request {
    pub fn new(body: User) -> Self {
        Request {
            path: Path {},
            method: Method::POST,
            queries: Queries {},
            headers: RequestHeaders {},
            body: body
        }
    }
}
#[derive(Debug, Clone, PartialEq, serde::Serialize, serde::Deserialize)]
pub enum Response {
    ResponseDefault(ResponseDefault),
}
impl From<ResponseDefault> for Response { fn from(value: ResponseDefault) -> Self { Response::ResponseDefault(value) } }
#[derive(Debug, Clone, PartialEq, serde::Serialize, serde::Deserialize)]
pub enum ResponsedXX {
    ResponseDefault(ResponseDefault),
}
#[derive(Debug, Clone, PartialEq, serde::Serialize, serde::Deserialize)]
pub enum ResponseUnit {
    ResponseDefault(ResponseDefault),
}
#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
pub struct ResponseDefaultHeaders;
#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
#[serde(default)]
pub struct ResponseDefault {
    #[serde(default, deserialize_with = "null_default")]
    pub status: i32,
    #[serde(default, deserialize_with = "null_default")]
    pub headers: ResponseDefaultHeaders,
    #[serde(default, deserialize_with = "null_default")]
    pub body: (),
}
impl ResponseDefault {
    pub fn new() -> Self {
        ResponseDefault {
            status: 0_i32,
            headers: ResponseDefaultHeaders {},
            body: ()
        }
    }
}
pub mod CreateUser {
    use super::*;
    pub fn to_raw_request(serialization: &impl Serializer, request: Request) -> RawRequest {
        return RawRequest { method: format!("{:?}", request.method), path: vec![String::from("user")], queries: std::collections::HashMap::new(), headers: std::collections::HashMap::new(), body: Some(serialization.serialize_body(&request.body, std::any::TypeId::of::<User>())) };
    }
    pub fn from_raw_request(serialization: &impl Deserializer, request: RawRequest) -> Request {
        return Request::new(request.body.as_ref().map(|it| serialization.deserialize_body(&it, std::any::TypeId::of::<User>())).expect("body is null"));
    }
    pub fn to_raw_response(serialization: &impl Serializer, response: Response) -> RawResponse {
        match response {
            Response::ResponseDefault(r) => {
                return RawResponse { status_code: r.status, headers: std::collections::HashMap::new(), body: None };
            }
        }
    }
    pub fn from_raw_response(serialization: &impl Deserializer, response: RawResponse) -> Response {
        match response.status_code {
            _ => {
                panic!("Cannot match response with status: {}", response.status_code);
            }
        }
    }
    pub trait Handler {
        fn create_user(&self, request: Request) -> Response;
    }
    impl<C: Client> Handler for C {
        fn create_user(&self, request: Request) -> Response {
            let raw = to_raw_request(self.serialization(), request);
            let resp = self.transport().transport(&raw);
            from_raw_response(self.serialization(), resp)
        }
    }
    pub struct Api;
    impl Server for Api {
        type Req = Request;
        type Res = Response;
        fn path_template(&self) -> &'static str { "/user" }
        fn method(&self) -> Method { Method::POST }
    }
}
