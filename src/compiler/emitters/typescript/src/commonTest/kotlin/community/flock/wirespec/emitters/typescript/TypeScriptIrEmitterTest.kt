package community.flock.wirespec.emitters.typescript

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
            |    return { method: _request.method, path: ['todos', serialization.serializePath(_request.path.id, "string")], queries: { 'done': serialization.serializeParam(_request.queries.done, "boolean"), 'name': _request.queries.name != null ? serialization.serializeParam(_request.queries.name, "string") : [] as string[] }, headers: { 'token': serialization.serializeParam(_request.headers.token, "Token"), 'Refresh-Token': _request.headers.refreshToken != null ? serialization.serializeParam(_request.headers.refreshToken, "Token") : [] as string[] }, body: serialization.serializeBody(_request.body, "PotentialTodoDto") };
            |  }
            |  export function fromRawRequest(serialization: Wirespec.Deserializer, _request: Wirespec.RawRequest): Request {
            |    return request({"id": serialization.deserializePath(_request.path[1], "string"), "done": _request.queries['done'] != null ? serialization.deserializeParam(_request.queries['done'], "boolean") : (() => { throw new Error('Param done cannot be null') })(), "name": _request.queries['name'] != null ? serialization.deserializeParam(_request.queries['name'], "string") : undefined, "token": _request.headers['token'] != null ? serialization.deserializeParam(_request.headers['token'], "Token") : (() => { throw new Error('Param token cannot be null') })(), "refreshToken": _request.headers['Refresh-Token'] != null ? serialization.deserializeParam(_request.headers['Refresh-Token'], "Token") : undefined, "body": _request.body != null ? serialization.deserializeBody(_request.body, "PotentialTodoDto") : (() => { throw new Error('body is null') })()});
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
            |  export interface Call extends Wirespec.Call {
            |    putTodo(id: string, done: boolean, name: string | undefined, token: Token, refreshToken: Token | undefined, body: PotentialTodoDto): Promise<Response<unknown>>;
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
            |import {PutTodo} from './PutTodo'
            |import {type Token} from '../model'
            |import {type PotentialTodoDto} from '../model'
            |import {type TodoDto} from '../model'
            |import {type Error} from '../model'
            |export const putTodoClient = (serialization: Wirespec.Serialization, transportation: Wirespec.Transportation) => ({
            |  putTodo: async (id: string, done: boolean, name: string | undefined, token: Token, refreshToken: Token | undefined, body: PotentialTodoDto): Promise<PutTodo.Response<unknown>> => {
            |    const request: PutTodo.Request = PutTodo.request({id, done, name, token, refreshToken, body});
            |    const rawRequest = PutTodo.toRawRequest(serialization, request);
            |    const rawResponse = await transportation.transport(rawRequest);
            |    return PutTodo.fromRawResponse(serialization, rawResponse);
            |  }
            |})
            |
            |import {Wirespec} from '../Wirespec'
            |import {putTodoClient} from './PutTodoClient'
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
            |import {GetTodos} from './GetTodos'
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
            |import {getTodosClient} from './GetTodosClient'
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
