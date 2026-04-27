package community.flock.wirespec.emitters.rust

import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Enum
import community.flock.wirespec.compiler.core.parse.ast.Field
import community.flock.wirespec.compiler.core.parse.ast.FieldIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Module
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.compiler.core.parse.ast.Union
import community.flock.wirespec.compiler.test.CompileChannelTest
import community.flock.wirespec.compiler.test.CompileComplexModelTest
import community.flock.wirespec.compiler.test.CompileEnumTest
import community.flock.wirespec.compiler.test.CompileFullEndpointTest
import community.flock.wirespec.compiler.test.CompileMinimalEndpointTest
import community.flock.wirespec.compiler.test.CompileNestedTypeTest
import community.flock.wirespec.compiler.test.CompileRefinedTest
import community.flock.wirespec.compiler.test.CompileTypeTest
import community.flock.wirespec.compiler.test.CompileUnionTest
import community.flock.wirespec.compiler.utils.noLogger
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class RustIrEmitterTest {

    @Test
    fun compileEnumTest() {
        val rust = """
        |use super::super::wirespec::*;
        |use regex;
        |#[derive(Debug, Clone, PartialEq)]
        |pub enum MyAwesomeEnum {
        |    ONE,
        |    Two,
        |    THREEMORE,
        |    UnitedKingdom,
        |    _1,
        |    _0,
        |    _10,
        |    _999,
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
        |use super::super::wirespec::*;
        |use regex;
        |pub struct MyAwesomeEnumGenerator;
        |impl MyAwesomeEnumGenerator {
        |    pub fn generate(path: Vec<String>, generator: Wirespec.Generator) -> MyAwesomeEnum {
        |        return MyAwesomeEnum.value_of(generator.generate((path + String::from("value")), std::any::TypeId::of::<MyAwesomeEnum>(), Wirespec.GeneratorFieldEnum { values: vec![String::from("ONE"), String::from("Two"), String::from("THREE_MORE"), String::from("UnitedKingdom"), String::from("-1"), String::from("0"), String::from("10"), String::from("-999"), String::from("88")] }));
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
        |#[derive(Debug, Clone, Default, PartialEq)]
        |pub struct Request {
        |    pub r#type: String,
        |    pub url: String,
        |    pub body_type: Option<String>,
        |    pub params: Vec<String>,
        |    pub headers: std::collections::HashMap<String, String>,
        |    pub body: Option<std::collections::HashMap<String, Option<Vec<Option<String>>>>>,
        |}
        |impl Request {
        |    pub fn validate(&self) -> Vec<String> {
        |        return Vec::<String>::new();
        |    }
        |}
        |
        |use super::super::wirespec::*;
        |use regex;
        |pub struct RequestGenerator;
        |impl RequestGenerator {
        |    pub fn generate(path: Vec<String>, generator: Wirespec.Generator) -> Request {
        |        return Request { r#type: generator.generate((path + String::from("type")), std::any::TypeId::of::<Request>(), Wirespec.GeneratorFieldString { regex: None }), url: generator.generate((path + String::from("url")), std::any::TypeId::of::<Request>(), Wirespec.GeneratorFieldString { regex: None }), body_type: if generator.generate((path + String::from("BODY_TYPE")), std::any::TypeId::of::<Request>(), Wirespec.GeneratorFieldNullable { inner: Wirespec.GeneratorFieldString { regex: None } }) { None } else { generator.generate((path + String::from("BODY_TYPE")), std::any::TypeId::of::<Request>(), Wirespec.GeneratorFieldString { regex: None }) }, params: generator.generate((path + String::from("params")), std::any::TypeId::of::<Request>(), Wirespec.GeneratorFieldArray { inner: Wirespec.GeneratorFieldString { regex: None } }), headers: generator.generate((path + String::from("headers")), std::any::TypeId::of::<Request>(), Wirespec.GeneratorFieldDict { key: None, value: Wirespec.GeneratorFieldString { regex: None } }), body: if generator.generate((path + String::from("body")), std::any::TypeId::of::<Request>(), Wirespec.GeneratorFieldNullable { inner: Wirespec.GeneratorFieldDict { key: None, value: None } }) { None } else { generator.generate((path + String::from("body")), std::any::TypeId::of::<Request>(), Wirespec.GeneratorFieldDict { key: None, value: None }) } };
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
        |pub trait Queue: Wirespec.Channel {
        |    fn invoke(message: &str);
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
        |#[derive(Debug, Clone, Default, PartialEq)]
        |pub struct TodoId {
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
        |#[derive(Debug, Clone, Default, PartialEq)]
        |pub struct TodoNoRegex {
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
        |#[derive(Debug, Clone, Default, PartialEq)]
        |pub struct TestInt {
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
        |#[derive(Debug, Clone, Default, PartialEq)]
        |pub struct TestInt0 {
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
        |#[derive(Debug, Clone, Default, PartialEq)]
        |pub struct TestInt1 {
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
        |#[derive(Debug, Clone, Default, PartialEq)]
        |pub struct TestInt2 {
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
        |#[derive(Debug, Clone, Default, PartialEq)]
        |pub struct TestNum {
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
        |#[derive(Debug, Clone, Default, PartialEq)]
        |pub struct TestNum0 {
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
        |#[derive(Debug, Clone, Default, PartialEq)]
        |pub struct TestNum1 {
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
        |#[derive(Debug, Clone, Default, PartialEq)]
        |pub struct TestNum2 {
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
        |use super::super::wirespec::*;
        |use regex;
        |pub struct TodoIdGenerator;
        |impl TodoIdGenerator {
        |    pub fn generate(path: Vec<String>, generator: Wirespec.Generator) -> TodoId {
        |        return TodoId { value: generator.generate((path + String::from("value")), std::any::TypeId::of::<TodoId>(), Wirespec.GeneratorFieldString { regex: String::from("^[0-9a-fA-F]{8}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{12}${'$'}") }) };
        |    }
        |}
        |
        |use super::super::wirespec::*;
        |use regex;
        |pub struct TodoNoRegexGenerator;
        |impl TodoNoRegexGenerator {
        |    pub fn generate(path: Vec<String>, generator: Wirespec.Generator) -> TodoNoRegex {
        |        return TodoNoRegex { value: generator.generate((path + String::from("value")), std::any::TypeId::of::<TodoNoRegex>(), Wirespec.GeneratorFieldString { regex: None }) };
        |    }
        |}
        |
        |use super::super::wirespec::*;
        |use regex;
        |pub struct TestIntGenerator;
        |impl TestIntGenerator {
        |    pub fn generate(path: Vec<String>, generator: Wirespec.Generator) -> TestInt {
        |        return TestInt { value: generator.generate((path + String::from("value")), std::any::TypeId::of::<TestInt>(), Wirespec.GeneratorFieldInteger { min: None, max: None }) };
        |    }
        |}
        |
        |use super::super::wirespec::*;
        |use regex;
        |pub struct TestInt0Generator;
        |impl TestInt0Generator {
        |    pub fn generate(path: Vec<String>, generator: Wirespec.Generator) -> TestInt0 {
        |        return TestInt0 { value: generator.generate((path + String::from("value")), std::any::TypeId::of::<TestInt0>(), Wirespec.GeneratorFieldInteger { min: None, max: None }) };
        |    }
        |}
        |
        |use super::super::wirespec::*;
        |use regex;
        |pub struct TestInt1Generator;
        |impl TestInt1Generator {
        |    pub fn generate(path: Vec<String>, generator: Wirespec.Generator) -> TestInt1 {
        |        return TestInt1 { value: generator.generate((path + String::from("value")), std::any::TypeId::of::<TestInt1>(), Wirespec.GeneratorFieldInteger { min: 0_i32, max: None }) };
        |    }
        |}
        |
        |use super::super::wirespec::*;
        |use regex;
        |pub struct TestInt2Generator;
        |impl TestInt2Generator {
        |    pub fn generate(path: Vec<String>, generator: Wirespec.Generator) -> TestInt2 {
        |        return TestInt2 { value: generator.generate((path + String::from("value")), std::any::TypeId::of::<TestInt2>(), Wirespec.GeneratorFieldInteger { min: 1_i32, max: 3_i32 }) };
        |    }
        |}
        |
        |use super::super::wirespec::*;
        |use regex;
        |pub struct TestNumGenerator;
        |impl TestNumGenerator {
        |    pub fn generate(path: Vec<String>, generator: Wirespec.Generator) -> TestNum {
        |        return TestNum { value: generator.generate((path + String::from("value")), std::any::TypeId::of::<TestNum>(), Wirespec.GeneratorFieldNumber { min: None, max: None }) };
        |    }
        |}
        |
        |use super::super::wirespec::*;
        |use regex;
        |pub struct TestNum0Generator;
        |impl TestNum0Generator {
        |    pub fn generate(path: Vec<String>, generator: Wirespec.Generator) -> TestNum0 {
        |        return TestNum0 { value: generator.generate((path + String::from("value")), std::any::TypeId::of::<TestNum0>(), Wirespec.GeneratorFieldNumber { min: None, max: None }) };
        |    }
        |}
        |
        |use super::super::wirespec::*;
        |use regex;
        |pub struct TestNum1Generator;
        |impl TestNum1Generator {
        |    pub fn generate(path: Vec<String>, generator: Wirespec.Generator) -> TestNum1 {
        |        return TestNum1 { value: generator.generate((path + String::from("value")), std::any::TypeId::of::<TestNum1>(), Wirespec.GeneratorFieldNumber { min: None, max: 0.5_f64 }) };
        |    }
        |}
        |
        |use super::super::wirespec::*;
        |use regex;
        |pub struct TestNum2Generator;
        |impl TestNum2Generator {
        |    pub fn generate(path: Vec<String>, generator: Wirespec.Generator) -> TestNum2 {
        |        return TestNum2 { value: generator.generate((path + String::from("value")), std::any::TypeId::of::<TestNum2>(), Wirespec.GeneratorFieldNumber { min: -0.2_f64, max: 0.5_f64 }) };
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
        |#[derive(Debug, Clone, Default, PartialEq)]
        |pub struct UserAccountPassword {
        |    pub username: String,
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
        |#[derive(Debug, Clone, Default, PartialEq)]
        |pub struct UserAccountToken {
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
        |#[derive(Debug, Clone, Default, PartialEq)]
        |pub struct User {
        |    pub username: String,
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
        |#[derive(Debug, Clone, PartialEq)]
        |pub enum UserAccount {
        |    UserAccountPassword(UserAccountPassword),
        |    UserAccountToken(UserAccountToken),
        |}
        |
        |use super::super::wirespec::*;
        |use regex;
        |pub struct UserAccountPasswordGenerator;
        |impl UserAccountPasswordGenerator {
        |    pub fn generate(path: Vec<String>, generator: Wirespec.Generator) -> UserAccountPassword {
        |        return UserAccountPassword { username: generator.generate((path + String::from("username")), std::any::TypeId::of::<UserAccountPassword>(), Wirespec.GeneratorFieldString { regex: None }), password: generator.generate((path + String::from("password")), std::any::TypeId::of::<UserAccountPassword>(), Wirespec.GeneratorFieldString { regex: None }) };
        |    }
        |}
        |
        |use super::super::wirespec::*;
        |use regex;
        |pub struct UserAccountTokenGenerator;
        |impl UserAccountTokenGenerator {
        |    pub fn generate(path: Vec<String>, generator: Wirespec.Generator) -> UserAccountToken {
        |        return UserAccountToken { token: generator.generate((path + String::from("token")), std::any::TypeId::of::<UserAccountToken>(), Wirespec.GeneratorFieldString { regex: None }) };
        |    }
        |}
        |
        |use super::super::wirespec::*;
        |use regex;
        |pub struct UserGenerator;
        |impl UserGenerator {
        |    pub fn generate(path: Vec<String>, generator: Wirespec.Generator) -> User {
        |        return User { username: generator.generate((path + String::from("username")), std::any::TypeId::of::<User>(), Wirespec.GeneratorFieldString { regex: None }), account: UserAccountGenerator.generate((path + String::from("account")), generator) };
        |    }
        |}
        |
        |use super::super::wirespec::*;
        |use regex;
        |pub struct UserAccountGenerator;
        |impl UserAccountGenerator {
        |    pub fn generate(path: Vec<String>, generator: Wirespec.Generator) -> UserAccount {
        |        let variant = generator.generate((path + String::from("variant")), std::any::TypeId::of::<UserAccount>(), Wirespec.GeneratorFieldUnion { variants: vec![String::from("UserAccountPassword"), String::from("UserAccountToken")] });
        |        match variant {
        |            String::from("UserAccountPassword") => {
        |                return UserAccountPasswordGenerator.generate((path + String::from("UserAccountPassword")), generator);
        |            }
        |            String::from("UserAccountToken") => {
        |                return UserAccountTokenGenerator.generate((path + String::from("UserAccountToken")), generator);
        |            }
        |        }
        |        panic!("Unknown variant");
        |    }
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
        |#[derive(Debug, Clone, Default, PartialEq)]
        |pub struct TodoDto {
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
        |#[derive(Debug, Clone, Default, PartialEq)]
        |pub struct Path;
        |#[derive(Debug, Clone, Default, PartialEq)]
        |pub struct Queries;
        |#[derive(Debug, Clone, Default, PartialEq)]
        |pub struct RequestHeaders;
        |#[derive(Debug, Clone, Default, PartialEq)]
        |pub struct Request {
        |    pub path: Path,
        |    pub method: Method,
        |    pub queries: Queries,
        |    pub headers: RequestHeaders,
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
        |#[derive(Debug, Clone, PartialEq)]
        |pub enum Response {
        |    Response200(Response200),
        |}
        |impl From<Response200> for Response { fn from(value: Response200) -> Self { Response::Response200(value) } }
        |#[derive(Debug, Clone, PartialEq)]
        |pub enum Response2XX {
        |    Response200(Response200),
        |}
        |#[derive(Debug, Clone, PartialEq)]
        |pub enum ResponseListTodoDto {
        |    Response200(Response200),
        |}
        |#[derive(Debug, Clone, Default, PartialEq)]
        |pub struct Response200Headers;
        |#[derive(Debug, Clone, Default, PartialEq)]
        |pub struct Response200 {
        |    pub status: i32,
        |    pub headers: Response200Headers,
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
        |                return Response::Response200(Response200::new(response.body.as_ref().map(|it| serialization.deserialize_body(it, std::any::TypeId::of::<Vec<TodoDto>>())).expect("body is null")));
        |            }
        |            _ => {
        |                panic!("Cannot match response with status: {}", response.status_code);
        |            }
        |        }
        |    }
        |    pub trait Handler {
        |        async fn get_todos(&self, request: Request) -> Response;
        |    }
        |    pub trait Call {
        |        async fn get_todos(&self) -> Response;
        |    }
        |    impl<C: Client> Handler for C {
        |        async fn get_todos(&self, request: Request) -> Response {
        |            let raw = to_raw_request(self.serialization(), request);
        |            let resp = self.transport().transport(&raw).await;
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
        |use super::super::wirespec::*;
        |use super::super::endpoint::get_todos;
        |use super::super::model::todo_dto::TodoDto;
        |pub struct GetTodosClient<'a, S: Serialization, T: Transportation> {
        |    pub serialization: &'a S,
        |    pub transportation: &'a T,
        |}
        |impl<'a, S: Serialization, T: Transportation> get_todos::GetTodos::Call for GetTodosClient<'a, S, T> {
        |    async fn get_todos(&self) -> get_todos::Response {
        |        let request = get_todos::Request::new();
        |        let raw_request = get_todos::GetTodos::to_raw_request(self.serialization, request);
        |        let raw_response = self.transportation.transport(&raw_request).await;
        |        get_todos::GetTodos::from_raw_response(self.serialization, raw_response)
        |    }
        |}
        |
        |use super::super::wirespec::*;
        |use regex;
        |pub struct TodoDtoGenerator;
        |impl TodoDtoGenerator {
        |    pub fn generate(path: Vec<String>, generator: Wirespec.Generator) -> TodoDto {
        |        return TodoDto { description: generator.generate((path + String::from("description")), std::any::TypeId::of::<TodoDto>(), Wirespec.GeneratorFieldString { regex: None }) };
        |    }
        |}
        |
        |#![allow(warnings)]
        |pub mod model;
        |pub mod endpoint;
        |pub mod client;
        |pub mod wirespec;
        |
        |pub mod get_todos_client;
        |use super::wirespec::*;
        |use super::model::todo_dto::TodoDto;
        |use super::endpoint::get_todos;
        |use get_todos_client::GetTodosClient;
        |pub struct Client<S: Serialization, T: Transportation> {
        |    pub serialization: S,
        |    pub transportation: T,
        |}
        |impl<S: Serialization, T: Transportation> get_todos::GetTodos::Call for Client<S, T> {
        |    async fn get_todos(&self) -> get_todos::Response {
        |        GetTodosClient { serialization: &self.serialization, transportation: &self.transportation }
        |            .get_todos().await
        |    }
        |}
        |
        """.trimMargin()

        CompileMinimalEndpointTest.compiler { RustIrEmitter() } shouldBeRight rust
    }

    @Test
    fun compileFullEndpointTest() {
        val rust = """
        |use super::super::wirespec::*;
        |use regex;
        |#[derive(Debug, Clone, Default, PartialEq)]
        |pub struct PotentialTodoDto {
        |    pub name: String,
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
        |#[derive(Debug, Clone, Default, PartialEq)]
        |pub struct Token {
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
        |#[derive(Debug, Clone, Default, PartialEq)]
        |pub struct TodoDto {
        |    pub id: String,
        |    pub name: String,
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
        |#[derive(Debug, Clone, Default, PartialEq)]
        |pub struct Error {
        |    pub code: i64,
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
        |#[derive(Debug, Clone, Default, PartialEq)]
        |pub struct Path {
        |    pub id: String,
        |}
        |#[derive(Debug, Clone, Default, PartialEq)]
        |pub struct Queries {
        |    pub done: bool,
        |    pub name: Option<String>,
        |}
        |#[derive(Debug, Clone, Default, PartialEq)]
        |pub struct RequestHeaders {
        |    pub token: Token,
        |    pub refresh_token: Option<Token>,
        |}
        |#[derive(Debug, Clone, Default, PartialEq)]
        |pub struct Request {
        |    pub path: Path,
        |    pub method: Method,
        |    pub queries: Queries,
        |    pub headers: RequestHeaders,
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
        |#[derive(Debug, Clone, PartialEq)]
        |pub enum Response {
        |    Response200(Response200),
        |    Response201(Response201),
        |    Response500(Response500),
        |}
        |impl From<Response200> for Response { fn from(value: Response200) -> Self { Response::Response200(value) } }
        |impl From<Response201> for Response { fn from(value: Response201) -> Self { Response::Response201(value) } }
        |impl From<Response500> for Response { fn from(value: Response500) -> Self { Response::Response500(value) } }
        |#[derive(Debug, Clone, PartialEq)]
        |pub enum Response2XX {
        |    Response200(Response200),
        |    Response201(Response201),
        |}
        |#[derive(Debug, Clone, PartialEq)]
        |pub enum Response5XX {
        |    Response500(Response500),
        |}
        |#[derive(Debug, Clone, PartialEq)]
        |pub enum ResponseTodoDto {
        |    Response200(Response200),
        |    Response201(Response201),
        |}
        |#[derive(Debug, Clone, PartialEq)]
        |pub enum ResponseError {
        |    Response500(Response500),
        |}
        |#[derive(Debug, Clone, Default, PartialEq)]
        |pub struct Response200Headers;
        |#[derive(Debug, Clone, Default, PartialEq)]
        |pub struct Response200 {
        |    pub status: i32,
        |    pub headers: Response200Headers,
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
        |#[derive(Debug, Clone, Default, PartialEq)]
        |pub struct Response201Headers {
        |    pub token: Token,
        |    pub refresh_token: Option<Token>,
        |}
        |#[derive(Debug, Clone, Default, PartialEq)]
        |pub struct Response201 {
        |    pub status: i32,
        |    pub headers: Response201Headers,
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
        |#[derive(Debug, Clone, Default, PartialEq)]
        |pub struct Response500Headers;
        |#[derive(Debug, Clone, Default, PartialEq)]
        |pub struct Response500 {
        |    pub status: i32,
        |    pub headers: Response500Headers,
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
        |        return RawRequest { method: format!("{:?}", request.method), path: vec![String::from("todos"), serialization.serialize_path(&request.path.id, std::any::TypeId::of::<String>())], queries: std::collections::HashMap::from([(String::from("done"), serialization.serialize_param(&request.queries.done, std::any::TypeId::of::<bool>())), (String::from("name"), request.queries.name.as_ref().map(|it| serialization.serialize_param(it, std::any::TypeId::of::<String>())).unwrap_or(Vec::<String>::new()))]), headers: std::collections::HashMap::from([(String::from("token"), serialization.serialize_param(&request.headers.token, std::any::TypeId::of::<Token>())), (String::from("Refresh-Token"), request.headers.refresh_token.as_ref().map(|it| serialization.serialize_param(it, std::any::TypeId::of::<Token>())).unwrap_or(Vec::<String>::new()))]), body: Some(serialization.serialize_body(&request.body, std::any::TypeId::of::<PotentialTodoDto>())) };
        |    }
        |    pub fn from_raw_request(serialization: &impl Deserializer, request: RawRequest) -> Request {
        |        return Request::new(serialization.deserialize_path(&request.path[1], std::any::TypeId::of::<String>()), request.queries.get("done").as_ref().map(|it| serialization.deserialize_param(it, std::any::TypeId::of::<bool>())).expect("Param done cannot be null"), request.queries.get("name").as_ref().map(|it| serialization.deserialize_param(it, std::any::TypeId::of::<String>())), request.headers.iter().find(|(k, _)| k.eq_ignore_ascii_case("token")).map(|(_, v)| v.clone()).as_ref().map(|it| serialization.deserialize_param(it, std::any::TypeId::of::<Token>())).expect("Param token cannot be null"), request.headers.iter().find(|(k, _)| k.eq_ignore_ascii_case("Refresh-Token")).map(|(_, v)| v.clone()).as_ref().map(|it| serialization.deserialize_param(it, std::any::TypeId::of::<Token>())), request.body.as_ref().map(|it| serialization.deserialize_body(it, std::any::TypeId::of::<PotentialTodoDto>())).expect("body is null"));
        |    }
        |    pub fn to_raw_response(serialization: &impl Serializer, response: Response) -> RawResponse {
        |        match response {
        |            Response::Response200(r) => {
        |                return RawResponse { status_code: r.status, headers: std::collections::HashMap::new(), body: Some(serialization.serialize_body(&r.body, std::any::TypeId::of::<TodoDto>())) };
        |            }
        |            Response::Response201(r) => {
        |                return RawResponse { status_code: r.status, headers: std::collections::HashMap::from([(String::from("token"), serialization.serialize_param(&r.headers.token, std::any::TypeId::of::<Token>())), (String::from("refreshToken"), r.headers.refresh_token.as_ref().map(|it| serialization.serialize_param(it, std::any::TypeId::of::<Token>())).unwrap_or(Vec::<String>::new()))]), body: Some(serialization.serialize_body(&r.body, std::any::TypeId::of::<TodoDto>())) };
        |            }
        |            Response::Response500(r) => {
        |                return RawResponse { status_code: r.status, headers: std::collections::HashMap::new(), body: Some(serialization.serialize_body(&r.body, std::any::TypeId::of::<Error>())) };
        |            }
        |        }
        |    }
        |    pub fn from_raw_response(serialization: &impl Deserializer, response: RawResponse) -> Response {
        |        match response.status_code {
        |            200_i32 => {
        |                return Response::Response200(Response200::new(response.body.as_ref().map(|it| serialization.deserialize_body(it, std::any::TypeId::of::<TodoDto>())).expect("body is null")));
        |            }
        |            201_i32 => {
        |                return Response::Response201(Response201::new(response.headers.iter().find(|(k, _)| k.eq_ignore_ascii_case("token")).map(|(_, v)| v.clone()).as_ref().map(|it| serialization.deserialize_param(it, std::any::TypeId::of::<Token>())).expect("Param token cannot be null"), response.headers.iter().find(|(k, _)| k.eq_ignore_ascii_case("refreshToken")).map(|(_, v)| v.clone()).as_ref().map(|it| serialization.deserialize_param(it, std::any::TypeId::of::<Token>())), response.body.as_ref().map(|it| serialization.deserialize_body(it, std::any::TypeId::of::<TodoDto>())).expect("body is null")));
        |            }
        |            500_i32 => {
        |                return Response::Response500(Response500::new(response.body.as_ref().map(|it| serialization.deserialize_body(it, std::any::TypeId::of::<Error>())).expect("body is null")));
        |            }
        |            _ => {
        |                panic!("Cannot match response with status: {}", response.status_code);
        |            }
        |        }
        |    }
        |    pub trait Handler {
        |        async fn put_todo(&self, request: Request) -> Response;
        |    }
        |    pub trait Call {
        |        async fn put_todo(&self, id: String, done: bool, name: Option<String>, token: Token, refresh_token: Option<Token>, body: PotentialTodoDto) -> Response;
        |    }
        |    impl<C: Client> Handler for C {
        |        async fn put_todo(&self, request: Request) -> Response {
        |            let raw = to_raw_request(self.serialization(), request);
        |            let resp = self.transport().transport(&raw).await;
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
        |use super::super::wirespec::*;
        |use super::super::endpoint::put_todo;
        |use super::super::model::token::Token;
        |use super::super::model::potential_todo_dto::PotentialTodoDto;
        |use super::super::model::todo_dto::TodoDto;
        |use super::super::model::error::Error;
        |pub struct PutTodoClient<'a, S: Serialization, T: Transportation> {
        |    pub serialization: &'a S,
        |    pub transportation: &'a T,
        |}
        |impl<'a, S: Serialization, T: Transportation> put_todo::PutTodo::Call for PutTodoClient<'a, S, T> {
        |    async fn put_todo(&self, id: String, done: bool, name: Option<String>, token: Token, refresh_token: Option<Token>, body: PotentialTodoDto) -> put_todo::Response {
        |        let request = put_todo::Request::new(id, done, name, token, refresh_token, body);
        |        let raw_request = put_todo::PutTodo::to_raw_request(self.serialization, request);
        |        let raw_response = self.transportation.transport(&raw_request).await;
        |        put_todo::PutTodo::from_raw_response(self.serialization, raw_response)
        |    }
        |}
        |
        |use super::super::wirespec::*;
        |use regex;
        |pub struct PotentialTodoDtoGenerator;
        |impl PotentialTodoDtoGenerator {
        |    pub fn generate(path: Vec<String>, generator: Wirespec.Generator) -> PotentialTodoDto {
        |        return PotentialTodoDto { name: generator.generate((path + String::from("name")), std::any::TypeId::of::<PotentialTodoDto>(), Wirespec.GeneratorFieldString { regex: None }), done: generator.generate((path + String::from("done")), std::any::TypeId::of::<PotentialTodoDto>(), Wirespec.GeneratorFieldBoolean {}) };
        |    }
        |}
        |
        |use super::super::wirespec::*;
        |use regex;
        |pub struct TokenGenerator;
        |impl TokenGenerator {
        |    pub fn generate(path: Vec<String>, generator: Wirespec.Generator) -> Token {
        |        return Token { iss: generator.generate((path + String::from("iss")), std::any::TypeId::of::<Token>(), Wirespec.GeneratorFieldString { regex: None }) };
        |    }
        |}
        |
        |use super::super::wirespec::*;
        |use regex;
        |pub struct TodoDtoGenerator;
        |impl TodoDtoGenerator {
        |    pub fn generate(path: Vec<String>, generator: Wirespec.Generator) -> TodoDto {
        |        return TodoDto { id: generator.generate((path + String::from("id")), std::any::TypeId::of::<TodoDto>(), Wirespec.GeneratorFieldString { regex: None }), name: generator.generate((path + String::from("name")), std::any::TypeId::of::<TodoDto>(), Wirespec.GeneratorFieldString { regex: None }), done: generator.generate((path + String::from("done")), std::any::TypeId::of::<TodoDto>(), Wirespec.GeneratorFieldBoolean {}) };
        |    }
        |}
        |
        |use super::super::wirespec::*;
        |use regex;
        |pub struct ErrorGenerator;
        |impl ErrorGenerator {
        |    pub fn generate(path: Vec<String>, generator: Wirespec.Generator) -> Error {
        |        return Error { code: generator.generate((path + String::from("code")), std::any::TypeId::of::<Error>(), Wirespec.GeneratorFieldInteger { min: None, max: None }), description: generator.generate((path + String::from("description")), std::any::TypeId::of::<Error>(), Wirespec.GeneratorFieldString { regex: None }) };
        |    }
        |}
        |
        |#![allow(warnings)]
        |pub mod model;
        |pub mod endpoint;
        |pub mod client;
        |pub mod wirespec;
        |
        |pub mod put_todo_client;
        |use super::wirespec::*;
        |use super::model::token::Token;
        |use super::model::potential_todo_dto::PotentialTodoDto;
        |use super::model::todo_dto::TodoDto;
        |use super::model::error::Error;
        |use super::endpoint::put_todo;
        |use put_todo_client::PutTodoClient;
        |pub struct Client<S: Serialization, T: Transportation> {
        |    pub serialization: S,
        |    pub transportation: T,
        |}
        |impl<S: Serialization, T: Transportation> put_todo::PutTodo::Call for Client<S, T> {
        |    async fn put_todo(&self, id: String, done: bool, name: Option<String>, token: Token, refresh_token: Option<Token>, body: PotentialTodoDto) -> put_todo::Response {
        |        PutTodoClient { serialization: &self.serialization, transportation: &self.transportation }
        |            .put_todo(id, done, name, token, refresh_token, body).await
        |    }
        |}
        |
        """.trimMargin()

        CompileFullEndpointTest.compiler { RustIrEmitter() } shouldBeRight rust
    }

    @Test
    fun compileNestedTypeTest() {
        val rust = """
        |use super::super::wirespec::*;
        |use regex;
        |#[derive(Debug, Clone, Default, PartialEq)]
        |pub struct DutchPostalCode {
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
        |#[derive(Debug, Clone, Default, PartialEq)]
        |pub struct Address {
        |    pub street: String,
        |    pub house_number: i64,
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
        |#[derive(Debug, Clone, Default, PartialEq)]
        |pub struct Person {
        |    pub name: String,
        |    pub address: Address,
        |    pub tags: Vec<String>,
        |}
        |impl Person {
        |    pub fn validate(&self) -> Vec<String> {
        |        return self.address.validate().iter().map(|e| format!("address.{}", e)).collect::<Vec<_>>();
        |    }
        |}
        |
        |use super::super::wirespec::*;
        |use regex;
        |pub struct DutchPostalCodeGenerator;
        |impl DutchPostalCodeGenerator {
        |    pub fn generate(path: Vec<String>, generator: Wirespec.Generator) -> DutchPostalCode {
        |        return DutchPostalCode { value: generator.generate((path + String::from("value")), std::any::TypeId::of::<DutchPostalCode>(), Wirespec.GeneratorFieldString { regex: String::from("^([0-9]{4}[A-Z]{2})${'$'}") }) };
        |    }
        |}
        |
        |use super::super::wirespec::*;
        |use regex;
        |pub struct AddressGenerator;
        |impl AddressGenerator {
        |    pub fn generate(path: Vec<String>, generator: Wirespec.Generator) -> Address {
        |        return Address { street: generator.generate((path + String::from("street")), std::any::TypeId::of::<Address>(), Wirespec.GeneratorFieldString { regex: None }), house_number: generator.generate((path + String::from("houseNumber")), std::any::TypeId::of::<Address>(), Wirespec.GeneratorFieldInteger { min: None, max: None }), postal_code: DutchPostalCodeGenerator.generate((path + String::from("postalCode")), generator) };
        |    }
        |}
        |
        |use super::super::wirespec::*;
        |use regex;
        |pub struct PersonGenerator;
        |impl PersonGenerator {
        |    pub fn generate(path: Vec<String>, generator: Wirespec.Generator) -> Person {
        |        return Person { name: generator.generate((path + String::from("name")), std::any::TypeId::of::<Person>(), Wirespec.GeneratorFieldString { regex: None }), address: AddressGenerator.generate((path + String::from("address")), generator), tags: generator.generate((path + String::from("tags")), std::any::TypeId::of::<Person>(), Wirespec.GeneratorFieldArray { inner: Wirespec.GeneratorFieldString { regex: None } }) };
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
        |#[derive(Debug, Clone, Default, PartialEq)]
        |pub struct Email {
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
        |#[derive(Debug, Clone, Default, PartialEq)]
        |pub struct PhoneNumber {
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
        |#[derive(Debug, Clone, Default, PartialEq)]
        |pub struct Tag {
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
        |#[derive(Debug, Clone, Default, PartialEq)]
        |pub struct EmployeeAge {
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
        |#[derive(Debug, Clone, Default, PartialEq)]
        |pub struct ContactInfo {
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
        |#[derive(Debug, Clone, Default, PartialEq)]
        |pub struct Employee {
        |    pub name: String,
        |    pub age: EmployeeAge,
        |    pub contact_info: ContactInfo,
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
        |#[derive(Debug, Clone, Default, PartialEq)]
        |pub struct Department {
        |    pub name: String,
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
        |#[derive(Debug, Clone, Default, PartialEq)]
        |pub struct Company {
        |    pub name: String,
        |    pub departments: Vec<Department>,
        |}
        |impl Company {
        |    pub fn validate(&self) -> Vec<String> {
        |        return self.departments.iter().enumerate().flat_map(|(i, el)| el.validate().iter().map(|e| format!("departments[{}].{}", i, e)).collect::<Vec<_>>()).collect::<Vec<_>>();
        |    }
        |}
        |
        |use super::super::wirespec::*;
        |use regex;
        |pub struct EmailGenerator;
        |impl EmailGenerator {
        |    pub fn generate(path: Vec<String>, generator: Wirespec.Generator) -> Email {
        |        return Email { value: generator.generate((path + String::from("value")), std::any::TypeId::of::<Email>(), Wirespec.GeneratorFieldString { regex: String::from("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}${'$'}") }) };
        |    }
        |}
        |
        |use super::super::wirespec::*;
        |use regex;
        |pub struct PhoneNumberGenerator;
        |impl PhoneNumberGenerator {
        |    pub fn generate(path: Vec<String>, generator: Wirespec.Generator) -> PhoneNumber {
        |        return PhoneNumber { value: generator.generate((path + String::from("value")), std::any::TypeId::of::<PhoneNumber>(), Wirespec.GeneratorFieldString { regex: String::from("^\+[1-9]\d{1,14}${'$'}") }) };
        |    }
        |}
        |
        |use super::super::wirespec::*;
        |use regex;
        |pub struct TagGenerator;
        |impl TagGenerator {
        |    pub fn generate(path: Vec<String>, generator: Wirespec.Generator) -> Tag {
        |        return Tag { value: generator.generate((path + String::from("value")), std::any::TypeId::of::<Tag>(), Wirespec.GeneratorFieldString { regex: String::from("^[a-z][a-z0-9-]{0,19}${'$'}") }) };
        |    }
        |}
        |
        |use super::super::wirespec::*;
        |use regex;
        |pub struct EmployeeAgeGenerator;
        |impl EmployeeAgeGenerator {
        |    pub fn generate(path: Vec<String>, generator: Wirespec.Generator) -> EmployeeAge {
        |        return EmployeeAge { value: generator.generate((path + String::from("value")), std::any::TypeId::of::<EmployeeAge>(), Wirespec.GeneratorFieldInteger { min: 18_i32, max: 65_i32 }) };
        |    }
        |}
        |
        |use super::super::wirespec::*;
        |use regex;
        |pub struct ContactInfoGenerator;
        |impl ContactInfoGenerator {
        |    pub fn generate(path: Vec<String>, generator: Wirespec.Generator) -> ContactInfo {
        |        return ContactInfo { email: EmailGenerator.generate((path + String::from("email")), generator), phone: if generator.generate((path + String::from("phone")), std::any::TypeId::of::<ContactInfo>(), Wirespec.GeneratorFieldNullable { inner: None }) { None } else { PhoneNumberGenerator.generate((path + String::from("phone")), generator) } };
        |    }
        |}
        |
        |use super::super::wirespec::*;
        |use regex;
        |pub struct EmployeeGenerator;
        |impl EmployeeGenerator {
        |    pub fn generate(path: Vec<String>, generator: Wirespec.Generator) -> Employee {
        |        return Employee { name: generator.generate((path + String::from("name")), std::any::TypeId::of::<Employee>(), Wirespec.GeneratorFieldString { regex: None }), age: EmployeeAgeGenerator.generate((path + String::from("age")), generator), contact_info: ContactInfoGenerator.generate((path + String::from("contactInfo")), generator), tags: generator.generate((path + String::from("tags")), std::any::TypeId::of::<Employee>(), Wirespec.GeneratorFieldArray { inner: None }) };
        |    }
        |}
        |
        |use super::super::wirespec::*;
        |use regex;
        |pub struct DepartmentGenerator;
        |impl DepartmentGenerator {
        |    pub fn generate(path: Vec<String>, generator: Wirespec.Generator) -> Department {
        |        return Department { name: generator.generate((path + String::from("name")), std::any::TypeId::of::<Department>(), Wirespec.GeneratorFieldString { regex: None }), employees: generator.generate((path + String::from("employees")), std::any::TypeId::of::<Department>(), Wirespec.GeneratorFieldArray { inner: None }) };
        |    }
        |}
        |
        |use super::super::wirespec::*;
        |use regex;
        |pub struct CompanyGenerator;
        |impl CompanyGenerator {
        |    pub fn generate(path: Vec<String>, generator: Wirespec.Generator) -> Company {
        |        return Company { name: generator.generate((path + String::from("name")), std::any::TypeId::of::<Company>(), Wirespec.GeneratorFieldString { regex: None }), departments: generator.generate((path + String::from("departments")), std::any::TypeId::of::<Company>(), Wirespec.GeneratorFieldArray { inner: None }) };
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
            |pub trait Call {}
            |
            |#[derive(Debug, Clone, Default, PartialEq)]
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
            |    fn serialize_body<T: 'static>(&self, t: &T, r#type: TypeId) -> Vec<u8>;
            |}
            |
            |pub trait BodyDeserializer {
            |    fn deserialize_body<T: 'static>(&self, raw: &[u8], r#type: TypeId) -> T;
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
            |    fn serialize_param<T: 'static>(&self, value: &T, r#type: TypeId) -> Vec<String>;
            |}
            |
            |pub trait ParamDeserializer {
            |    fn deserialize_param<T: 'static>(&self, values: &[String], r#type: TypeId) -> T;
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
            |    pub queries: std::collections::HashMap<String, Vec<String>>,
            |    pub headers: std::collections::HashMap<String, Vec<String>>,
            |    pub body: Option<Vec<u8>>,
            |}
            |
            |#[derive(Debug, Clone, PartialEq)]
            |pub struct RawResponse {
            |    pub status_code: i32,
            |    pub headers: std::collections::HashMap<String, Vec<String>>,
            |    pub body: Option<Vec<u8>>,
            |}
            |
            |pub trait Transportation {
            |    async fn transport(&self, request: &RawRequest) -> RawResponse;
            |}
            |
            |pub trait GeneratorField<T> {}
            |
            |#[derive(Debug, Clone, Default, PartialEq)]
            |pub struct GeneratorFieldString {
            |    pub regex: Option<String>,
            |}
            |
            |#[derive(Debug, Clone, Default, PartialEq)]
            |pub struct GeneratorFieldInteger {
            |    pub min: Option<i64>,
            |    pub max: Option<i64>,
            |}
            |
            |#[derive(Debug, Clone, Default, PartialEq)]
            |pub struct GeneratorFieldNumber {
            |    pub min: Option<f64>,
            |    pub max: Option<f64>,
            |}
            |
            |#[derive(Debug, Clone, Default, PartialEq)]
            |pub struct GeneratorFieldBoolean;
            |
            |#[derive(Debug, Clone, Default, PartialEq)]
            |pub struct GeneratorFieldBytes;
            |
            |#[derive(Debug, Clone, Default, PartialEq)]
            |pub struct GeneratorFieldEnum {
            |    pub values: Vec<String>,
            |}
            |
            |#[derive(Debug, Clone, Default, PartialEq)]
            |pub struct GeneratorFieldUnion {
            |    pub variants: Vec<String>,
            |}
            |
            |#[derive(Debug, Default)]
            |pub struct GeneratorFieldArray {
            |    pub inner: Option<Box<dyn std::any::Any>>,
            |}
            |
            |#[derive(Debug, Default)]
            |pub struct GeneratorFieldNullable {
            |    pub inner: Option<Box<dyn std::any::Any>>,
            |}
            |
            |#[derive(Debug, Default)]
            |pub struct GeneratorFieldDict {
            |    pub key: Option<Box<dyn std::any::Any>>,
            |    pub value: Option<Box<dyn std::any::Any>>,
            |}
            |
            |pub trait Generator {
            |    fn generate<T>(&self, path: Vec<String>, r#type: std::any::TypeId, field: impl GeneratorField<T>) -> T;
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
        """.trimMargin()

        val emitter = RustIrEmitter()
        emitter.shared.source shouldBe expected
    }

    private fun emitGeneratorSource(node: Definition, fileNameSubstring: String): String {
        val emitter = RustIrEmitter()
        val ast = AST(
            nonEmptyListOf(
                Module(
                    FileUri(""),
                    nonEmptyListOf(node),
                ),
            ),
        )
        val emitted = emitter.emit(ast, noLogger)
        val match = emitted.toList().first { it.file.contains(fileNameSubstring) }
        return match.result
    }

    @Test
    fun testEmitGeneratorForType() {
        val address = Type(
            comment = null,
            annotations = emptyList(),
            identifier = DefinitionIdentifier("Address"),
            shape = Type.Shape(
                value = listOf(
                    Field(
                        identifier = FieldIdentifier("street"),
                        annotations = emptyList(),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String(null),
                            isNullable = false,
                        ),
                    ),
                    Field(
                        identifier = FieldIdentifier("number"),
                        annotations = emptyList(),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.Integer(constraint = null),
                            isNullable = false,
                        ),
                    ),
                ),
            ),
            extends = emptyList(),
        )

        val expected = """
            |use super::super::wirespec::*;
            |use regex;
            |pub struct AddressGenerator;
            |impl AddressGenerator {
            |    pub fn generate(path: Vec<String>, generator: Wirespec.Generator) -> Address {
            |        return Address { street: generator.generate((path + String::from("street")), std::any::TypeId::of::<Address>(), Wirespec.GeneratorFieldString { regex: None }), number: generator.generate((path + String::from("number")), std::any::TypeId::of::<Address>(), Wirespec.GeneratorFieldInteger { min: None, max: None }) };
            |    }
            |}
            |
        """.trimMargin()

        emitGeneratorSource(address, "address_generator") shouldBe expected
    }

    @Test
    fun testEmitGeneratorForEnum() {
        val color = Enum(
            comment = null,
            annotations = emptyList(),
            identifier = DefinitionIdentifier("Color"),
            entries = setOf("RED", "GREEN", "BLUE"),
        )

        val expected = """
            |use super::super::wirespec::*;
            |use regex;
            |pub struct ColorGenerator;
            |impl ColorGenerator {
            |    pub fn generate(path: Vec<String>, generator: Wirespec.Generator) -> Color {
            |        return Color.value_of(generator.generate((path + String::from("value")), std::any::TypeId::of::<Color>(), Wirespec.GeneratorFieldEnum { values: vec![String::from("RED"), String::from("GREEN"), String::from("BLUE")] }));
            |    }
            |}
            |
        """.trimMargin()

        emitGeneratorSource(color, "color_generator") shouldBe expected
    }

    @Test
    fun testEmitGeneratorForUnion() {
        val shape = Union(
            comment = null,
            annotations = emptyList(),
            identifier = DefinitionIdentifier("Shape"),
            entries = setOf(
                Reference.Custom(value = "Circle", isNullable = false),
                Reference.Custom(value = "Square", isNullable = false),
            ),
        )

        val expected = """
            |use super::super::wirespec::*;
            |use regex;
            |pub struct ShapeGenerator;
            |impl ShapeGenerator {
            |    pub fn generate(path: Vec<String>, generator: Wirespec.Generator) -> Shape {
            |        let variant = generator.generate((path + String::from("variant")), std::any::TypeId::of::<Shape>(), Wirespec.GeneratorFieldUnion { variants: vec![String::from("Circle"), String::from("Square")] });
            |        match variant {
            |            String::from("Circle") => {
            |                return CircleGenerator.generate((path + String::from("Circle")), generator);
            |            }
            |            String::from("Square") => {
            |                return SquareGenerator.generate((path + String::from("Square")), generator);
            |            }
            |        }
            |        panic!("Unknown variant");
            |    }
            |}
            |
        """.trimMargin()

        emitGeneratorSource(shape, "shape_generator") shouldBe expected
    }

    @Test
    fun testEmitGeneratorForRefined() {
        val uuid = Refined(
            comment = null,
            annotations = emptyList(),
            identifier = DefinitionIdentifier("UUID"),
            reference = Reference.Primitive(
                type = Reference.Primitive.Type.String(
                    Reference.Primitive.Type.Constraint.RegExp("/^[0-9a-f]{8}${'$'}/g"),
                ),
                isNullable = false,
            ),
        )

        val expected = """
            |use super::super::wirespec::*;
            |use regex;
            |pub struct UUIDGenerator;
            |impl UUIDGenerator {
            |    pub fn generate(path: Vec<String>, generator: Wirespec.Generator) -> UUID {
            |        return UUID { value: generator.generate((path + String::from("value")), std::any::TypeId::of::<UUID>(), Wirespec.GeneratorFieldString { regex: String::from("^[0-9a-f]{8}${'$'}") }) };
            |    }
            |}
            |
        """.trimMargin()

        emitGeneratorSource(uuid, "uuid_generator") shouldBe expected
    }

    @Test
    fun testEmitGeneratorForArrayField() {
        val inventory = Type(
            comment = null,
            annotations = emptyList(),
            identifier = DefinitionIdentifier("Inventory"),
            shape = Type.Shape(
                value = listOf(
                    Field(
                        identifier = FieldIdentifier("items"),
                        annotations = emptyList(),
                        reference = Reference.Iterable(
                            reference = Reference.Primitive(
                                type = Reference.Primitive.Type.Integer(constraint = null),
                                isNullable = false,
                            ),
                            isNullable = false,
                        ),
                    ),
                ),
            ),
            extends = emptyList(),
        )

        val expected = """
            |use super::super::wirespec::*;
            |use regex;
            |pub struct InventoryGenerator;
            |impl InventoryGenerator {
            |    pub fn generate(path: Vec<String>, generator: Wirespec.Generator) -> Inventory {
            |        return Inventory { items: generator.generate((path + String::from("items")), std::any::TypeId::of::<Inventory>(), Wirespec.GeneratorFieldArray { inner: Wirespec.GeneratorFieldInteger { min: None, max: None } }) };
            |    }
            |}
            |
        """.trimMargin()

        emitGeneratorSource(inventory, "inventory_generator") shouldBe expected
    }

    @Test
    fun testEmitGeneratorForDictField() {
        val lookup = Type(
            comment = null,
            annotations = emptyList(),
            identifier = DefinitionIdentifier("Lookup"),
            shape = Type.Shape(
                value = listOf(
                    Field(
                        identifier = FieldIdentifier("entries"),
                        annotations = emptyList(),
                        reference = Reference.Dict(
                            reference = Reference.Primitive(
                                type = Reference.Primitive.Type.Integer(constraint = null),
                                isNullable = false,
                            ),
                            isNullable = false,
                        ),
                    ),
                ),
            ),
            extends = emptyList(),
        )

        val expected = """
            |use super::super::wirespec::*;
            |use regex;
            |pub struct LookupGenerator;
            |impl LookupGenerator {
            |    pub fn generate(path: Vec<String>, generator: Wirespec.Generator) -> Lookup {
            |        return Lookup { entries: generator.generate((path + String::from("entries")), std::any::TypeId::of::<Lookup>(), Wirespec.GeneratorFieldDict { key: None, value: Wirespec.GeneratorFieldInteger { min: None, max: None } }) };
            |    }
            |}
            |
        """.trimMargin()

        emitGeneratorSource(lookup, "lookup_generator") shouldBe expected
    }

    @Test
    fun testEmitGeneratorForNullableField() {
        val person = Type(
            comment = null,
            annotations = emptyList(),
            identifier = DefinitionIdentifier("Person"),
            shape = Type.Shape(
                value = listOf(
                    Field(
                        identifier = FieldIdentifier("nickname"),
                        annotations = emptyList(),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String(null),
                            isNullable = true,
                        ),
                    ),
                ),
            ),
            extends = emptyList(),
        )

        val expected = """
            |use super::super::wirespec::*;
            |use regex;
            |pub struct PersonGenerator;
            |impl PersonGenerator {
            |    pub fn generate(path: Vec<String>, generator: Wirespec.Generator) -> Person {
            |        return Person { nickname: if generator.generate((path + String::from("nickname")), std::any::TypeId::of::<Person>(), Wirespec.GeneratorFieldNullable { inner: Wirespec.GeneratorFieldString { regex: None } }) { None } else { generator.generate((path + String::from("nickname")), std::any::TypeId::of::<Person>(), Wirespec.GeneratorFieldString { regex: None }) } };
            |    }
            |}
            |
        """.trimMargin()

        emitGeneratorSource(person, "person_generator") shouldBe expected
    }
}
