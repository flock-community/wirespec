package community.flock.wirespec.emitters.rust

import community.flock.wirespec.compiler.test.CompileChannelTest
import community.flock.wirespec.compiler.test.CompileComplexModelTest
import community.flock.wirespec.compiler.test.CompileEnumTest
import community.flock.wirespec.compiler.test.CompileFullEndpointTest
import community.flock.wirespec.compiler.test.CompileMinimalEndpointTest
import community.flock.wirespec.compiler.test.CompileNestedTypeTest
import community.flock.wirespec.compiler.test.CompileRefinedTest
import community.flock.wirespec.compiler.test.CompileTypeTest
import community.flock.wirespec.compiler.test.CompileUnionTest
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class RustIrEmitterTest {

    @Test
    fun compileEnumTest() {
        val rust = """
        |use super::super::wirespec::*;
        |use regex;
        |#[derive(Debug, Clone, PartialEq, serde::Serialize, serde::Deserialize)]
        |pub enum MyAwesomeEnum {
        |    #[serde(rename = "ONE")]
        |    ONE,
        |    #[serde(rename = "Two")]
        |    Two,
        |    #[serde(rename = "THREE_MORE")]
        |    THREEMORE,
        |    #[serde(rename = "UnitedKingdom")]
        |    UnitedKingdom,
        |    #[serde(rename = "-1")]
        |    _1,
        |    #[serde(rename = "0")]
        |    _0,
        |    #[serde(rename = "10")]
        |    _10,
        |    #[serde(rename = "-999")]
        |    _999,
        |    #[serde(rename = "88")]
        |    _88,
        |}
        |impl Enum for MyAwesomeEnum {
        |    fn label(&self) -> &str {
        |        match self {
        |            MyAwesomeEnum::ONE => "ONE",
        |            MyAwesomeEnum::Two => "Two",
        |            MyAwesomeEnum::THREEMORE => "THREE_MORE",
        |            MyAwesomeEnum::UnitedKingdom => "UnitedKingdom",
        |            MyAwesomeEnum::_1 => "-1",
        |            MyAwesomeEnum::_0 => "0",
        |            MyAwesomeEnum::_10 => "10",
        |            MyAwesomeEnum::_999 => "-999",
        |            MyAwesomeEnum::_88 => "88",
        |        }
        |    }
        |    fn from_label(s: &str) -> Option<Self> {
        |        match s {
        |            "ONE" => Some(MyAwesomeEnum::ONE),
        |            "Two" => Some(MyAwesomeEnum::Two),
        |            "THREE_MORE" => Some(MyAwesomeEnum::THREEMORE),
        |            "UnitedKingdom" => Some(MyAwesomeEnum::UnitedKingdom),
        |            "-1" => Some(MyAwesomeEnum::_1),
        |            "0" => Some(MyAwesomeEnum::_0),
        |            "10" => Some(MyAwesomeEnum::_10),
        |            "-999" => Some(MyAwesomeEnum::_999),
        |            "88" => Some(MyAwesomeEnum::_88),
        |            _ => None,
        |        }
        |    }
        |}
        |impl std::fmt::Display for MyAwesomeEnum {
        |    fn fmt(&self, f: &mut std::fmt::Formatter) -> std::fmt::Result {
        |        write!(f, "{}", self.label())
        |    }
        |}
        |
        |#![allow(warnings)]
        |pub mod model;
        |pub mod endpoint;
        |pub mod wirespec;
        |
        """.trimMargin()

        CompileEnumTest.compiler { RustIrEmitter() } shouldBeRight rust
    }

    @Test
    fun compileTypeTest() {
        val rust = """
        |use super::super::wirespec::*;
        |use regex;
        |#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
        |#[serde(default)]
        |pub struct Request {
        |    #[serde(rename = "type", default, deserialize_with = "null_default")]
        |    pub r#type: String,
        |    #[serde(default, deserialize_with = "null_default")]
        |    pub url: String,
        |    #[serde(rename = "BODY_TYPE")]
        |    pub body_type: Option<String>,
        |    #[serde(default, deserialize_with = "null_default")]
        |    pub params: Vec<String>,
        |    #[serde(default, deserialize_with = "null_default")]
        |    pub headers: std::collections::HashMap<String, String>,
        |    pub body: Option<std::collections::HashMap<String, Option<Vec<Option<String>>>>>,
        |}
        |impl Request {
        |    pub fn validate(&self) -> Vec<String> {
        |        return Vec::<String>::new();
        |    }
        |}
        |
        |#![allow(warnings)]
        |pub mod model;
        |pub mod endpoint;
        |pub mod wirespec;
        |
        """.trimMargin()

        CompileTypeTest.compiler { RustIrEmitter() } shouldBeRight rust
    }

    @Test
    fun compileChannelTest() {
        val rust = """
        |use super::super::wirespec::*;
        |use regex;
        |pub trait Queue : Wirespec.Channel {
        |    fn invoke(message: String);
        |}
        |
        |#![allow(warnings)]
        |pub mod model;
        |pub mod endpoint;
        |pub mod wirespec;
        |
        """.trimMargin()

        CompileChannelTest.compiler { RustIrEmitter() } shouldBeRight rust
    }

    @Test
    fun compileRefinedTest() {
        val rust = """
        |use super::super::wirespec::*;
        |use regex;
        |#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
        |#[serde(default)]
        |pub struct TodoId {
        |    #[serde(default, deserialize_with = "null_default")]
        |    pub value: String,
        |}
        |impl TodoId {
        |    pub fn validate(&self) -> bool {
        |        return regex::Regex::new(r"^[0-9a-fA-F]{8}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{12}${'$'}").unwrap().is_match(&self.value);
        |    }
        |    pub fn to_string(&self) -> String {
        |        return self.value.clone();
        |    }
        |}
        |
        |use super::super::wirespec::*;
        |use regex;
        |#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
        |#[serde(default)]
        |pub struct TodoNoRegex {
        |    #[serde(default, deserialize_with = "null_default")]
        |    pub value: String,
        |}
        |impl TodoNoRegex {
        |    pub fn validate(&self) -> bool {
        |        return true;
        |    }
        |    pub fn to_string(&self) -> String {
        |        return self.value.clone();
        |    }
        |}
        |
        |use super::super::wirespec::*;
        |use regex;
        |#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
        |#[serde(default)]
        |pub struct TestInt {
        |    #[serde(default, deserialize_with = "null_default")]
        |    pub value: i64,
        |}
        |impl TestInt {
        |    pub fn validate(&self) -> bool {
        |        return true;
        |    }
        |    pub fn to_string(&self) -> String {
        |        return format!("{}", self.value);
        |    }
        |}
        |
        |use super::super::wirespec::*;
        |use regex;
        |#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
        |#[serde(default)]
        |pub struct TestInt0 {
        |    #[serde(default, deserialize_with = "null_default")]
        |    pub value: i64,
        |}
        |impl TestInt0 {
        |    pub fn validate(&self) -> bool {
        |        return true;
        |    }
        |    pub fn to_string(&self) -> String {
        |        return format!("{}", self.value);
        |    }
        |}
        |
        |use super::super::wirespec::*;
        |use regex;
        |#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
        |#[serde(default)]
        |pub struct TestInt1 {
        |    #[serde(default, deserialize_with = "null_default")]
        |    pub value: i64,
        |}
        |impl TestInt1 {
        |    pub fn validate(&self) -> bool {
        |        return 0 <= self.value;
        |    }
        |    pub fn to_string(&self) -> String {
        |        return format!("{}", self.value);
        |    }
        |}
        |
        |use super::super::wirespec::*;
        |use regex;
        |#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
        |#[serde(default)]
        |pub struct TestInt2 {
        |    #[serde(default, deserialize_with = "null_default")]
        |    pub value: i64,
        |}
        |impl TestInt2 {
        |    pub fn validate(&self) -> bool {
        |        return 1 <= self.value && self.value <= 3;
        |    }
        |    pub fn to_string(&self) -> String {
        |        return format!("{}", self.value);
        |    }
        |}
        |
        |use super::super::wirespec::*;
        |use regex;
        |#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
        |#[serde(default)]
        |pub struct TestNum {
        |    #[serde(default, deserialize_with = "null_default")]
        |    pub value: f64,
        |}
        |impl TestNum {
        |    pub fn validate(&self) -> bool {
        |        return true;
        |    }
        |    pub fn to_string(&self) -> String {
        |        return format!("{}", self.value);
        |    }
        |}
        |
        |use super::super::wirespec::*;
        |use regex;
        |#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
        |#[serde(default)]
        |pub struct TestNum0 {
        |    #[serde(default, deserialize_with = "null_default")]
        |    pub value: f64,
        |}
        |impl TestNum0 {
        |    pub fn validate(&self) -> bool {
        |        return true;
        |    }
        |    pub fn to_string(&self) -> String {
        |        return format!("{}", self.value);
        |    }
        |}
        |
        |use super::super::wirespec::*;
        |use regex;
        |#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
        |#[serde(default)]
        |pub struct TestNum1 {
        |    #[serde(default, deserialize_with = "null_default")]
        |    pub value: f64,
        |}
        |impl TestNum1 {
        |    pub fn validate(&self) -> bool {
        |        return self.value <= 0.5;
        |    }
        |    pub fn to_string(&self) -> String {
        |        return format!("{}", self.value);
        |    }
        |}
        |
        |use super::super::wirespec::*;
        |use regex;
        |#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
        |#[serde(default)]
        |pub struct TestNum2 {
        |    #[serde(default, deserialize_with = "null_default")]
        |    pub value: f64,
        |}
        |impl TestNum2 {
        |    pub fn validate(&self) -> bool {
        |        return -0.2 <= self.value && self.value <= 0.5;
        |    }
        |    pub fn to_string(&self) -> String {
        |        return format!("{}", self.value);
        |    }
        |}
        |
        |#![allow(warnings)]
        |pub mod model;
        |pub mod endpoint;
        |pub mod wirespec;
        |
        """.trimMargin()

        CompileRefinedTest.compiler { RustIrEmitter() } shouldBeRight rust
    }

    @Test
    fun compileUnionTest() {
        val rust = """
        |use super::super::wirespec::*;
        |use regex;
        |#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
        |#[serde(default)]
        |pub struct UserAccountPassword {
        |    #[serde(default, deserialize_with = "null_default")]
        |    pub username: String,
        |    #[serde(default, deserialize_with = "null_default")]
        |    pub password: String,
        |}
        |impl UserAccountPassword {
        |    pub fn validate(&self) -> Vec<String> {
        |        return Vec::<String>::new();
        |    }
        |}
        |
        |use super::super::wirespec::*;
        |use regex;
        |#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
        |#[serde(default)]
        |pub struct UserAccountToken {
        |    #[serde(default, deserialize_with = "null_default")]
        |    pub token: String,
        |}
        |impl UserAccountToken {
        |    pub fn validate(&self) -> Vec<String> {
        |        return Vec::<String>::new();
        |    }
        |}
        |
        |use super::super::wirespec::*;
        |use regex;
        |use super::user_account::UserAccount;
        |#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
        |#[serde(default)]
        |pub struct User {
        |    #[serde(default, deserialize_with = "null_default")]
        |    pub username: String,
        |    #[serde(default, deserialize_with = "null_default")]
        |    pub account: UserAccount,
        |}
        |impl User {
        |    pub fn validate(&self) -> Vec<String> {
        |        return Vec::<String>::new();
        |    }
        |}
        |
        |use super::super::wirespec::*;
        |use regex;
        |#[derive(Debug, Clone, PartialEq, serde::Serialize, serde::Deserialize)]
        |pub enum UserAccount {
        |    UserAccountPassword(UserAccountPassword),
        |    UserAccountToken(UserAccountToken),
        |}
        |
        |#![allow(warnings)]
        |pub mod model;
        |pub mod endpoint;
        |pub mod wirespec;
        |
        """.trimMargin()

        CompileUnionTest.compiler { RustIrEmitter() } shouldBeRight rust
    }

    @Test
    fun compileMinimalEndpointTest() {
        val rust = """
        |use super::super::wirespec::*;
        |use regex;
        |#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
        |#[serde(default)]
        |pub struct TodoDto {
        |    #[serde(default, deserialize_with = "null_default")]
        |    pub description: String,
        |}
        |impl TodoDto {
        |    pub fn validate(&self) -> Vec<String> {
        |        return Vec::<String>::new();
        |    }
        |}
        |
        |use super::super::wirespec::*;
        |use regex;
        |use super::super::model::todo_dto::TodoDto;
        |#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
        |pub struct Path;
        |#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
        |pub struct Queries;
        |#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
        |pub struct RequestHeaders;
        |#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
        |#[serde(default)]
        |pub struct Request {
        |    #[serde(default, deserialize_with = "null_default")]
        |    pub path: Path,
        |    #[serde(default, deserialize_with = "null_default")]
        |    pub method: Method,
        |    #[serde(default, deserialize_with = "null_default")]
        |    pub queries: Queries,
        |    #[serde(default, deserialize_with = "null_default")]
        |    pub headers: RequestHeaders,
        |    #[serde(default, deserialize_with = "null_default")]
        |    pub body: (),
        |}
        |impl Request {
        |    pub fn new() -> Self {
        |        Request {
        |            path: Path {},
        |            method: Method::GET,
        |            queries: Queries {},
        |            headers: RequestHeaders {},
        |            body: ()
        |        }
        |    }
        |}
        |#[derive(Debug, Clone, PartialEq, serde::Serialize, serde::Deserialize)]
        |pub enum Response {
        |    Response200(Response200),
        |}
        |impl From<Response200> for Response { fn from(value: Response200) -> Self { Response::Response200(value) } }
        |#[derive(Debug, Clone, PartialEq, serde::Serialize, serde::Deserialize)]
        |pub enum Response2XX {
        |    Response200(Response200),
        |}
        |#[derive(Debug, Clone, PartialEq, serde::Serialize, serde::Deserialize)]
        |pub enum ResponseListTodoDto {
        |    Response200(Response200),
        |}
        |#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
        |pub struct Response200Headers;
        |#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
        |#[serde(default)]
        |pub struct Response200 {
        |    #[serde(default, deserialize_with = "null_default")]
        |    pub status: i32,
        |    #[serde(default, deserialize_with = "null_default")]
        |    pub headers: Response200Headers,
        |    #[serde(default, deserialize_with = "null_default")]
        |    pub body: Vec<TodoDto>,
        |}
        |impl Response200 {
        |    pub fn new(body: Vec<TodoDto>) -> Self {
        |        Response200 {
        |            status: 200_i32,
        |            headers: Response200Headers {},
        |            body: body
        |        }
        |    }
        |}
        |pub mod GetTodos {
        |    use super::*;
        |    pub fn to_raw_request(serialization: &impl Serializer, request: Request) -> RawRequest {
        |        return RawRequest { method: format!("{:?}", request.method), path: vec![String::from("todos")], queries: std::collections::HashMap::new(), headers: std::collections::HashMap::new(), body: None };
        |    }
        |    pub fn from_raw_request(serialization: &impl Deserializer, request: RawRequest) -> Request {
        |        return Request::new();
        |    }
        |    pub fn to_raw_response(serialization: &impl Serializer, response: Response) -> RawResponse {
        |        match response {
        |            Response::Response200(r) => {
        |                return RawResponse { status_code: r.status, headers: std::collections::HashMap::new(), body: Some(serialization.serialize_body(&r.body, std::any::TypeId::of::<Vec<TodoDto>>())) };
        |            }
        |        }
        |    }
        |    pub fn from_raw_response(serialization: &impl Deserializer, response: RawResponse) -> Response {
        |        match response.status_code {
        |            200_i32 => {
        |                return Response::Response200(Response200::new(response.body.as_ref().map(|it| serialization.deserialize_body(&it, std::any::TypeId::of::<Vec<TodoDto>>())).expect("body is null")));
        |            }
        |            _ => {
        |                panic!("Cannot match response with status: {}", response.status_code);
        |            }
        |        }
        |    }
        |    pub trait Handler {
        |        fn get_todos(&self, request: Request) -> Response;
        |    }
        |    impl<C: Client> Handler for C {
        |        fn get_todos(&self, request: Request) -> Response {
        |            let raw = to_raw_request(self.serialization(), request);
        |            let resp = self.transport().transport(&raw);
        |            from_raw_response(self.serialization(), resp)
        |        }
        |    }
        |    pub struct Api;
        |    impl Server for Api {
        |        type Req = Request;
        |        type Res = Response;
        |        fn path_template(&self) -> &'static str { "/todos" }
        |        fn method(&self) -> Method { Method::GET }
        |    }
        |}
        |
        |#![allow(warnings)]
        |pub mod model;
        |pub mod endpoint;
        |pub mod wirespec;
        |
        """.trimMargin()

        CompileMinimalEndpointTest.compiler { RustIrEmitter() } shouldBeRight rust
    }

    @Test
    fun compileFullEndpointTest() {
        val rust = """
        |use super::super::wirespec::*;
        |use regex;
        |#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
        |#[serde(default)]
        |pub struct PotentialTodoDto {
        |    #[serde(default, deserialize_with = "null_default")]
        |    pub name: String,
        |    #[serde(default, deserialize_with = "null_default")]
        |    pub done: bool,
        |}
        |impl PotentialTodoDto {
        |    pub fn validate(&self) -> Vec<String> {
        |        return Vec::<String>::new();
        |    }
        |}
        |
        |use super::super::wirespec::*;
        |use regex;
        |#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
        |#[serde(default)]
        |pub struct Token {
        |    #[serde(default, deserialize_with = "null_default")]
        |    pub iss: String,
        |}
        |impl Token {
        |    pub fn validate(&self) -> Vec<String> {
        |        return Vec::<String>::new();
        |    }
        |}
        |
        |use super::super::wirespec::*;
        |use regex;
        |#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
        |#[serde(default)]
        |pub struct TodoDto {
        |    #[serde(default, deserialize_with = "null_default")]
        |    pub id: String,
        |    #[serde(default, deserialize_with = "null_default")]
        |    pub name: String,
        |    #[serde(default, deserialize_with = "null_default")]
        |    pub done: bool,
        |}
        |impl TodoDto {
        |    pub fn validate(&self) -> Vec<String> {
        |        return Vec::<String>::new();
        |    }
        |}
        |
        |use super::super::wirespec::*;
        |use regex;
        |#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
        |#[serde(default)]
        |pub struct Error {
        |    #[serde(default, deserialize_with = "null_default")]
        |    pub code: i64,
        |    #[serde(default, deserialize_with = "null_default")]
        |    pub description: String,
        |}
        |impl Error {
        |    pub fn validate(&self) -> Vec<String> {
        |        return Vec::<String>::new();
        |    }
        |}
        |
        |use super::super::wirespec::*;
        |use regex;
        |use super::super::model::token::Token;
        |use super::super::model::potential_todo_dto::PotentialTodoDto;
        |use super::super::model::todo_dto::TodoDto;
        |use super::super::model::error::Error;
        |#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
        |#[serde(default)]
        |pub struct Path {
        |    #[serde(default, deserialize_with = "null_default")]
        |    pub id: String,
        |}
        |#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
        |#[serde(default)]
        |pub struct Queries {
        |    #[serde(default, deserialize_with = "null_default")]
        |    pub done: bool,
        |    pub name: Option<String>,
        |}
        |#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
        |#[serde(default)]
        |pub struct RequestHeaders {
        |    #[serde(default, deserialize_with = "null_default")]
        |    pub token: Token,
        |    #[serde(rename = "refreshToken")]
        |    pub refresh_token: Option<Token>,
        |}
        |#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
        |#[serde(default)]
        |pub struct Request {
        |    #[serde(default, deserialize_with = "null_default")]
        |    pub path: Path,
        |    #[serde(default, deserialize_with = "null_default")]
        |    pub method: Method,
        |    #[serde(default, deserialize_with = "null_default")]
        |    pub queries: Queries,
        |    #[serde(default, deserialize_with = "null_default")]
        |    pub headers: RequestHeaders,
        |    #[serde(default, deserialize_with = "null_default")]
        |    pub body: PotentialTodoDto,
        |}
        |impl Request {
        |    pub fn new(id: String, done: bool, name: Option<String>, token: Token, refresh_token: Option<Token>, body: PotentialTodoDto) -> Self {
        |        Request {
        |            path: Path { id: id },
        |            method: Method::PUT,
        |            queries: Queries { done: done, name: name },
        |            headers: RequestHeaders { token: token, refresh_token: refresh_token },
        |            body: body
        |        }
        |    }
        |}
        |#[derive(Debug, Clone, PartialEq, serde::Serialize, serde::Deserialize)]
        |pub enum Response {
        |    Response200(Response200),
        |    Response201(Response201),
        |    Response500(Response500),
        |}
        |impl From<Response200> for Response { fn from(value: Response200) -> Self { Response::Response200(value) } }
        |impl From<Response201> for Response { fn from(value: Response201) -> Self { Response::Response201(value) } }
        |impl From<Response500> for Response { fn from(value: Response500) -> Self { Response::Response500(value) } }
        |#[derive(Debug, Clone, PartialEq, serde::Serialize, serde::Deserialize)]
        |pub enum Response2XX {
        |    Response200(Response200),
        |    Response201(Response201),
        |}
        |#[derive(Debug, Clone, PartialEq, serde::Serialize, serde::Deserialize)]
        |pub enum Response5XX {
        |    Response500(Response500),
        |}
        |#[derive(Debug, Clone, PartialEq, serde::Serialize, serde::Deserialize)]
        |pub enum ResponseTodoDto {
        |    Response200(Response200),
        |    Response201(Response201),
        |}
        |#[derive(Debug, Clone, PartialEq, serde::Serialize, serde::Deserialize)]
        |pub enum ResponseError {
        |    Response500(Response500),
        |}
        |#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
        |pub struct Response200Headers;
        |#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
        |#[serde(default)]
        |pub struct Response200 {
        |    #[serde(default, deserialize_with = "null_default")]
        |    pub status: i32,
        |    #[serde(default, deserialize_with = "null_default")]
        |    pub headers: Response200Headers,
        |    #[serde(default, deserialize_with = "null_default")]
        |    pub body: TodoDto,
        |}
        |impl Response200 {
        |    pub fn new(body: TodoDto) -> Self {
        |        Response200 {
        |            status: 200_i32,
        |            headers: Response200Headers {},
        |            body: body
        |        }
        |    }
        |}
        |#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
        |#[serde(default)]
        |pub struct Response201Headers {
        |    #[serde(default, deserialize_with = "null_default")]
        |    pub token: Token,
        |    #[serde(rename = "refreshToken")]
        |    pub refresh_token: Option<Token>,
        |}
        |#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
        |#[serde(default)]
        |pub struct Response201 {
        |    #[serde(default, deserialize_with = "null_default")]
        |    pub status: i32,
        |    #[serde(default, deserialize_with = "null_default")]
        |    pub headers: Response201Headers,
        |    #[serde(default, deserialize_with = "null_default")]
        |    pub body: TodoDto,
        |}
        |impl Response201 {
        |    pub fn new(token: Token, refresh_token: Option<Token>, body: TodoDto) -> Self {
        |        Response201 {
        |            status: 201_i32,
        |            headers: Response201Headers { token: token, refresh_token: refresh_token },
        |            body: body
        |        }
        |    }
        |}
        |#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
        |pub struct Response500Headers;
        |#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
        |#[serde(default)]
        |pub struct Response500 {
        |    #[serde(default, deserialize_with = "null_default")]
        |    pub status: i32,
        |    #[serde(default, deserialize_with = "null_default")]
        |    pub headers: Response500Headers,
        |    #[serde(default, deserialize_with = "null_default")]
        |    pub body: Error,
        |}
        |impl Response500 {
        |    pub fn new(body: Error) -> Self {
        |        Response500 {
        |            status: 500_i32,
        |            headers: Response500Headers {},
        |            body: body
        |        }
        |    }
        |}
        |pub mod PutTodo {
        |    use super::*;
        |    pub fn to_raw_request(serialization: &impl Serializer, request: Request) -> RawRequest {
        |        return RawRequest { method: format!("{:?}", request.method), path: vec![String::from("todos"), serialization.serialize_path(&request.path.id, std::any::TypeId::of::<String>())], queries: std::collections::HashMap::from([(String::from("done"), serialization.serialize_param(&request.queries.done, std::any::TypeId::of::<bool>())), (String::from("name"), request.queries.name.as_ref().map(|it| serialization.serialize_param(&it, std::any::TypeId::of::<String>())).unwrap_or(Vec::<String>::new()))]), headers: std::collections::HashMap::from([(String::from("token"), serialization.serialize_param(&request.headers.token, std::any::TypeId::of::<Token>())), (String::from("refreshToken"), request.headers.refresh_token.as_ref().map(|it| serialization.serialize_param(&it, std::any::TypeId::of::<Token>())).unwrap_or(Vec::<String>::new()))]), body: Some(serialization.serialize_body(&request.body, std::any::TypeId::of::<PotentialTodoDto>())) };
        |    }
        |    pub fn from_raw_request(serialization: &impl Deserializer, request: RawRequest) -> Request {
        |        return Request::new(serialization.deserialize_path(&request.path[1], std::any::TypeId::of::<String>()), request.queries.get("done").as_ref().map(|it| serialization.deserialize_param(&it, std::any::TypeId::of::<bool>())).expect("Param done cannot be null"), request.queries.get("name").as_ref().map(|it| serialization.deserialize_param(&it, std::any::TypeId::of::<String>())), request.headers.get("token").as_ref().map(|it| serialization.deserialize_param(&it, std::any::TypeId::of::<Token>())).expect("Param token cannot be null"), request.headers.get("refreshToken").as_ref().map(|it| serialization.deserialize_param(&it, std::any::TypeId::of::<Token>())), request.body.as_ref().map(|it| serialization.deserialize_body(&it, std::any::TypeId::of::<PotentialTodoDto>())).expect("body is null"));
        |    }
        |    pub fn to_raw_response(serialization: &impl Serializer, response: Response) -> RawResponse {
        |        match response {
        |            Response::Response200(r) => {
        |                return RawResponse { status_code: r.status, headers: std::collections::HashMap::new(), body: Some(serialization.serialize_body(&r.body, std::any::TypeId::of::<TodoDto>())) };
        |            }
        |            Response::Response201(r) => {
        |                return RawResponse { status_code: r.status, headers: std::collections::HashMap::from([(String::from("token"), serialization.serialize_param(&r.headers.token, std::any::TypeId::of::<Token>())), (String::from("refreshToken"), r.headers.refresh_token.as_ref().map(|it| serialization.serialize_param(&it, std::any::TypeId::of::<Token>())).unwrap_or(Vec::<String>::new()))]), body: Some(serialization.serialize_body(&r.body, std::any::TypeId::of::<TodoDto>())) };
        |            }
        |            Response::Response500(r) => {
        |                return RawResponse { status_code: r.status, headers: std::collections::HashMap::new(), body: Some(serialization.serialize_body(&r.body, std::any::TypeId::of::<Error>())) };
        |            }
        |        }
        |    }
        |    pub fn from_raw_response(serialization: &impl Deserializer, response: RawResponse) -> Response {
        |        match response.status_code {
        |            200_i32 => {
        |                return Response::Response200(Response200::new(response.body.as_ref().map(|it| serialization.deserialize_body(&it, std::any::TypeId::of::<TodoDto>())).expect("body is null")));
        |            }
        |            201_i32 => {
        |                return Response::Response201(Response201::new(response.headers.get("token").as_ref().map(|it| serialization.deserialize_param(&it, std::any::TypeId::of::<Token>())).expect("Param token cannot be null"), response.headers.get("refreshToken").as_ref().map(|it| serialization.deserialize_param(&it, std::any::TypeId::of::<Token>())), response.body.as_ref().map(|it| serialization.deserialize_body(&it, std::any::TypeId::of::<TodoDto>())).expect("body is null")));
        |            }
        |            500_i32 => {
        |                return Response::Response500(Response500::new(response.body.as_ref().map(|it| serialization.deserialize_body(&it, std::any::TypeId::of::<Error>())).expect("body is null")));
        |            }
        |            _ => {
        |                panic!("Cannot match response with status: {}", response.status_code);
        |            }
        |        }
        |    }
        |    pub trait Handler {
        |        fn put_todo(&self, request: Request) -> Response;
        |    }
        |    impl<C: Client> Handler for C {
        |        fn put_todo(&self, request: Request) -> Response {
        |            let raw = to_raw_request(self.serialization(), request);
        |            let resp = self.transport().transport(&raw);
        |            from_raw_response(self.serialization(), resp)
        |        }
        |    }
        |    pub struct Api;
        |    impl Server for Api {
        |        type Req = Request;
        |        type Res = Response;
        |        fn path_template(&self) -> &'static str { "/todos/{id}" }
        |        fn method(&self) -> Method { Method::PUT }
        |    }
        |}
        |
        |#![allow(warnings)]
        |pub mod model;
        |pub mod endpoint;
        |pub mod wirespec;
        |
        """.trimMargin()

        CompileFullEndpointTest.compiler { RustIrEmitter() } shouldBeRight rust
    }

    @Test
    fun compileNestedTypeTest() {
        val rust = """
        |use super::super::wirespec::*;
        |use regex;
        |#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
        |#[serde(default)]
        |pub struct DutchPostalCode {
        |    #[serde(default, deserialize_with = "null_default")]
        |    pub value: String,
        |}
        |impl DutchPostalCode {
        |    pub fn validate(&self) -> bool {
        |        return regex::Regex::new(r"^([0-9]{4}[A-Z]{2})${'$'}").unwrap().is_match(&self.value);
        |    }
        |    pub fn to_string(&self) -> String {
        |        return self.value.clone();
        |    }
        |}
        |
        |use super::super::wirespec::*;
        |use regex;
        |use super::dutch_postal_code::DutchPostalCode;
        |#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
        |#[serde(default)]
        |pub struct Address {
        |    #[serde(default, deserialize_with = "null_default")]
        |    pub street: String,
        |    #[serde(rename = "houseNumber", default, deserialize_with = "null_default")]
        |    pub house_number: i64,
        |    #[serde(rename = "postalCode", default, deserialize_with = "null_default")]
        |    pub postal_code: DutchPostalCode,
        |}
        |impl Address {
        |    pub fn validate(&self) -> Vec<String> {
        |        return if !self.postal_code.validate() { vec![String::from("postalCode")] } else { Vec::<String>::new() };
        |    }
        |}
        |
        |use super::super::wirespec::*;
        |use regex;
        |use super::address::Address;
        |#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
        |#[serde(default)]
        |pub struct Person {
        |    #[serde(default, deserialize_with = "null_default")]
        |    pub name: String,
        |    #[serde(default, deserialize_with = "null_default")]
        |    pub address: Address,
        |    #[serde(default, deserialize_with = "null_default")]
        |    pub tags: Vec<String>,
        |}
        |impl Person {
        |    pub fn validate(&self) -> Vec<String> {
        |        return self.address.validate().iter().map(|e| format!("address.{}", e)).collect::<Vec<_>>();
        |    }
        |}
        |
        |#![allow(warnings)]
        |pub mod model;
        |pub mod endpoint;
        |pub mod wirespec;
        |
        """.trimMargin()

        CompileNestedTypeTest.compiler { RustIrEmitter() } shouldBeRight rust
    }

    @Test
    fun compileComplexModelTest() {
        val rust = """
        |use super::super::wirespec::*;
        |use regex;
        |#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
        |#[serde(default)]
        |pub struct Email {
        |    #[serde(default, deserialize_with = "null_default")]
        |    pub value: String,
        |}
        |impl Email {
        |    pub fn validate(&self) -> bool {
        |        return regex::Regex::new(r"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}${'$'}").unwrap().is_match(&self.value);
        |    }
        |    pub fn to_string(&self) -> String {
        |        return self.value.clone();
        |    }
        |}
        |
        |use super::super::wirespec::*;
        |use regex;
        |#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
        |#[serde(default)]
        |pub struct PhoneNumber {
        |    #[serde(default, deserialize_with = "null_default")]
        |    pub value: String,
        |}
        |impl PhoneNumber {
        |    pub fn validate(&self) -> bool {
        |        return regex::Regex::new(r"^\+[1-9]\d{1,14}${'$'}").unwrap().is_match(&self.value);
        |    }
        |    pub fn to_string(&self) -> String {
        |        return self.value.clone();
        |    }
        |}
        |
        |use super::super::wirespec::*;
        |use regex;
        |#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
        |#[serde(default)]
        |pub struct Tag {
        |    #[serde(default, deserialize_with = "null_default")]
        |    pub value: String,
        |}
        |impl Tag {
        |    pub fn validate(&self) -> bool {
        |        return regex::Regex::new(r"^[a-z][a-z0-9-]{0,19}${'$'}").unwrap().is_match(&self.value);
        |    }
        |    pub fn to_string(&self) -> String {
        |        return self.value.clone();
        |    }
        |}
        |
        |use super::super::wirespec::*;
        |use regex;
        |#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
        |#[serde(default)]
        |pub struct EmployeeAge {
        |    #[serde(default, deserialize_with = "null_default")]
        |    pub value: i64,
        |}
        |impl EmployeeAge {
        |    pub fn validate(&self) -> bool {
        |        return 18 <= self.value && self.value <= 65;
        |    }
        |    pub fn to_string(&self) -> String {
        |        return format!("{}", self.value);
        |    }
        |}
        |
        |use super::super::wirespec::*;
        |use regex;
        |use super::email::Email;
        |use super::phone_number::PhoneNumber;
        |#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
        |#[serde(default)]
        |pub struct ContactInfo {
        |    #[serde(default, deserialize_with = "null_default")]
        |    pub email: Email,
        |    pub phone: Option<PhoneNumber>,
        |}
        |impl ContactInfo {
        |    pub fn validate(&self) -> Vec<String> {
        |        return vec![if !self.email.validate() { vec![String::from("email")] } else { Vec::<String>::new() }.as_slice(), self.phone.as_ref().map(|it| if !it.validate() { vec![String::from("phone")] } else { Vec::<String>::new() }).unwrap_or(Vec::<String>::new()).as_slice()].concat();
        |    }
        |}
        |
        |use super::super::wirespec::*;
        |use regex;
        |use super::employee_age::EmployeeAge;
        |use super::contact_info::ContactInfo;
        |use super::tag::Tag;
        |#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
        |#[serde(default)]
        |pub struct Employee {
        |    #[serde(default, deserialize_with = "null_default")]
        |    pub name: String,
        |    #[serde(default, deserialize_with = "null_default")]
        |    pub age: EmployeeAge,
        |    #[serde(rename = "contactInfo", default, deserialize_with = "null_default")]
        |    pub contact_info: ContactInfo,
        |    #[serde(default, deserialize_with = "null_default")]
        |    pub tags: Vec<Tag>,
        |}
        |impl Employee {
        |    pub fn validate(&self) -> Vec<String> {
        |        return vec![if !self.age.validate() { vec![String::from("age")] } else { Vec::<String>::new() }.as_slice(), self.contact_info.validate().iter().map(|e| format!("contactInfo.{}", e)).collect::<Vec<_>>().as_slice(), self.tags.iter().enumerate().flat_map(|(i, el)| if !el.validate() { vec![format!("tags[{}]", i)] } else { Vec::<String>::new() }).collect::<Vec<_>>().as_slice()].concat();
        |    }
        |}
        |
        |use super::super::wirespec::*;
        |use regex;
        |use super::employee::Employee;
        |#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
        |#[serde(default)]
        |pub struct Department {
        |    #[serde(default, deserialize_with = "null_default")]
        |    pub name: String,
        |    #[serde(default, deserialize_with = "null_default")]
        |    pub employees: Vec<Employee>,
        |}
        |impl Department {
        |    pub fn validate(&self) -> Vec<String> {
        |        return self.employees.iter().enumerate().flat_map(|(i, el)| el.validate().iter().map(|e| format!("employees[{}].{}", i, e)).collect::<Vec<_>>()).collect::<Vec<_>>();
        |    }
        |}
        |
        |use super::super::wirespec::*;
        |use regex;
        |use super::department::Department;
        |#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
        |#[serde(default)]
        |pub struct Company {
        |    #[serde(default, deserialize_with = "null_default")]
        |    pub name: String,
        |    #[serde(default, deserialize_with = "null_default")]
        |    pub departments: Vec<Department>,
        |}
        |impl Company {
        |    pub fn validate(&self) -> Vec<String> {
        |        return self.departments.iter().enumerate().flat_map(|(i, el)| el.validate().iter().map(|e| format!("departments[{}].{}", i, e)).collect::<Vec<_>>()).collect::<Vec<_>>();
        |    }
        |}
        |
        |#![allow(warnings)]
        |pub mod model;
        |pub mod endpoint;
        |pub mod wirespec;
        |
        """.trimMargin()

        CompileComplexModelTest.compiler { RustIrEmitter() } shouldBeRight rust
    }

    @Test
    fun sharedOutputTest() {
        val expected = """
            |use std::any::TypeId;
            |use std::collections::HashMap;
            |
            |pub trait Model {
            |    fn validate(&self) -> Vec<String>;
            |}
            |
            |pub trait Enum: Sized {
            |    fn label(&self) -> &str;
            |    fn from_label(s: &str) -> Option<Self>;
            |}
            |
            |pub trait Endpoint {}
            |
            |pub trait Channel {}
            |
            |pub trait Refined<T> {
            |    fn value(&self) -> &T;
            |    fn validate(&self) -> bool;
            |}
            |
            |pub trait Path {}
            |
            |pub trait Queries {}
            |
            |pub trait Headers {}
            |
            |pub trait Handler {}
            |
            |#[derive(Debug, Clone, Default, PartialEq, serde::Serialize, serde::Deserialize)]
            |pub enum Method {
            |    #[default]
            |    GET,
            |    PUT,
            |    POST,
            |    DELETE,
            |    OPTIONS,
            |    HEAD,
            |    PATCH,
            |    TRACE,
            |}
            |
            |pub trait Request<T> {
            |    fn path(&self) -> &dyn Path;
            |    fn method(&self) -> &Method;
            |    fn queries(&self) -> &dyn Queries;
            |    fn headers(&self) -> &dyn RequestHeaders;
            |    fn body(&self) -> &T;
            |}
            |
            |pub trait RequestHeaders: Headers {}
            |
            |pub trait Response<T> {
            |    fn status(&self) -> i32;
            |    fn headers(&self) -> &dyn ResponseHeaders;
            |    fn body(&self) -> &T;
            |}
            |
            |pub trait ResponseHeaders: Headers {}
            |
            |pub trait BodySerializer {
            |    fn serialize_body<T: serde::Serialize>(&self, t: &T, r#type: TypeId) -> Vec<u8>;
            |}
            |
            |pub trait BodyDeserializer {
            |    fn deserialize_body<T: serde::de::DeserializeOwned>(&self, raw: &[u8], r#type: TypeId) -> T;
            |}
            |
            |pub trait BodySerialization: BodySerializer + BodyDeserializer {}
            |
            |pub trait PathSerializer {
            |    fn serialize_path<T: std::fmt::Display>(&self, t: &T, r#type: TypeId) -> String;
            |}
            |
            |pub trait PathDeserializer {
            |    fn deserialize_path<T: std::str::FromStr>(&self, raw: &str, r#type: TypeId) -> T where T::Err: std::fmt::Debug;
            |}
            |
            |pub trait PathSerialization: PathSerializer + PathDeserializer {}
            |
            |pub trait ParamSerializer {
            |    fn serialize_param<T: serde::Serialize>(&self, value: &T, r#type: TypeId) -> Vec<String>;
            |}
            |
            |pub trait ParamDeserializer {
            |    fn deserialize_param<T: serde::de::DeserializeOwned>(&self, values: &[String], r#type: TypeId) -> T;
            |}
            |
            |pub trait ParamSerialization: ParamSerializer + ParamDeserializer {}
            |
            |pub trait Serializer: BodySerializer + PathSerializer + ParamSerializer {}
            |
            |pub trait Deserializer: BodyDeserializer + PathDeserializer + ParamDeserializer {}
            |
            |pub trait Serialization: Serializer + Deserializer {}
            |
            |#[derive(Debug, Clone, PartialEq)]
            |pub struct RawRequest {
            |    pub method: String,
            |    pub path: Vec<String>,
            |    pub queries: HashMap<String, Vec<String>>,
            |    pub headers: HashMap<String, Vec<String>>,
            |    pub body: Option<Vec<u8>>,
            |}
            |
            |#[derive(Debug, Clone, PartialEq)]
            |pub struct RawResponse {
            |    pub status_code: i32,
            |    pub headers: HashMap<String, Vec<String>>,
            |    pub body: Option<Vec<u8>>,
            |}
            |
            |pub trait Transportation {
            |    fn transport(&self, request: &RawRequest) -> RawResponse;
            |}
            |
            |pub trait Client {
            |    type Transport: Transportation;
            |    type Ser: Serialization;
            |    fn transport(&self) -> &Self::Transport;
            |    fn serialization(&self) -> &Self::Ser;
            |}
            |
            |pub trait Server {
            |    type Req;
            |    type Res;
            |    fn path_template(&self) -> &'static str;
            |    fn method(&self) -> Method;
            |}
            |
            |pub fn null_default<'de, D, T>(deserializer: D) -> Result<T, D::Error>
            |where
            |    D: serde::Deserializer<'de>,
            |    T: Default + serde::Deserialize<'de>,
            |{
            |    use serde::Deserialize;
            |    Option::<T>::deserialize(deserializer).map(|opt| opt.unwrap_or_default())
            |}
            |
        """.trimMargin()

        val emitter = RustIrEmitter()
        emitter.shared.source shouldBe expected
    }
}
