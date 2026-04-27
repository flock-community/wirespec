package community.flock.wirespec.emitters.typescript

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

class TypeScriptIrEmitterTest {

    @Test
    fun compileFullEndpointTest() {
        val typescript = """
            |import {Wirespec} from '../Wirespec'
            |import {type Token} from '../model'
            |import {type PotentialTodoDto} from '../model'
            |import {type TodoDto} from '../model'
            |import {type Error} from '../model'
            |export namespace PutTodo {
            |  export type Path = {
            |    "id": string,
            |  }
            |  export type Queries = {
            |    "done": boolean,
            |    "name": string | undefined,
            |  }
            |  export type RequestHeaders = {
            |    "token": Token,
            |    "refreshToken": Token | undefined,
            |  }
            |  export type Request = {
            |    "path": Path,
            |    "method": Wirespec.Method,
            |    "queries": Queries,
            |    "headers": RequestHeaders,
            |    "body": PotentialTodoDto,
            |  }
            |  export type RequestParams = {"id": string, "done": boolean, "name"?: string, "token": Token, "refreshToken"?: Token, "body": PotentialTodoDto}
            |  export const request = (params: RequestParams): Request => ({
            |    path: {"id": params["id"]},
            |    method: "PUT",
            |    queries: {"done": params["done"], "name": params["name"]},
            |    headers: {"token": params["token"], "refreshToken": params["refreshToken"]},
            |    body: params.body,
            |  })
            |  export type Response<T = unknown> = Response2XX | Response5XX | ResponseTodoDto | ResponseError
            |  export type Response2XX<T = unknown> = Response200 | Response201
            |  export type Response5XX<T = unknown> = Response500
            |  export type ResponseTodoDto = Response200 | Response201
            |  export type ResponseError = Response500
            |  export type Response200 = {
            |    "status": 200,
            |    "headers": {},
            |    "body": TodoDto,
            |  }
            |  export type Response200Params = {"body": TodoDto}
            |  export const response200 = (params: Response200Params): Response200 => ({
            |    status: 200,
            |    headers: {},
            |    body: params.body,
            |  })
            |  export type Response201 = {
            |    "status": 201,
            |    "headers": {"token": Token, "refreshToken": Token | undefined},
            |    "body": TodoDto,
            |  }
            |  export type Response201Params = {"token": Token, "refreshToken"?: Token, "body": TodoDto}
            |  export const response201 = (params: Response201Params): Response201 => ({
            |    status: 201,
            |    headers: {"token": params["token"], "refreshToken": params["refreshToken"]},
            |    body: params.body,
            |  })
            |  export type Response500 = {
            |    "status": 500,
            |    "headers": {},
            |    "body": Error,
            |  }
            |  export type Response500Params = {"body": Error}
            |  export const response500 = (params: Response500Params): Response500 => ({
            |    status: 500,
            |    headers: {},
            |    body: params.body,
            |  })
            |  export function toRawRequest(serialization: Wirespec.Serializer, _request: Request): Wirespec.RawRequest {
            |    return { method: _request.method, path: ['todos', serialization.serializePath(_request.path.id, "string")], queries: { 'done': serialization.serializeParam(_request.queries.done, "boolean"), 'name': _request.queries.name != null ? serialization.serializeParam(_request.queries.name, "string") : [] as string[] }, headers: { 'token': serialization.serializeParam(_request.headers.token, "Token"), 'Refresh-Token': _request.headers.refreshToken != null ? serialization.serializeParam(_request.headers.refreshToken, "Token") : [] as string[] }, body: serialization.serializeBody(_request.body, "PotentialTodoDto") };
            |  }
            |  export function fromRawRequest(serialization: Wirespec.Deserializer, _request: Wirespec.RawRequest): Request {
            |    return request({"id": serialization.deserializePath(_request.path[1], "string"), "done": _request.queries['done'] != null ? serialization.deserializeParam(_request.queries['done'], "boolean") : (() => { throw new Error('Param done cannot be null') })(), "name": _request.queries['name'] != null ? serialization.deserializeParam(_request.queries['name'], "string") : undefined, "token": Object.entries(_request.headers).find(([k]) => k.toLowerCase() === 'token'.toLowerCase())?.[1] != null ? serialization.deserializeParam(Object.entries(_request.headers).find(([k]) => k.toLowerCase() === 'token'.toLowerCase())?.[1]!, "Token") : (() => { throw new Error('Param token cannot be null') })(), "refreshToken": Object.entries(_request.headers).find(([k]) => k.toLowerCase() === 'Refresh-Token'.toLowerCase())?.[1] != null ? serialization.deserializeParam(Object.entries(_request.headers).find(([k]) => k.toLowerCase() === 'Refresh-Token'.toLowerCase())?.[1]!, "Token") : undefined, "body": _request.body != null ? serialization.deserializeBody(_request.body, "PotentialTodoDto") : (() => { throw new Error('body is null') })()});
            |  }
            |  export function toRawResponse(serialization: Wirespec.Serializer, response: Response<unknown>): Wirespec.RawResponse {
            |    switch (response.status) {
            |      case 200: {
            |        const r = response as Response200;
            |        return { statusCode: r.status, headers: {}, body: serialization.serializeBody(r.body, "TodoDto") };
            |      }
            |      case 201: {
            |        const r = response as Response201;
            |        return { statusCode: r.status, headers: { 'token': serialization.serializeParam(r.headers.token, "Token"), 'refreshToken': r.headers.refreshToken != null ? serialization.serializeParam(r.headers.refreshToken, "Token") : [] as string[] }, body: serialization.serializeBody(r.body, "TodoDto") };
            |      }
            |      case 500: {
            |        const r = response as Response500;
            |        return { statusCode: r.status, headers: {}, body: serialization.serializeBody(r.body, "Error") };
            |      }
            |      default: {
            |        throw new Error('Cannot match response with status:');
            |      }
            |    }
            |  }
            |  export function fromRawResponse(serialization: Wirespec.Deserializer, response: Wirespec.RawResponse): Response<unknown> {
            |    switch (response.statusCode) {
            |      case 200:
            |        return response200({"body": response.body != null ? serialization.deserializeBody(response.body, "TodoDto") : (() => { throw new Error('body is null') })()});
            |        break;
            |      case 201:
            |        return response201({"token": Object.entries(response.headers).find(([k]) => k.toLowerCase() === 'token'.toLowerCase())?.[1] != null ? serialization.deserializeParam(Object.entries(response.headers).find(([k]) => k.toLowerCase() === 'token'.toLowerCase())?.[1]!, "Token") : (() => { throw new Error('Param token cannot be null') })(), "refreshToken": Object.entries(response.headers).find(([k]) => k.toLowerCase() === 'refreshToken'.toLowerCase())?.[1] != null ? serialization.deserializeParam(Object.entries(response.headers).find(([k]) => k.toLowerCase() === 'refreshToken'.toLowerCase())?.[1]!, "Token") : undefined, "body": response.body != null ? serialization.deserializeBody(response.body, "TodoDto") : (() => { throw new Error('body is null') })()});
            |        break;
            |      case 500:
            |        return response500({"body": response.body != null ? serialization.deserializeBody(response.body, "Error") : (() => { throw new Error('body is null') })()});
            |        break;
            |      default:
            |        throw new Error('Cannot match response with status:');
            |    }
            |  }
            |  export interface Handler extends Wirespec.Handler {
            |    putTodo(_request: Request): Promise<Response<unknown>>;
            |  }
            |  export interface Call extends Wirespec.Call {
            |    putTodo(params: RequestParams): Promise<Response<unknown>>;
            |  }
            |  export const client:Wirespec.Client<Request, Response> = (serialization: Wirespec.Serialization) => ({
            |    from: (it) => fromRawResponse(serialization, it),
            |    to: (it) => toRawRequest(serialization, it)
            |  })
            |  export const server:Wirespec.Server<Request, Response> = (serialization: Wirespec.Serialization) => ({
            |    from: (it) => fromRawRequest(serialization, it),
            |    to: (it) => toRawResponse(serialization, it)
            |  })
            |  export const api = {
            |    name: "putTodo",
            |    method: "PUT",
            |    path: "todos/{id}",
            |    server,
            |    client
            |  } as const
            |}
            |
            |import {Wirespec} from '../Wirespec'
            |export type PotentialTodoDto = {
            |  "name": string,
            |  "done": boolean,
            |}
            |export function validatePotentialTodoDto(obj: PotentialTodoDto): string[] {
            |  return [] as string[];
            |}
            |
            |import {Wirespec} from '../Wirespec'
            |export type Token = {
            |  "iss": string,
            |}
            |export function validateToken(obj: Token): string[] {
            |  return [] as string[];
            |}
            |
            |import {Wirespec} from '../Wirespec'
            |export type TodoDto = {
            |  "id": string,
            |  "name": string,
            |  "done": boolean,
            |}
            |export function validateTodoDto(obj: TodoDto): string[] {
            |  return [] as string[];
            |}
            |
            |import {Wirespec} from '../Wirespec'
            |export type Error = {
            |  "code": number,
            |  "description": string,
            |}
            |export function validateError(obj: Error): string[] {
            |  return [] as string[];
            |}
            |
            |import {Wirespec} from '../Wirespec'
            |import {PutTodo} from '../endpoint/PutTodo'
            |import {type Token} from '../model'
            |import {type PotentialTodoDto} from '../model'
            |import {type TodoDto} from '../model'
            |import {type Error} from '../model'
            |export const putTodoClient = (serialization: Wirespec.Serialization, transportation: Wirespec.Transportation) => ({
            |  putTodo: async (params: PutTodo.RequestParams): Promise<PutTodo.Response<unknown>> => {
            |    const request: PutTodo.Request = PutTodo.request(params);
            |    const rawRequest = PutTodo.toRawRequest(serialization, request);
            |    const rawResponse = await transportation.transport(rawRequest);
            |    return PutTodo.fromRawResponse(serialization, rawResponse);
            |  }
            |})
            |
            |import {Wirespec} from '../Wirespec'
            |export namespace PotentialTodoDtoGenerator {
            |  export function generate(path: string[], generator: Wirespec.Generator): PotentialTodoDto {
            |    return { name: generator.generate((path + 'name'), "PotentialTodoDto", { regex: undefined }), done: generator.generate((path + 'done'), "PotentialTodoDto", {}) };
            |  }
            |}
            |
            |import {Wirespec} from '../Wirespec'
            |export namespace TokenGenerator {
            |  export function generate(path: string[], generator: Wirespec.Generator): Token {
            |    return { iss: generator.generate((path + 'iss'), "Token", { regex: undefined }) };
            |  }
            |}
            |
            |import {Wirespec} from '../Wirespec'
            |export namespace TodoDtoGenerator {
            |  export function generate(path: string[], generator: Wirespec.Generator): TodoDto {
            |    return { id: generator.generate((path + 'id'), "TodoDto", { regex: undefined }), name: generator.generate((path + 'name'), "TodoDto", { regex: undefined }), done: generator.generate((path + 'done'), "TodoDto", {}) };
            |  }
            |}
            |
            |import {Wirespec} from '../Wirespec'
            |export namespace ErrorGenerator {
            |  export function generate(path: string[], generator: Wirespec.Generator): Error {
            |    return { code: generator.generate((path + 'code'), "Error", { min: undefined, max: undefined }), description: generator.generate((path + 'description'), "Error", { regex: undefined }) };
            |  }
            |}
            |
            |import {Wirespec} from './Wirespec'
            |import {putTodoClient} from './client/PutTodoClient'
            |export const client = (serialization: Wirespec.Serialization, transportation: Wirespec.Transportation) => ({
            |  ...putTodoClient(serialization, transportation),
            |})
            |
            |export {PutTodo} from './PutTodo'
            |export {PotentialTodoDto} from './PotentialTodoDto'
            |export {Token} from './Token'
            |export {TodoDto} from './TodoDto'
            |export {Error} from './Error'
        """.trimMargin()

        CompileFullEndpointTest.compiler { TypeScriptIrEmitter() } shouldBeRight typescript
    }

    @Test
    fun compileChannelTest() {
        val typescript = """
            |import {Wirespec} from '../Wirespec'
            |export interface Queue extends Wirespec.Channel {
            |  invoke(message: string): void;
            |}
            |
            |export {Queue} from './Queue'
        """.trimMargin()

        CompileChannelTest.compiler { TypeScriptIrEmitter() } shouldBeRight typescript
    }

    @Test
    fun compileEnumTest() {
        val typescript = """
            |import {Wirespec} from '../Wirespec'
            |export type MyAwesomeEnum = "ONE" | "Two" | "THREE_MORE" | "UnitedKingdom" | "-1" | "0" | "10" | "-999" | "88"
            |
            |import {Wirespec} from '../Wirespec'
            |export namespace MyAwesomeEnumGenerator {
            |  export function generate(path: string[], generator: Wirespec.Generator): MyAwesomeEnum {
            |    return MyAwesomeEnum.valueOf(generator.generate((path + 'value'), "MyAwesomeEnum", { values: ['ONE', 'Two', 'THREE_MORE', 'UnitedKingdom', '-1', '0', '10', '-999', '88'] }));
            |  }
            |}
            |
            |export {MyAwesomeEnum} from './MyAwesomeEnum'
        """.trimMargin()

        CompileEnumTest.compiler { TypeScriptIrEmitter() } shouldBeRight typescript
    }

    @Test
    fun compileMinimalEndpointTest() {
        val typescript = """
            |import {Wirespec} from '../Wirespec'
            |import {type TodoDto} from '../model'
            |export namespace GetTodos {
            |  export type Path = {}
            |  export type Queries = {}
            |  export type RequestHeaders = {}
            |  export type Request = {
            |    "path": Path,
            |    "method": Wirespec.Method,
            |    "queries": Queries,
            |    "headers": RequestHeaders,
            |    "body": void,
            |  }
            |  export type RequestParams = {}
            |  export const request = (): Request => ({
            |    path: {},
            |    method: "GET",
            |    queries: {},
            |    headers: {},
            |    body: undefined,
            |  })
            |  export type Response<T = unknown> = Response2XX | ResponseListTodoDto
            |  export type Response2XX<T = unknown> = Response200
            |  export type ResponseListTodoDto = Response200
            |  export type Response200 = {
            |    "status": 200,
            |    "headers": {},
            |    "body": TodoDto[],
            |  }
            |  export type Response200Params = {"body": TodoDto[]}
            |  export const response200 = (params: Response200Params): Response200 => ({
            |    status: 200,
            |    headers: {},
            |    body: params.body,
            |  })
            |  export function toRawRequest(serialization: Wirespec.Serializer, _request: Request): Wirespec.RawRequest {
            |    return { method: _request.method, path: ['todos'], queries: {}, headers: {}, body: undefined };
            |  }
            |  export function fromRawRequest(serialization: Wirespec.Deserializer, _request: Wirespec.RawRequest): Request {
            |    return request();
            |  }
            |  export function toRawResponse(serialization: Wirespec.Serializer, response: Response<unknown>): Wirespec.RawResponse {
            |    switch (response.status) {
            |      case 200: {
            |        const r = response as Response200;
            |        return { statusCode: r.status, headers: {}, body: serialization.serializeBody(r.body, "TodoDto[]") };
            |      }
            |      default: {
            |        throw new Error('Cannot match response with status:');
            |      }
            |    }
            |  }
            |  export function fromRawResponse(serialization: Wirespec.Deserializer, response: Wirespec.RawResponse): Response<unknown> {
            |    switch (response.statusCode) {
            |      case 200:
            |        return response200({"body": response.body != null ? serialization.deserializeBody(response.body, "TodoDto[]") : (() => { throw new Error('body is null') })()});
            |        break;
            |      default:
            |        throw new Error('Cannot match response with status:');
            |    }
            |  }
            |  export interface Handler extends Wirespec.Handler {
            |    getTodos(_request: Request): Promise<Response<unknown>>;
            |  }
            |  export interface Call extends Wirespec.Call {
            |    getTodos(): Promise<Response<unknown>>;
            |  }
            |  export const client:Wirespec.Client<Request, Response> = (serialization: Wirespec.Serialization) => ({
            |    from: (it) => fromRawResponse(serialization, it),
            |    to: (it) => toRawRequest(serialization, it)
            |  })
            |  export const server:Wirespec.Server<Request, Response> = (serialization: Wirespec.Serialization) => ({
            |    from: (it) => fromRawRequest(serialization, it),
            |    to: (it) => toRawResponse(serialization, it)
            |  })
            |  export const api = {
            |    name: "getTodos",
            |    method: "GET",
            |    path: "todos",
            |    server,
            |    client
            |  } as const
            |}
            |
            |import {Wirespec} from '../Wirespec'
            |export type TodoDto = {
            |  "description": string,
            |}
            |export function validateTodoDto(obj: TodoDto): string[] {
            |  return [] as string[];
            |}
            |
            |import {Wirespec} from '../Wirespec'
            |import {GetTodos} from '../endpoint/GetTodos'
            |import {type TodoDto} from '../model'
            |export const getTodosClient = (serialization: Wirespec.Serialization, transportation: Wirespec.Transportation) => ({
            |  getTodos: async (): Promise<GetTodos.Response<unknown>> => {
            |    const request: GetTodos.Request = GetTodos.request();
            |    const rawRequest = GetTodos.toRawRequest(serialization, request);
            |    const rawResponse = await transportation.transport(rawRequest);
            |    return GetTodos.fromRawResponse(serialization, rawResponse);
            |  }
            |})
            |
            |import {Wirespec} from '../Wirespec'
            |export namespace TodoDtoGenerator {
            |  export function generate(path: string[], generator: Wirespec.Generator): TodoDto {
            |    return { description: generator.generate((path + 'description'), "TodoDto", { regex: undefined }) };
            |  }
            |}
            |
            |import {Wirespec} from './Wirespec'
            |import {getTodosClient} from './client/GetTodosClient'
            |export const client = (serialization: Wirespec.Serialization, transportation: Wirespec.Transportation) => ({
            |  ...getTodosClient(serialization, transportation),
            |})
            |
            |export {GetTodos} from './GetTodos'
            |export {TodoDto} from './TodoDto'
        """.trimMargin()

        CompileMinimalEndpointTest.compiler { TypeScriptIrEmitter() } shouldBeRight typescript
    }

    @Test
    fun compileRefinedTest() {
        val typescript = """
            |import {Wirespec} from '../Wirespec'
            |export type TodoId = string;
            |export const validateTodoId = (value: string) =>
            |  /^[0-9a-fA-F]{8}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{12}${'$'}/g.test(value);
            |
            |import {Wirespec} from '../Wirespec'
            |export type TodoNoRegex = string;
            |export const validateTodoNoRegex = (value: string) =>
            |  true;
            |
            |import {Wirespec} from '../Wirespec'
            |export type TestInt = number;
            |export const validateTestInt = (value: number) =>
            |  true;
            |
            |import {Wirespec} from '../Wirespec'
            |export type TestInt0 = number;
            |export const validateTestInt0 = (value: number) =>
            |  true;
            |
            |import {Wirespec} from '../Wirespec'
            |export type TestInt1 = number;
            |export const validateTestInt1 = (value: number) =>
            |  0 <= value;
            |
            |import {Wirespec} from '../Wirespec'
            |export type TestInt2 = number;
            |export const validateTestInt2 = (value: number) =>
            |  1 <= value && value <= 3;
            |
            |import {Wirespec} from '../Wirespec'
            |export type TestNum = number;
            |export const validateTestNum = (value: number) =>
            |  true;
            |
            |import {Wirespec} from '../Wirespec'
            |export type TestNum0 = number;
            |export const validateTestNum0 = (value: number) =>
            |  true;
            |
            |import {Wirespec} from '../Wirespec'
            |export type TestNum1 = number;
            |export const validateTestNum1 = (value: number) =>
            |  value <= 0.5;
            |
            |import {Wirespec} from '../Wirespec'
            |export type TestNum2 = number;
            |export const validateTestNum2 = (value: number) =>
            |  -0.2 <= value && value <= 0.5;
            |
            |import {Wirespec} from '../Wirespec'
            |export namespace TodoIdGenerator {
            |  export function generate(path: string[], generator: Wirespec.Generator): TodoId {
            |    return { value: generator.generate((path + 'value'), "TodoId", { regex: '^[0-9a-fA-F]{8}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{12}${'$'}' }) };
            |  }
            |}
            |
            |import {Wirespec} from '../Wirespec'
            |export namespace TodoNoRegexGenerator {
            |  export function generate(path: string[], generator: Wirespec.Generator): TodoNoRegex {
            |    return { value: generator.generate((path + 'value'), "TodoNoRegex", { regex: undefined }) };
            |  }
            |}
            |
            |import {Wirespec} from '../Wirespec'
            |export namespace TestIntGenerator {
            |  export function generate(path: string[], generator: Wirespec.Generator): TestInt {
            |    return { value: generator.generate((path + 'value'), "TestInt", { min: undefined, max: undefined }) };
            |  }
            |}
            |
            |import {Wirespec} from '../Wirespec'
            |export namespace TestInt0Generator {
            |  export function generate(path: string[], generator: Wirespec.Generator): TestInt0 {
            |    return { value: generator.generate((path + 'value'), "TestInt0", { min: undefined, max: undefined }) };
            |  }
            |}
            |
            |import {Wirespec} from '../Wirespec'
            |export namespace TestInt1Generator {
            |  export function generate(path: string[], generator: Wirespec.Generator): TestInt1 {
            |    return { value: generator.generate((path + 'value'), "TestInt1", { min: 0, max: undefined }) };
            |  }
            |}
            |
            |import {Wirespec} from '../Wirespec'
            |export namespace TestInt2Generator {
            |  export function generate(path: string[], generator: Wirespec.Generator): TestInt2 {
            |    return { value: generator.generate((path + 'value'), "TestInt2", { min: 1, max: 3 }) };
            |  }
            |}
            |
            |import {Wirespec} from '../Wirespec'
            |export namespace TestNumGenerator {
            |  export function generate(path: string[], generator: Wirespec.Generator): TestNum {
            |    return { value: generator.generate((path + 'value'), "TestNum", { min: undefined, max: undefined }) };
            |  }
            |}
            |
            |import {Wirespec} from '../Wirespec'
            |export namespace TestNum0Generator {
            |  export function generate(path: string[], generator: Wirespec.Generator): TestNum0 {
            |    return { value: generator.generate((path + 'value'), "TestNum0", { min: undefined, max: undefined }) };
            |  }
            |}
            |
            |import {Wirespec} from '../Wirespec'
            |export namespace TestNum1Generator {
            |  export function generate(path: string[], generator: Wirespec.Generator): TestNum1 {
            |    return { value: generator.generate((path + 'value'), "TestNum1", { min: undefined, max: 0.5 }) };
            |  }
            |}
            |
            |import {Wirespec} from '../Wirespec'
            |export namespace TestNum2Generator {
            |  export function generate(path: string[], generator: Wirespec.Generator): TestNum2 {
            |    return { value: generator.generate((path + 'value'), "TestNum2", { min: -0.2, max: 0.5 }) };
            |  }
            |}
            |
            |export {TodoId} from './TodoId'
            |export {TodoNoRegex} from './TodoNoRegex'
            |export {TestInt} from './TestInt'
            |export {TestInt0} from './TestInt0'
            |export {TestInt1} from './TestInt1'
            |export {TestInt2} from './TestInt2'
            |export {TestNum} from './TestNum'
            |export {TestNum0} from './TestNum0'
            |export {TestNum1} from './TestNum1'
            |export {TestNum2} from './TestNum2'
        """.trimMargin()

        CompileRefinedTest.compiler { TypeScriptIrEmitter() } shouldBeRight typescript
    }

    @Test
    fun compileUnionTest() {
        val typescript = """
            |import {Wirespec} from '../Wirespec'
            |import {type UserAccountPassword} from '../model'
            |import {type UserAccountToken} from '../model'
            |export type UserAccount = UserAccountPassword | UserAccountToken
            |
            |import {Wirespec} from '../Wirespec'
            |export type UserAccountPassword = {
            |  "username": string,
            |  "password": string,
            |}
            |export function validateUserAccountPassword(obj: UserAccountPassword): string[] {
            |  return [] as string[];
            |}
            |
            |import {Wirespec} from '../Wirespec'
            |export type UserAccountToken = {
            |  "token": string,
            |}
            |export function validateUserAccountToken(obj: UserAccountToken): string[] {
            |  return [] as string[];
            |}
            |
            |import {Wirespec} from '../Wirespec'
            |import {UserAccount} from './UserAccount'
            |export type User = {
            |  "username": string,
            |  "account": UserAccount,
            |}
            |export function validateUser(obj: User): string[] {
            |  return [] as string[];
            |}
            |
            |import {Wirespec} from '../Wirespec'
            |export namespace UserAccountGenerator {
            |  export function generate(path: string[], generator: Wirespec.Generator): UserAccount {
            |    const variant = generator.generate((path + 'variant'), "UserAccount", { variants: ['UserAccountPassword', 'UserAccountToken'] });
            |    switch (variant) {
            |      case 'UserAccountPassword':
            |        return UserAccountPasswordGenerator.generate((path + 'UserAccountPassword'), generator);
            |        break;
            |      case 'UserAccountToken':
            |        return UserAccountTokenGenerator.generate((path + 'UserAccountToken'), generator);
            |        break;
            |    }
            |    throw new Error('Unknown variant');
            |  }
            |}
            |
            |import {Wirespec} from '../Wirespec'
            |export namespace UserAccountPasswordGenerator {
            |  export function generate(path: string[], generator: Wirespec.Generator): UserAccountPassword {
            |    return { username: generator.generate((path + 'username'), "UserAccountPassword", { regex: undefined }), password: generator.generate((path + 'password'), "UserAccountPassword", { regex: undefined }) };
            |  }
            |}
            |
            |import {Wirespec} from '../Wirespec'
            |export namespace UserAccountTokenGenerator {
            |  export function generate(path: string[], generator: Wirespec.Generator): UserAccountToken {
            |    return { token: generator.generate((path + 'token'), "UserAccountToken", { regex: undefined }) };
            |  }
            |}
            |
            |import {Wirespec} from '../Wirespec'
            |export namespace UserGenerator {
            |  export function generate(path: string[], generator: Wirespec.Generator): User {
            |    return { username: generator.generate((path + 'username'), "User", { regex: undefined }), account: UserAccountGenerator.generate((path + 'account'), generator) };
            |  }
            |}
            |
            |export {UserAccount} from './UserAccount'
            |export {UserAccountPassword} from './UserAccountPassword'
            |export {UserAccountToken} from './UserAccountToken'
            |export {User} from './User'
        """.trimMargin()

        CompileUnionTest.compiler { TypeScriptIrEmitter() } shouldBeRight typescript
    }

    @Test
    fun compileTypeTest() {
        val typescript = """
            |import {Wirespec} from '../Wirespec'
            |export type Request = {
            |  "type": string,
            |  "url": string,
            |  "BODY_TYPE": string | undefined,
            |  "params": string[],
            |  "headers": Record<string, string>,
            |  "body": Record<string, (string | undefined)[] | undefined> | undefined,
            |}
            |export function validateRequest(obj: Request): string[] {
            |  return [] as string[];
            |}
            |
            |import {Wirespec} from '../Wirespec'
            |export namespace RequestGenerator {
            |  export function generate(path: string[], generator: Wirespec.Generator): Request {
            |    return { type: generator.generate((path + 'type'), "Request", { regex: undefined }), url: generator.generate((path + 'url'), "Request", { regex: undefined }), BODY_TYPE: (generator.generate((path + 'BODY_TYPE'), "Request", { inner: { regex: undefined } }) ? undefined : generator.generate((path + 'BODY_TYPE'), "Request", { regex: undefined })), params: generator.generate((path + 'params'), "Request", { inner: { regex: undefined } }), headers: generator.generate((path + 'headers'), "Request", { key: undefined, value: { regex: undefined } }), body: (generator.generate((path + 'body'), "Request", { inner: { key: undefined, value: undefined } }) ? undefined : generator.generate((path + 'body'), "Request", { key: undefined, value: undefined })) };
            |  }
            |}
            |
            |export {Request} from './Request'
        """.trimMargin()

        CompileTypeTest.compiler { TypeScriptIrEmitter() } shouldBeRight typescript
    }

    @Test
    fun compileNestedTypeTest() {
        val typescript = """
            |import {Wirespec} from '../Wirespec'
            |export type DutchPostalCode = string;
            |export const validateDutchPostalCode = (value: string) =>
            |  /^([0-9]{4}[A-Z]{2})${'$'}/g.test(value);
            |
            |import {Wirespec} from '../Wirespec'
            |import {DutchPostalCode} from './DutchPostalCode'
            |import {validateDutchPostalCode} from './DutchPostalCode'
            |export type Address = {
            |  "street": string,
            |  "houseNumber": number,
            |  "postalCode": DutchPostalCode,
            |}
            |export function validateAddress(obj: Address): string[] {
            |  return (!validateDutchPostalCode(obj.postalCode) ? ['postalCode'] : [] as string[]);
            |}
            |
            |import {Wirespec} from '../Wirespec'
            |import {Address} from './Address'
            |import {validateAddress} from './Address'
            |export type Person = {
            |  "name": string,
            |  "address": Address,
            |  "tags": string[],
            |}
            |export function validatePerson(obj: Person): string[] {
            |  return validateAddress(obj.address).map(e => `address.${'$'}{e}`);
            |}
            |
            |import {Wirespec} from '../Wirespec'
            |export namespace DutchPostalCodeGenerator {
            |  export function generate(path: string[], generator: Wirespec.Generator): DutchPostalCode {
            |    return { value: generator.generate((path + 'value'), "DutchPostalCode", { regex: '^([0-9]{4}[A-Z]{2})${'$'}' }) };
            |  }
            |}
            |
            |import {Wirespec} from '../Wirespec'
            |export namespace AddressGenerator {
            |  export function generate(path: string[], generator: Wirespec.Generator): Address {
            |    return { street: generator.generate((path + 'street'), "Address", { regex: undefined }), houseNumber: generator.generate((path + 'houseNumber'), "Address", { min: undefined, max: undefined }), postalCode: DutchPostalCodeGenerator.generate((path + 'postalCode'), generator) };
            |  }
            |}
            |
            |import {Wirespec} from '../Wirespec'
            |export namespace PersonGenerator {
            |  export function generate(path: string[], generator: Wirespec.Generator): Person {
            |    return { name: generator.generate((path + 'name'), "Person", { regex: undefined }), address: AddressGenerator.generate((path + 'address'), generator), tags: generator.generate((path + 'tags'), "Person", { inner: { regex: undefined } }) };
            |  }
            |}
            |
            |export {DutchPostalCode} from './DutchPostalCode'
            |export {Address} from './Address'
            |export {Person} from './Person'
        """.trimMargin()

        CompileNestedTypeTest.compiler { TypeScriptIrEmitter() } shouldBeRight typescript
    }

    @Test
    fun compileComplexModelTest() {
        val typescript = """
            |import {Wirespec} from '../Wirespec'
            |export type Email = string;
            |export const validateEmail = (value: string) =>
            |  /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}${'$'}/g.test(value);
            |
            |import {Wirespec} from '../Wirespec'
            |export type PhoneNumber = string;
            |export const validatePhoneNumber = (value: string) =>
            |  /^\+[1-9]\d{1,14}${'$'}/g.test(value);
            |
            |import {Wirespec} from '../Wirespec'
            |export type Tag = string;
            |export const validateTag = (value: string) =>
            |  /^[a-z][a-z0-9-]{0,19}${'$'}/g.test(value);
            |
            |import {Wirespec} from '../Wirespec'
            |export type EmployeeAge = number;
            |export const validateEmployeeAge = (value: number) =>
            |  18 <= value && value <= 65;
            |
            |import {Wirespec} from '../Wirespec'
            |import {Email} from './Email'
            |import {PhoneNumber} from './PhoneNumber'
            |import {validateEmail} from './Email'
            |import {validatePhoneNumber} from './PhoneNumber'
            |export type ContactInfo = {
            |  "email": Email,
            |  "phone": PhoneNumber | undefined,
            |}
            |export function validateContactInfo(obj: ContactInfo): string[] {
            |  return [...(!validateEmail(obj.email) ? ['email'] : [] as string[]), ...obj.phone != null ? (!validatePhoneNumber(obj.phone) ? ['phone'] : [] as string[]) : [] as string[]];
            |}
            |
            |import {Wirespec} from '../Wirespec'
            |import {EmployeeAge} from './EmployeeAge'
            |import {ContactInfo} from './ContactInfo'
            |import {Tag} from './Tag'
            |import {validateEmployeeAge} from './EmployeeAge'
            |import {validateContactInfo} from './ContactInfo'
            |import {validateTag} from './Tag'
            |export type Employee = {
            |  "name": string,
            |  "age": EmployeeAge,
            |  "contactInfo": ContactInfo,
            |  "tags": Tag[],
            |}
            |export function validateEmployee(obj: Employee): string[] {
            |  return [...(!validateEmployeeAge(obj.age) ? ['age'] : [] as string[]), ...validateContactInfo(obj.contactInfo).map(e => `contactInfo.${'$'}{e}`), ...obj.tags.flatMap((el, i) => (!validateTag(el) ? [`tags[${'$'}{i}]`] : [] as string[]))];
            |}
            |
            |import {Wirespec} from '../Wirespec'
            |import {Employee} from './Employee'
            |import {validateEmployee} from './Employee'
            |export type Department = {
            |  "name": string,
            |  "employees": Employee[],
            |}
            |export function validateDepartment(obj: Department): string[] {
            |  return obj.employees.flatMap((el, i) => validateEmployee(el).map(e => `employees[${'$'}{i}].${'$'}{e}`));
            |}
            |
            |import {Wirespec} from '../Wirespec'
            |import {Department} from './Department'
            |import {validateDepartment} from './Department'
            |export type Company = {
            |  "name": string,
            |  "departments": Department[],
            |}
            |export function validateCompany(obj: Company): string[] {
            |  return obj.departments.flatMap((el, i) => validateDepartment(el).map(e => `departments[${'$'}{i}].${'$'}{e}`));
            |}
            |
            |import {Wirespec} from '../Wirespec'
            |export namespace EmailGenerator {
            |  export function generate(path: string[], generator: Wirespec.Generator): Email {
            |    return { value: generator.generate((path + 'value'), "Email", { regex: '^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}${'$'}' }) };
            |  }
            |}
            |
            |import {Wirespec} from '../Wirespec'
            |export namespace PhoneNumberGenerator {
            |  export function generate(path: string[], generator: Wirespec.Generator): PhoneNumber {
            |    return { value: generator.generate((path + 'value'), "PhoneNumber", { regex: '^\+[1-9]\d{1,14}${'$'}' }) };
            |  }
            |}
            |
            |import {Wirespec} from '../Wirespec'
            |export namespace TagGenerator {
            |  export function generate(path: string[], generator: Wirespec.Generator): Tag {
            |    return { value: generator.generate((path + 'value'), "Tag", { regex: '^[a-z][a-z0-9-]{0,19}${'$'}' }) };
            |  }
            |}
            |
            |import {Wirespec} from '../Wirespec'
            |export namespace EmployeeAgeGenerator {
            |  export function generate(path: string[], generator: Wirespec.Generator): EmployeeAge {
            |    return { value: generator.generate((path + 'value'), "EmployeeAge", { min: 18, max: 65 }) };
            |  }
            |}
            |
            |import {Wirespec} from '../Wirespec'
            |export namespace ContactInfoGenerator {
            |  export function generate(path: string[], generator: Wirespec.Generator): ContactInfo {
            |    return { email: EmailGenerator.generate((path + 'email'), generator), phone: (generator.generate((path + 'phone'), "ContactInfo", { inner: undefined }) ? undefined : PhoneNumberGenerator.generate((path + 'phone'), generator)) };
            |  }
            |}
            |
            |import {Wirespec} from '../Wirespec'
            |export namespace EmployeeGenerator {
            |  export function generate(path: string[], generator: Wirespec.Generator): Employee {
            |    return { name: generator.generate((path + 'name'), "Employee", { regex: undefined }), age: EmployeeAgeGenerator.generate((path + 'age'), generator), contactInfo: ContactInfoGenerator.generate((path + 'contactInfo'), generator), tags: generator.generate((path + 'tags'), "Employee", { inner: undefined }) };
            |  }
            |}
            |
            |import {Wirespec} from '../Wirespec'
            |export namespace DepartmentGenerator {
            |  export function generate(path: string[], generator: Wirespec.Generator): Department {
            |    return { name: generator.generate((path + 'name'), "Department", { regex: undefined }), employees: generator.generate((path + 'employees'), "Department", { inner: undefined }) };
            |  }
            |}
            |
            |import {Wirespec} from '../Wirespec'
            |export namespace CompanyGenerator {
            |  export function generate(path: string[], generator: Wirespec.Generator): Company {
            |    return { name: generator.generate((path + 'name'), "Company", { regex: undefined }), departments: generator.generate((path + 'departments'), "Company", { inner: undefined }) };
            |  }
            |}
            |
            |export {Email} from './Email'
            |export {PhoneNumber} from './PhoneNumber'
            |export {Tag} from './Tag'
            |export {EmployeeAge} from './EmployeeAge'
            |export {ContactInfo} from './ContactInfo'
            |export {Employee} from './Employee'
            |export {Department} from './Department'
            |export {Company} from './Company'
        """.trimMargin()

        CompileComplexModelTest.compiler { TypeScriptIrEmitter() } shouldBeRight typescript
    }

    @Test
    fun sharedOutputTest() {
        val expected = """
            |export namespace Wirespec {
            |  export type Type = string
            |  export interface Model {
            |    validate(): string[];
            |  }
            |  export interface Enum {
            |    label: string;
            |  }
            |  export interface Endpoint {}
            |  export interface Channel {}
            |  export interface Refined<T> {
            |    value: T;
            |    validate(): boolean;
            |  }
            |  export interface Path {}
            |  export interface Queries {}
            |  export interface Headers {}
            |  export interface Handler {}
            |  export interface Call {}
            |  export type Method = "GET" | "PUT" | "POST" | "DELETE" | "OPTIONS" | "HEAD" | "PATCH" | "TRACE"
            |  export interface Request<T> {
            |    path: Path;
            |    method: Method;
            |    queries: Queries;
            |    headers: {};
            |    body: T;
            |  }
            |  export interface Response<T> {
            |    status: number;
            |    headers: {};
            |    body: T;
            |  }
            |  export interface BodySerializer {
            |    serializeBody<T>(t: T, type: Type): Uint8Array;
            |  }
            |  export interface BodyDeserializer {
            |    deserializeBody<T>(raw: Uint8Array, type: Type): T;
            |  }
            |  export interface BodySerialization extends BodySerializer, BodyDeserializer {}
            |  export interface PathSerializer {
            |    serializePath<T>(t: T, type: Type): string;
            |  }
            |  export interface PathDeserializer {
            |    deserializePath<T>(raw: string, type: Type): T;
            |  }
            |  export interface PathSerialization extends PathSerializer, PathDeserializer {}
            |  export interface ParamSerializer {
            |    serializeParam<T>(value: T, type: Type): string[];
            |  }
            |  export interface ParamDeserializer {
            |    deserializeParam<T>(values: string[], type: Type): T;
            |  }
            |  export interface ParamSerialization extends ParamSerializer, ParamDeserializer {}
            |  export interface Serializer extends BodySerializer, PathSerializer, ParamSerializer {}
            |  export interface Deserializer extends BodyDeserializer, PathDeserializer, ParamDeserializer {}
            |  export interface Serialization extends Serializer, Deserializer {}
            |  export type RawRequest = {
            |    "method": string,
            |    "path": string[],
            |    "queries": Record<string, string[]>,
            |    "headers": Record<string, string[]>,
            |    "body": Uint8Array | undefined,
            |  }
            |  export type RawResponse = {
            |    "statusCode": number,
            |    "headers": Record<string, string[]>,
            |    "body": Uint8Array | undefined,
            |  }
            |  export interface Transportation {
            |    transport(request: RawRequest): Promise<RawResponse>;
            |  }
            |  export interface GeneratorField<T> {}
            |  export type GeneratorFieldString = {
            |    "regex": string | undefined,
            |  }
            |  export type GeneratorFieldInteger = {
            |    "min": number | undefined,
            |    "max": number | undefined,
            |  }
            |  export type GeneratorFieldNumber = {
            |    "min": number | undefined,
            |    "max": number | undefined,
            |  }
            |  export type GeneratorFieldBoolean = {}
            |  export type GeneratorFieldBytes = {}
            |  export type GeneratorFieldEnum = {
            |    "values": string[],
            |  }
            |  export type GeneratorFieldUnion = {
            |    "variants": string[],
            |  }
            |  export type GeneratorFieldArray = {
            |    "inner": GeneratorField<unknown> | undefined,
            |  }
            |  export type GeneratorFieldNullable = {
            |    "inner": GeneratorField<unknown> | undefined,
            |  }
            |  export type GeneratorFieldDict = {
            |    "key": GeneratorField<unknown> | undefined,
            |    "value": GeneratorField<unknown> | undefined,
            |  }
            |  export interface Generator {
            |    generate<T>(path: string[], type: Type, field: GeneratorField<T>): T;
            |  }
            |  export type Client<REQ extends Request<unknown>, RES extends Response<unknown>> = (serialization: Serialization) => {
            |    to: (request: REQ) => RawRequest;
            |    from: (response: RawResponse) => RES
            |  }
            |  export type Server<REQ extends Request<unknown>, RES extends Response<unknown>> = (serialization: Serialization) => {
            |    from: (request: RawRequest) => REQ;
            |    to: (response: RES) => RawResponse
            |  }
            |  export type Api<REQ extends Request<unknown>, RES extends Response<unknown>> = {
            |    name: string;
            |    method: Method,
            |    path: string,
            |    client: Client<REQ, RES>;
            |    server: Server<REQ, RES>
            |  }
            |}
            |
        """.trimMargin()

        val emitter = TypeScriptIrEmitter()
        emitter.shared.source shouldBe expected
    }

    private fun emitGeneratorSource(node: Definition, fileNameSubstring: String): String {
        val emitter = TypeScriptIrEmitter()
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
            |import {Wirespec} from '../Wirespec'
            |export namespace AddressGenerator {
            |  export function generate(path: string[], generator: Wirespec.Generator): Address {
            |    return { street: generator.generate((path + 'street'), "Address", { regex: undefined }), number: generator.generate((path + 'number'), "Address", { min: undefined, max: undefined }) };
            |  }
            |}
            |
        """.trimMargin()

        emitGeneratorSource(address, "AddressGenerator") shouldBe expected
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
            |import {Wirespec} from '../Wirespec'
            |export namespace ColorGenerator {
            |  export function generate(path: string[], generator: Wirespec.Generator): Color {
            |    return Color.valueOf(generator.generate((path + 'value'), "Color", { values: ['RED', 'GREEN', 'BLUE'] }));
            |  }
            |}
            |
        """.trimMargin()

        emitGeneratorSource(color, "ColorGenerator") shouldBe expected
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
            |import {Wirespec} from '../Wirespec'
            |export namespace ShapeGenerator {
            |  export function generate(path: string[], generator: Wirespec.Generator): Shape {
            |    const variant = generator.generate((path + 'variant'), "Shape", { variants: ['Circle', 'Square'] });
            |    switch (variant) {
            |      case 'Circle':
            |        return CircleGenerator.generate((path + 'Circle'), generator);
            |        break;
            |      case 'Square':
            |        return SquareGenerator.generate((path + 'Square'), generator);
            |        break;
            |    }
            |    throw new Error('Unknown variant');
            |  }
            |}
            |
        """.trimMargin()

        emitGeneratorSource(shape, "ShapeGenerator") shouldBe expected
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
            |import {Wirespec} from '../Wirespec'
            |export namespace UUIDGenerator {
            |  export function generate(path: string[], generator: Wirespec.Generator): UUID {
            |    return { value: generator.generate((path + 'value'), "UUID", { regex: '^[0-9a-f]{8}${'$'}' }) };
            |  }
            |}
            |
        """.trimMargin()

        emitGeneratorSource(uuid, "UUIDGenerator") shouldBe expected
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
            |import {Wirespec} from '../Wirespec'
            |export namespace InventoryGenerator {
            |  export function generate(path: string[], generator: Wirespec.Generator): Inventory {
            |    return { items: generator.generate((path + 'items'), "Inventory", { inner: { min: undefined, max: undefined } }) };
            |  }
            |}
            |
        """.trimMargin()

        emitGeneratorSource(inventory, "InventoryGenerator") shouldBe expected
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
            |import {Wirespec} from '../Wirespec'
            |export namespace LookupGenerator {
            |  export function generate(path: string[], generator: Wirespec.Generator): Lookup {
            |    return { entries: generator.generate((path + 'entries'), "Lookup", { key: undefined, value: { min: undefined, max: undefined } }) };
            |  }
            |}
            |
        """.trimMargin()

        emitGeneratorSource(lookup, "LookupGenerator") shouldBe expected
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
            |import {Wirespec} from '../Wirespec'
            |export namespace PersonGenerator {
            |  export function generate(path: string[], generator: Wirespec.Generator): Person {
            |    return { nickname: (generator.generate((path + 'nickname'), "Person", { inner: { regex: undefined } }) ? undefined : generator.generate((path + 'nickname'), "Person", { regex: undefined })) };
            |  }
            |}
            |
        """.trimMargin()

        emitGeneratorSource(person, "PersonGenerator") shouldBe expected
    }
}
