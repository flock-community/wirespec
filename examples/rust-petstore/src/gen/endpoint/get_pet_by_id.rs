use super::super::wirespec::*;
use regex;
use super::super::model::pet::Pet;
#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
#[serde(default)]
pub struct Path {
    #[serde(default, deserialize_with = "null_default")]
    pub pet_id: i64,
}
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
    pub body: (),
}
impl Request {
    pub fn new(pet_id: i64) -> Self {
        Request {
            path: Path { pet_id: pet_id },
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
    Response400(Response400),
    Response404(Response404),
}
impl From<Response200> for Response { fn from(value: Response200) -> Self { Response::Response200(value) } }
impl From<Response400> for Response { fn from(value: Response400) -> Self { Response::Response400(value) } }
impl From<Response404> for Response { fn from(value: Response404) -> Self { Response::Response404(value) } }
#[derive(Debug, Clone, PartialEq, serde::Serialize, serde::Deserialize)]
pub enum Response2XX {
    Response200(Response200),
}
#[derive(Debug, Clone, PartialEq, serde::Serialize, serde::Deserialize)]
pub enum Response4XX {
    Response400(Response400),
    Response404(Response404),
}
#[derive(Debug, Clone, PartialEq, serde::Serialize, serde::Deserialize)]
pub enum ResponsePet {
    Response200(Response200),
}
#[derive(Debug, Clone, PartialEq, serde::Serialize, serde::Deserialize)]
pub enum ResponseUnit {
    Response400(Response400),
    Response404(Response404),
}
#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
pub struct Response200Headers;
#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
#[serde(default)]
pub struct Response200 {
    #[serde(default, deserialize_with = "null_default")]
    pub status: i32,
    #[serde(default, deserialize_with = "null_default")]
    pub headers: Response200Headers,
    #[serde(default, deserialize_with = "null_default")]
    pub body: Pet,
}
impl Response200 {
    pub fn new(body: Pet) -> Self {
        Response200 {
            status: 200_i32,
            headers: Response200Headers {},
            body: body
        }
    }
}
#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
pub struct Response400Headers;
#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
#[serde(default)]
pub struct Response400 {
    #[serde(default, deserialize_with = "null_default")]
    pub status: i32,
    #[serde(default, deserialize_with = "null_default")]
    pub headers: Response400Headers,
    #[serde(default, deserialize_with = "null_default")]
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
    #[serde(default, deserialize_with = "null_default")]
    pub status: i32,
    #[serde(default, deserialize_with = "null_default")]
    pub headers: Response404Headers,
    #[serde(default, deserialize_with = "null_default")]
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
pub mod GetPetById {
    use super::*;
    pub fn to_raw_request(serialization: &impl Serializer, request: Request) -> RawRequest {
        return RawRequest { method: format!("{:?}", request.method), path: vec![String::from("pet"), serialization.serialize_path(&request.path.pet_id, std::any::TypeId::of::<i64>())], queries: std::collections::HashMap::new(), headers: std::collections::HashMap::new(), body: None };
    }
    pub fn from_raw_request(serialization: &impl Deserializer, request: RawRequest) -> Request {
        return Request::new(serialization.deserialize_path(&request.path[1], std::any::TypeId::of::<i64>()));
    }
    pub fn to_raw_response(serialization: &impl Serializer, response: Response) -> RawResponse {
        match response {
            Response::Response200(r) => {
                return RawResponse { status_code: r.status, headers: std::collections::HashMap::new(), body: Some(serialization.serialize_body(&r.body, std::any::TypeId::of::<Pet>())) };
            }
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
            200_i32 => {
                return Response::Response200(Response200::new(response.body.as_ref().map(|it| serialization.deserialize_body(&it, std::any::TypeId::of::<Pet>())).expect("body is null")));
            }
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
        fn get_pet_by_id(&self, request: Request) -> Response;
    }
    impl<C: Client> Handler for C {
        fn get_pet_by_id(&self, request: Request) -> Response {
            let raw = to_raw_request(self.serialization(), request);
            let resp = self.transport().transport(&raw);
            from_raw_response(self.serialization(), resp)
        }
    }
    pub struct Api;
    impl Server for Api {
        type Req = Request;
        type Res = Response;
        fn path_template(&self) -> &'static str { "/pet/{petId}" }
        fn method(&self) -> Method { Method::GET }
    }
}
