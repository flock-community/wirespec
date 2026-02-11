package community.flock.wirespec.emitters.typescript

import community.flock.wirespec.compiler.test.CompileChannelTest
import community.flock.wirespec.compiler.test.CompileEnumTest
import community.flock.wirespec.compiler.test.CompileFullEndpointTest
import community.flock.wirespec.compiler.test.CompileMinimalEndpointTest
import community.flock.wirespec.compiler.test.CompileRefinedTest
import community.flock.wirespec.compiler.test.CompileTypeTest
import community.flock.wirespec.compiler.test.CompileUnionTest
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class TypeScriptIrEmitterTest {

    @Test
    fun compileFullEndpointTest() {
        val typescript = """
            |import {Wirespec} from '../Wirespec'
            |
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
            |    "status": number,
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
            |    "status": number,
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
            |    "status": number,
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
            |    return { method: _request.method, path: ['todos', serialization.serializePath(_request.path.id, "string")], queries: { 'done': serialization.serializeParam(_request.queries.done, "boolean"), 'name': _request.queries.name != null ? serialization.serializeParam(_request.queries.name, "string") : [] }, headers: { 'token': serialization.serializeParam(_request.headers.token, "Token"), 'refreshToken': _request.headers.refreshToken != null ? serialization.serializeParam(_request.headers.refreshToken, "Token") : [] }, body: serialization.serializeBody(_request.body, "PotentialTodoDto") };
            |  }
            |  export function fromRawRequest(serialization: Wirespec.Deserializer, _request: Wirespec.RawRequest): Request {
            |    return request({"id": serialization.deserializePath(_request.path[1], "string"), "done": _request.queries['done'] != null ? serialization.deserializeParam(_request.queries['done'], "boolean") : (() => { throw new Error('Param done cannot be null') })(), "name": _request.queries['name'] != null ? serialization.deserializeParam(_request.queries['name'], "string") : undefined, "token": _request.headers['token'] != null ? serialization.deserializeParam(_request.headers['token'], "Token") : (() => { throw new Error('Param token cannot be null') })(), "refreshToken": _request.headers['refreshToken'] != null ? serialization.deserializeParam(_request.headers['refreshToken'], "Token") : undefined, "body": _request.body != null ? serialization.deserializeBody(_request.body, "PotentialTodoDto") : (() => { throw new Error('body is null') })()});
            |  }
            |  export function toRawResponse(serialization: Wirespec.Serializer, response: Response<unknown>): Wirespec.RawResponse {
            |    switch (response.status) {
            |      case 200: {
            |        const r = response as Response200;
            |        return { statusCode: r.status, headers: {}, body: serialization.serializeBody(r.body, "TodoDto") };
            |      }
            |      case 201: {
            |        const r = response as Response201;
            |        return { statusCode: r.status, headers: { 'token': serialization.serializeParam(r.headers.token, "Token"), 'refreshToken': r.headers.refreshToken != null ? serialization.serializeParam(r.headers.refreshToken, "Token") : [] }, body: serialization.serializeBody(r.body, "TodoDto") };
            |      }
            |      case 500: {
            |        const r = response as Response500;
            |        return { statusCode: r.status, headers: {}, body: serialization.serializeBody(r.body, "Error") };
            |      }
            |      default: {
            |        throw new Error(('Cannot match response with status: ' + response.status));
            |      }
            |    }
            |  }
            |  export function fromRawResponse(serialization: Wirespec.Deserializer, response: Wirespec.RawResponse): Response<unknown> {
            |    switch (response.statusCode) {
            |      case 200:
            |        return response200({"body": response.body != null ? serialization.deserializeBody(response.body, "TodoDto") : (() => { throw new Error('body is null') })()});
            |        break;
            |      case 201:
            |        return response201({"token": response.headers['token'] != null ? serialization.deserializeParam(response.headers['token'], "Token") : (() => { throw new Error('Param token cannot be null') })(), "refreshToken": response.headers['refreshToken'] != null ? serialization.deserializeParam(response.headers['refreshToken'], "Token") : undefined, "body": response.body != null ? serialization.deserializeBody(response.body, "TodoDto") : (() => { throw new Error('body is null') })()});
            |        break;
            |      case 500:
            |        return response500({"body": response.body != null ? serialization.deserializeBody(response.body, "Error") : (() => { throw new Error('body is null') })()});
            |        break;
            |      default:
            |        throw new Error(('Cannot match response with status: ' + response.statusCode));
            |    }
            |  }
            |  export interface Handler extends Wirespec.Handler {
            |    putTodo(_request: Request): Promise<Response<unknown>>;
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
            |
            |export type PotentialTodoDto = {
            |  "name": string,
            |  "done": boolean,
            |}
            |
            |import {Wirespec} from '../Wirespec'
            |
            |export type Token = {
            |  "iss": string,
            |}
            |
            |import {Wirespec} from '../Wirespec'
            |
            |export type TodoDto = {
            |  "id": string,
            |  "name": string,
            |  "done": boolean,
            |}
            |
            |import {Wirespec} from '../Wirespec'
            |
            |export type Error = {
            |  "code": number,
            |  "description": string,
            |}
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
            |
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
            |
            |export type MyAwesomeEnum = "ONE" | "Two" | "THREE_MORE" | "UnitedKingdom"
            |
            |export {MyAwesomeEnum} from './MyAwesomeEnum'
        """.trimMargin()

        CompileEnumTest.compiler { TypeScriptIrEmitter() } shouldBeRight typescript
    }

    @Test
    fun compileMinimalEndpointTest() {
        val typescript = """
            |import {Wirespec} from '../Wirespec'
            |
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
            |    "status": number,
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
            |        throw new Error(('Cannot match response with status: ' + response.status));
            |      }
            |    }
            |  }
            |  export function fromRawResponse(serialization: Wirespec.Deserializer, response: Wirespec.RawResponse): Response<unknown> {
            |    switch (response.statusCode) {
            |      case 200:
            |        return response200({"body": response.body != null ? serialization.deserializeBody(response.body, "TodoDto[]") : (() => { throw new Error('body is null') })()});
            |        break;
            |      default:
            |        throw new Error(('Cannot match response with status: ' + response.statusCode));
            |    }
            |  }
            |  export interface Handler extends Wirespec.Handler {
            |    getTodos(_request: Request): Promise<Response<unknown>>;
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
            |
            |export type TodoDto = {
            |  "description": string,
            |}
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
            |
            |export type TodoId = string;
            |export const validateTodoId = (value: string): value is TodoId =>
            |  /^[0-9a-fA-F]{8}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{12}${'$'}/g.test(value);
            |
            |export {TodoId} from './TodoId'
        """.trimMargin()

        CompileRefinedTest.compiler { TypeScriptIrEmitter() } shouldBeRight typescript
    }

    @Test
    fun compileUnionTest() {
        val typescript = """
            |import {Wirespec} from '../Wirespec'
            |
            |import {type UserAccountPassword} from '../model'
            |import {type UserAccountToken} from '../model'
            |export type UserAccount = UserAccountPassword | UserAccountToken
            |
            |import {Wirespec} from '../Wirespec'
            |
            |export type UserAccountPassword = {
            |  "username": string,
            |  "password": string,
            |}
            |
            |import {Wirespec} from '../Wirespec'
            |
            |export type UserAccountToken = {
            |  "token": string,
            |}
            |
            |import {Wirespec} from '../Wirespec'
            |
            |import {UserAccount} from './UserAccount'
            |export type User = {
            |  "username": string,
            |  "account": UserAccount,
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
            |
            |export type Request = {
            |  "type": string,
            |  "url": string,
            |  "BODY_TYPE": string | undefined,
            |  "params": string[],
            |  "headers": Record<string, string>,
            |  "body": Record<string, (string | undefined)[] | undefined> | undefined,
            |}
            |
            |export {Request} from './Request'
        """.trimMargin()

        CompileTypeTest.compiler { TypeScriptIrEmitter() } shouldBeRight typescript
    }

    @Test
    fun sharedOutputTest() {
        val expected = """
            |export namespace Wirespec {
            |  export type Type = string
            |  export interface Enum {
            |    label: string;
            |  }
            |  export interface Endpoint {}
            |  export interface Channel {}
            |  export interface Refined {
            |    value: string;
            |  }
            |  export interface Path {}
            |  export interface Queries {}
            |  export interface Headers {}
            |  export interface Handler {}
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
        emitter.shared!!.source shouldBe expected
    }
}
