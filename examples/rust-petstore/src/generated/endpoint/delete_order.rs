use super::super::wirespec::*;
use regex;
#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
#[serde(default)]
pub struct Path {
    pub order_id: i64,
}
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
    pub fn new(order_id: i64) -> Self {
        Request {
        path: Path { order_id: order_id },
        method: Method::DELETE,
        queries: Queries {},
        headers: RequestHeaders {},
        body: ()
        }
    }
}
#[derive(Debug, Clone, PartialEq, serde::Serialize, serde::Deserialize)]
pub enum Response {
    Response400(Response400),
    Response404(Response404),
}
impl From<Response400> for Response { fn from(value: Response400) -> Self { Response::Response400(value) } }
impl From<Response404> for Response { fn from(value: Response404) -> Self { Response::Response404(value) } }
#[derive(Debug, Clone, PartialEq, serde::Serialize, serde::Deserialize)]
pub enum Response4XX {
    Response400(Response400),
    Response404(Response404),
}
#[derive(Debug, Clone, PartialEq, serde::Serialize, serde::Deserialize)]
pub enum ResponseUnit {
    Response400(Response400),
    Response404(Response404),
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
#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
pub struct Response404Headers;
#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
#[serde(default)]
pub struct Response404 {
    pub status: i32,
    pub headers: Response404Headers,
    pub body: (),
}
impl Response404 {
    pub fn new() -> Self {
        Response404 {
        status: 404_i32,
        headers: Response404Headers {},
        body: ()
        }
    }
}
pub mod DeleteOrder {
    use super::*;
    pub fn to_raw_request(serialization: &impl Serializer, request: Request) -> RawRequest {
        return RawRequest { method: format!("{:?}", request.method), path: vec![String::from("store"), String::from("order"), serialization.serialize_path(&request.path.order_id, std::any::type_name::<i64>())], queries: std::collections::HashMap::new(), headers: std::collections::HashMap::new(), body: None };
}
    pub fn from_raw_request(serialization: &impl Deserializer, request: RawRequest) -> Request {
        return Request::new(serialization.deserialize_path(&request.path[2], std::any::type_name::<i64>()));
}
    pub fn to_raw_response(serialization: &impl Serializer, response: Response) -> RawResponse {
        match response {
            Response::Response400(r) => {
                return RawResponse { status_code: r.status, headers: std::collections::HashMap::new(), body: None };
}
            Response::Response404(r) => {
                return RawResponse { status_code: r.status, headers: std::collections::HashMap::new(), body: None };
}
}
}
    pub fn from_raw_response(serialization: &impl Deserializer, response: RawResponse) -> Response {
        match response.status_code {
            400_i32 => {
                return Response::Response400(Response400::new());
}
            404_i32 => {
                return Response::Response404(Response404::new());
}
            _ => {
                panic!("Cannot match response with status: {}", response.status_code);
}
}
}
    pub trait Handler {
        fn delete_order(&self, request: Request) -> Response;
}
    impl<C: Client> Handler for C {
    fn delete_order(&self, request: Request) -> Response {
        let raw = to_raw_request(self.serialization(), request);
        let resp = self.transport().transport(&raw);
        from_raw_response(self.serialization(), resp)
    }
}
    pub struct Api;
impl Server for Api {
    type Req = Request;
    type Res = Response;
    fn path_template(&self) -> &'static str { "/store/order/{orderId}" }
    fn method(&self) -> Method { Method::DELETE }
}
}
