use super::super::wirespec::*;
use regex;
#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
pub struct Path;
#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
pub struct Queries;
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
    pub fn new() -> Self {
        Request {
        path: Path {},
        method: Method::GET,
        queries: Queries {},
        headers: RequestHeaders {},
        body: ()
        }
    }
}
#[derive(Debug, Clone, PartialEq, serde::Serialize, serde::Deserialize)]
pub enum Response {
    Response200(Response200),
}
impl From<Response200> for Response { fn from(value: Response200) -> Self { Response::Response200(value) } }
#[derive(Debug, Clone, PartialEq, serde::Serialize, serde::Deserialize)]
pub enum Response2XX {
    Response200(Response200),
}
#[derive(Debug, Clone, PartialEq, serde::Serialize, serde::Deserialize)]
pub enum ResponseMap {
    Response200(Response200),
}
#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
pub struct Response200Headers;
#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
#[serde(default)]
pub struct Response200 {
    pub status: i32,
    pub headers: Response200Headers,
    pub body: std::collections::HashMap<String, i32>,
}
impl Response200 {
    pub fn new(body: std::collections::HashMap<String, i32>) -> Self {
        Response200 {
        status: 200_i32,
        headers: Response200Headers {},
        body: body
        }
    }
}
pub mod GetInventory {
    use super::*;
    pub fn to_raw_request(serialization: &impl Serializer, request: Request) -> RawRequest {
        return RawRequest { method: format!("{:?}", request.method), path: vec![String::from("store"), String::from("inventory")], queries: std::collections::HashMap::new(), headers: std::collections::HashMap::new(), body: None };
}
    pub fn from_raw_request(serialization: &impl Deserializer, request: RawRequest) -> Request {
        return Request::new();
}
    pub fn to_raw_response(serialization: &impl Serializer, response: Response) -> RawResponse {
        match response {
            Response::Response200(r) => {
                return RawResponse { status_code: r.status, headers: std::collections::HashMap::new(), body: Some(serialization.serialize_body(&r.body, std::any::type_name::<std::collections::HashMap<String, i32>>())) };
}
}
}
    pub fn from_raw_response(serialization: &impl Deserializer, response: RawResponse) -> Response {
        match response.status_code {
            200_i32 => {
                return Response::Response200(Response200::new(response.body.as_ref().map(|it| serialization.deserialize_body(&it, std::any::type_name::<std::collections::HashMap<String, i32>>())).expect("body is null")));
}
            _ => {
                panic!("Cannot match response with status: {}", response.status_code);
}
}
}
    pub trait Handler {
        fn get_inventory(&self, request: Request) -> Response;
}
    impl<C: Client> Handler for C {
    fn get_inventory(&self, request: Request) -> Response {
        let raw = to_raw_request(self.serialization(), request);
        let resp = self.transport().transport(&raw);
        from_raw_response(self.serialization(), resp)
    }
}
    pub struct Api;
impl Server for Api {
    type Req = Request;
    type Res = Response;
    fn path_template(&self) -> &'static str { "/store/inventory" }
    fn method(&self) -> Method { Method::GET }
}
}
