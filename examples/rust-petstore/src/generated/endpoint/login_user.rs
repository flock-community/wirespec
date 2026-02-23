use super::super::wirespec::*;
use regex;
#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
pub struct Path;
#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
#[serde(default)]
pub struct Queries {
    pub username: String,
    pub password: String,
}
#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
pub struct RequestHeaders;
#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
#[serde(default)]
pub struct Request {
    pub path: Path,
    pub method: Method,
    pub queries: Queries,
    pub headers: RequestHeaders,
    pub body: (),
}
impl Request {
    pub fn new(username: String, password: String) -> Self {
        Request {
        path: Path {},
        method: Method::GET,
        queries: Queries { username: username, password: password },
        headers: RequestHeaders {},
        body: ()
        }
    }
}
#[derive(Debug, Clone, PartialEq, serde::Serialize, serde::Deserialize)]
pub enum Response {
    Response200(Response200),
    Response400(Response400),
}
impl From<Response200> for Response { fn from(value: Response200) -> Self { Response::Response200(value) } }
impl From<Response400> for Response { fn from(value: Response400) -> Self { Response::Response400(value) } }
#[derive(Debug, Clone, PartialEq, serde::Serialize, serde::Deserialize)]
pub enum Response2XX {
    Response200(Response200),
}
#[derive(Debug, Clone, PartialEq, serde::Serialize, serde::Deserialize)]
pub enum Response4XX {
    Response400(Response400),
}
#[derive(Debug, Clone, PartialEq, serde::Serialize, serde::Deserialize)]
pub enum ResponseString {
    Response200(Response200),
}
#[derive(Debug, Clone, PartialEq, serde::Serialize, serde::Deserialize)]
pub enum ResponseUnit {
    Response400(Response400),
}
#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
#[serde(default)]
pub struct Response200Headers {
    pub x_expires_after: String,
    pub x_rate_limit: i32,
}
#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
#[serde(default)]
pub struct Response200 {
    pub status: i32,
    pub headers: Response200Headers,
    pub body: String,
}
impl Response200 {
    pub fn new(x_expires_after: String, x_rate_limit: i32, body: String) -> Self {
        Response200 {
        status: 200_i32,
        headers: Response200Headers { x_expires_after: x_expires_after, x_rate_limit: x_rate_limit },
        body: body
        }
    }
}
#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
pub struct Response400Headers;
#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
#[serde(default)]
pub struct Response400 {
    pub status: i32,
    pub headers: Response400Headers,
    pub body: (),
}
impl Response400 {
    pub fn new() -> Self {
        Response400 {
        status: 400_i32,
        headers: Response400Headers {},
        body: ()
        }
    }
}
pub mod LoginUser {
    use super::*;
    pub fn to_raw_request(serialization: &impl Serializer, request: Request) -> RawRequest {
        return RawRequest { method: format!("{:?}", request.method), path: vec![String::from("user"), String::from("login")], queries: std::collections::HashMap::from([(String::from("username"), serialization.serialize_param(&request.queries.username, std::any::type_name::<String>())), (String::from("password"), serialization.serialize_param(&request.queries.password, std::any::type_name::<String>()))]), headers: std::collections::HashMap::new(), body: None };
}
    pub fn from_raw_request(serialization: &impl Deserializer, request: RawRequest) -> Request {
        return Request::new(request.queries.get("username").as_ref().map(|it| serialization.deserialize_param(&it, std::any::type_name::<String>())).expect("Param username cannot be null"), request.queries.get("password").as_ref().map(|it| serialization.deserialize_param(&it, std::any::type_name::<String>())).expect("Param password cannot be null"));
}
    pub fn to_raw_response(serialization: &impl Serializer, response: Response) -> RawResponse {
        match response {
            Response::Response200(r) => {
                return RawResponse { status_code: r.status, headers: std::collections::HashMap::from([(String::from("xExpiresAfter"), serialization.serialize_param(&r.headers.x_expires_after, std::any::type_name::<String>())), (String::from("xRateLimit"), serialization.serialize_param(&r.headers.x_rate_limit, std::any::type_name::<i32>()))]), body: Some(serialization.serialize_body(&r.body, std::any::type_name::<String>())) };
}
            Response::Response400(r) => {
                return RawResponse { status_code: r.status, headers: std::collections::HashMap::new(), body: None };
}
}
}
    pub fn from_raw_response(serialization: &impl Deserializer, response: RawResponse) -> Response {
        match response.status_code {
            200_i32 => {
                return Response::Response200(Response200::new(response.headers.get("xExpiresAfter").as_ref().map(|it| serialization.deserialize_param(&it, std::any::type_name::<String>())).expect("Param xExpiresAfter cannot be null"), response.headers.get("xRateLimit").as_ref().map(|it| serialization.deserialize_param(&it, std::any::type_name::<i32>())).expect("Param xRateLimit cannot be null"), response.body.as_ref().map(|it| serialization.deserialize_body(&it, std::any::type_name::<String>())).expect("body is null")));
}
            400_i32 => {
                return Response::Response400(Response400::new());
}
            _ => {
                panic!("Cannot match response with status: {}", response.status_code);
}
}
}
    pub trait Handler {
        fn login_user(&self, request: Request) -> Response;
}
    impl<C: Client> Handler for C {
    fn login_user(&self, request: Request) -> Response {
        let raw = to_raw_request(self.serialization(), request);
        let resp = self.transport().transport(&raw);
        from_raw_response(self.serialization(), resp)
    }
}
    pub struct Api;
impl Server for Api {
    type Req = Request;
    type Res = Response;
    fn path_template(&self) -> &'static str { "/user/login" }
    fn method(&self) -> Method { Method::GET }
}
}
